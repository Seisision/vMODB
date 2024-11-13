package dk.ku.di.dms.vms.sdk.embed.facade;

import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;
import dk.ku.di.dms.vms.modb.api.query.clause.WhereClauseElement;
import dk.ku.di.dms.vms.modb.api.query.statement.SelectStatement;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.modb.definition.Table;
import dk.ku.di.dms.vms.modb.transaction.OperationalAPI;
import dk.ku.di.dms.vms.sdk.embed.entity.EntityHandler;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The interface between the application and internal database operations
 * Responsible to parse objects and convert database results to application objects.
 * A repository class do not need direct access to {@link ITransactionManager}
 */
public abstract class AbstractProxyRepository<PK extends Serializable, T extends IEntity<PK>> extends EntityHandler<PK,T> implements IRepository<PK, T> {

    /**
     * Respective table of the entity
     */
    private final Table table;

    /**
     * Attribute set after database is loaded
     * Not when the metadata is loaded
     * The transaction facade requires DBMS modules (planner, analyzer)
     * along with the catalog. These are not ready on metadata loading time
     */
    private final OperationalAPI operationalAPI;

    private final Map<String, SelectStatement> repositoryQueriesMap;

    public AbstractProxyRepository(Class<PK> pkClazz,
                                  Class<T> entityClazz,
                                  Table table,
                                  OperationalAPI operationalAPI,
                                  // key: method name, value: select stmt
                                  Map<String, SelectStatement> repositoryQueriesMap)
            throws NoSuchFieldException, IllegalAccessException {
        super(pkClazz, entityClazz, table.schema());
        this.table = table;
        this.operationalAPI = operationalAPI;
        this.repositoryQueriesMap = repositoryQueriesMap;
    }

    /**
     * <a href="http://mydailyjava.blogspot.com/2022/02/using-byte-buddy-for-proxy-creation.html">Proxy creation in ByteBuddy</a>
     */
    @SuppressWarnings("rawtypes")
    public static final class Interceptor {
        @RuntimeType
        public static Object intercept(
                @This AbstractProxyRepository self,
                @Origin Method method,
                @AllArguments Object[] args) {
            return self.intercept(method.getName(), args);
        }
    }

    public final List<T> intercept(String methodName, Object[] args) {
        // retrieve statically defined query
        SelectStatement selectStatement = this.repositoryQueriesMap.get(methodName);

        // set query arguments
        List<WhereClauseElement> whereClauseElements = new ArrayList<>(args.length);
        for(int i = 0; i < args.length; i++) {
            whereClauseElements.add(selectStatement.whereClause.get(i).overwriteValue( args[i] ));
        }
        selectStatement = selectStatement.clone(whereClauseElements);

        // submit for execution
        List<Object[]> records = this.operationalAPI.fetch(this.table, selectStatement);
        List<T> result = new ArrayList<>(records.size());
        for(Object[] record : records) {
            result.add( this.parseObjectIntoEntity(record) );
        }
        return result;
    }

    @Override
    public final List<T> getAll(){
        List<Object[]> records = this.operationalAPI.getAll(this.table);
        List<T> resultList = new ArrayList<>(records.size());
        for (var record : records){
            resultList.add(this.parseObjectIntoEntity(record));
        }
        return resultList;
    }

    @Override
    public final boolean exists(PK key){
        Object[] valuesOfKey = this.extractFieldValuesFromKeyObject(key);
        return this.operationalAPI.exists(this.table.primaryKeyIndex(), valuesOfKey);
    }

    @Override
    public final T lookupByKey(PK key){
        // this repository is oblivious to multi-versioning
        // given the single-thread model, we can work with writes easily
        // but reads are another story. multiple threads may be calling the repository facade
        // and requiring different data item versions
        // we always need to offload the lookup to the transaction facade
        Object[] valuesOfKey = this.extractFieldValuesFromKeyObject(key);
        Object[] object = this.operationalAPI.lookupByKey(this.table.primaryKeyIndex(), valuesOfKey);
        if (object == null) return null;
        // parse object into entity
        return this.parseObjectIntoEntity(object);
    }

    @Override
    public final List<T> lookupByKeys(Collection<PK> keys){
        List<T> resultList = new ArrayList<>(keys.size());
        for(PK obj : keys){
            Object[] valuesOfKey = this.extractFieldValuesFromKeyObject(obj);
            Object[] object = this.operationalAPI.lookupByKey(this.table.primaryKeyIndex(), valuesOfKey);
            resultList.add( this.parseObjectIntoEntity(object) );
        }
        return resultList;
    }

    @Override
    public final void insert(T entity) {
        Object[] values = this.extractFieldValuesFromEntityObject(entity);
        this.operationalAPI.insert(this.table, values);
    }

