package dk.ku.di.dms.vms.modb.api.query.statement;

import dk.ku.di.dms.vms.modb.api.query.clause.*;

import java.util.ArrayList;
import java.util.List;

/**
 * No support for UNION, EXCEPT, INTERSECT yet.
 * This class is immutable. That is, by the time it is created, none of its structure can be changed.
 * However, to avoid the overhead on creating the same statement on every call,
 * this class can be created as static on a class annotated as {@link dk.ku.di.dms.vms.modb.api.annotations.Microservice},
 * and benefit of parameterized calls
 */
public final class SelectStatement extends AbstractStatement {

    public List<String> selectClause;

    public List<GroupBySelectElement> groupBySelectClause;

    public List<HavingClauseElement<?>> havingClause;

    public List<String> fromClause;

    public List<JoinClauseElement> joinClause;

    public List<OrderByClauseElement> orderByClause;

    public List<String> groupByClause;

    public Integer limit;

    public SelectStatement() {
        super();
    }

    public SelectStatement(StringBuilder sql, List<String> selectClause, List<String> fromClause, List<WhereClauseElement> whereClause) {
        super(sql);
        this.selectClause = selectClause;
        this.fromClause = fromClause;
        this.whereClause.addAll(whereClause);
    }

    public SelectStatement(List<String> selectClause, String table, List<WhereClauseElement> whereClause) {
        this.selectClause = selectClause;
        this.fromClause = List.of(table);
        this.whereClause.addAll(whereClause);
    }

    public SelectStatement(StringBuilder sql, List<String> selectClause, List<GroupBySelectElement> groupBySelectClause,
                           List<String> fromClause, List<WhereClauseElement> whereClause, List<String> groupByClause) {
        super(sql);
        this.selectClause = selectClause;
        this.groupBySelectClause = groupBySelectClause;
        this.fromClause = fromClause;
        this.whereClause.addAll(whereClause);
        this.groupByClause = groupByClause;
    }

    @Override
    public StatementType getType() {
        return StatementType.SELECT;
    }

    @Override
    public SelectStatement asSelectStatement() {
        return this;
    }

    public SelectStatement clone(List<WhereClauseElement> whereClause){
        return new SelectStatement(this.SQL, this.selectClause, this.fromClause, whereClause);
    }

    public SelectStatement setParam(Object... params) {
        List<WhereClauseElement> whereClause_ = new ArrayList<>(this.whereClause.size());
        for (int i = 0; i < this.whereClause.size(); i++) {
            whereClause_.add(
                    this.whereClause.get(i).overwriteValue(params[i])
            );
        }
        return new SelectStatement(this.SQL, this.selectClause, this.groupBySelectClause,
                this.fromClause, whereClause_, this.groupByClause);
    }

}