    /**
     * An optimization is just setting the PK into the entity passed as parameter.
     * Another optimization is checking if PK is generated before returning the entity,
     * but this is missing for now. However, this method is probably used when the PK
     * must be retrieved
     */
    @Override
    public final T insertAndGet(T entity){
        Object[] values = this.extractFieldValuesFromEntityObject(entity);
        Object[] newValues = this.operationalAPI.insertAndGet(this.table, values);
        for(var entry : this.pkFieldMap.entrySet()){
            int i = this.table.schema().columnPosition(entry.getKey());
            if(newValues[i] == null){
                continue;
            }
            entry.getValue().set( entity, newValues[i] );
        }
        return entity;
    }

    @Override
    public final void insertAll(List<T> entities) {
        // acts as a single transaction, so all constraints, of every single row must be present
        List<Object[]> parsedEntities = new ArrayList<>(entities.size());
        for (T entityObject : entities){
            Object[] parsed = this.extractFieldValuesFromEntityObject(entityObject);
            parsedEntities.add(parsed);
        }
        // can only add to cache if all items were inserted since it is transactional
        this.operationalAPI.insertAll( this.table, parsedEntities );
    }

    @Override
    public final void upsert(T entity) {
        Object[] values = this.extractFieldValuesFromEntityObject(entity);
        this.operationalAPI.upsert(this.table, values);
    }

    @Override
    public final void update(T entity) {
        Object[] values = this.extractFieldValuesFromEntityObject(entity);
        this.operationalAPI.update(this.table, values);
    }

    @Override
    public final void updateAll(List<T> entities) {
        List<Object[]> parsedEntities = new ArrayList<>(entities.size());
        for (T entityObject : entities){
            Object[] parsed = this.extractFieldValuesFromEntityObject(entityObject);
            parsedEntities.add(parsed);
        }
        this.operationalAPI.updateAll(this.table, parsedEntities);
    }

    @Override
    public final void delete(T entity){
        Object[] values = this.extractFieldValuesFromEntityObject(entity);
        this.operationalAPI.delete(this.table, values);
    }

    @Override
    public final void deleteByKey(PK key) {
        Object[] valuesOfKey = this.extractFieldValuesFromKeyObject(key);
        this.operationalAPI.deleteByKey(this.table, valuesOfKey);
    }

    @Override
    public final void deleteAll(List<T> entities) {
        List<Object[]> parsedEntities = new ArrayList<>(entities.size());
        for (T entity : entities){
            parsedEntities.add( this.extractFieldValuesFromEntityObject(entity) );
        }
        this.operationalAPI.deleteAll(this.table, parsedEntities);
    }

    /**
     * This implementation should be used is a possible implementation with a sidecar
     * We can return objects in embedded mode. See last method
    public <DTO> List<DTO> fetchMany(SelectStatement statement, Class<DTO> clazz){
        // we need some context about the results in this memory space
        // solved by now with the assumption the result is of the same size of memory
        // number of records
        // schema of the return (maybe not if it is a dto)
        var memRes = this.operationalAPI.fetchMemoryReference(this.table, statement);
        try {
            List<DTO> result = new ArrayList<>(10);
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            Field[] fields = clazz.getFields();
            long currAddress = memRes.address();
            long lastOffset = currAddress + memRes.bytes();
            while(memRes != null) {
                DTO dto = (DTO) constructor.newInstance();
                for (var field : fields) {
                    DataType dt = DataTypeUtils.getDataTypeFromJavaType(field.getType());
                    field.set(dto, DataTypeUtils.callReadFunction(currAddress, dt));
                    currAddress += dt.value;
                }
                result.add(dto);

                if(currAddress == lastOffset) {
                    MemoryManager.releaseTemporaryDirectMemory(memRes.address());
                    memRes = memRes.next;
                    if(memRes != null){
                        memRes = memRes.next;
                        currAddress = memRes.address();
                        lastOffset = currAddress + memRes.bytes();
                    }
                }
            }
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
     */

    @Override
    public <DTO> DTO fetchOne(SelectStatement statement, Class<DTO> clazz){
        List<Object[]> objects = this.operationalAPI.fetch(this.table, statement);
        assert objects.size() == 1;
        Object[] object = objects.getFirst();
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        Field[] fields = clazz.getFields();
        return this.buildDtoInstance(object, constructor, fields);
    }

    @Override
    public <DTO> List<DTO> fetchMany(SelectStatement statement, Class<DTO> clazz){
        List<Object[]> objects = this.operationalAPI.fetch(this.table, statement);
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        Field[] fields = clazz.getFields();
        List<DTO> result = new ArrayList<>();
        for(Object[] object : objects){
            DTO dto = buildDtoInstance(object, constructor, fields);
            result.add(dto);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <DTO> DTO buildDtoInstance(Object[] object, Constructor<?> constructor, Field[] fields) {
        try {
            DTO dto = (DTO) constructor.newInstance();
            int i = 0;
            for (var field : fields) {
                // check if field was captured by query
                if(object[i] != null){
                    field.set(dto, object[i]);
                }
                i++;
            }
            return dto;
        } catch (InstantiationException | InvocationTargetException| IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

