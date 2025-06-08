package dk.ku.di.dms.vms.modb.api.query.parser;

import dk.ku.di.dms.vms.modb.api.query.clause.OrderByClauseElement;
import dk.ku.di.dms.vms.modb.api.query.clause.WhereClauseElement;
import dk.ku.di.dms.vms.modb.api.query.enums.ExpressionTypeEnum;
import dk.ku.di.dms.vms.modb.api.query.statement.SelectStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Only parses simple SELECT statements
 */
public final class Parser {

    /**
     *
     * @param sql a simple select statement (select <x,y,x,...> from table where <>
     */
    public static SelectStatement parse(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty.");
        }
    
        String[] tokens = sql.trim().split("\\s+");
        if (tokens.length == 0 || !tokens[0].equalsIgnoreCase("select")) {
            throw new IllegalArgumentException("Invalid SQL query. Must start with SELECT.");
        }
    
        int i = 1;
        List<String> projection = new ArrayList<>();
    
        // check columns to select
        while (i < tokens.length && !tokens[i].equalsIgnoreCase("from")) {
            projection.add(tokens[i].replace(",", "").trim());
            i++;
        }
    
        if (i >= tokens.length || !tokens[i].equalsIgnoreCase("from")) {
            throw new IllegalArgumentException("Invalid SQL query. Missing FROM clause.");
        }
        i++; // Move past "from"
    
        if (i >= tokens.length) {
            throw new IllegalArgumentException("Invalid SQL query. Missing table name.");
        }
        String table = tokens[i];
        i++;
    
        List<WhereClauseElement> whereClauseElements = new ArrayList<>();
        if (i < tokens.length && tokens[i].equalsIgnoreCase("where")) {
            i++; // Move past "where"
    
            while (i < tokens.length && !tokens[i].equalsIgnoreCase("order")) {
                if (i + 2 >= tokens.length) {
                    throw new IllegalArgumentException("Invalid WHERE clause. Incomplete condition.");
                }
    
                String left = tokens[i];
                ExpressionTypeEnum exp = getExpressionFromString(tokens[i + 1]);
                String right = tokens[i + 2];
                whereClauseElements.add(new WhereClauseElement(left, exp, right));
                i += 3;
    
                // handle and / or
                if (i < tokens.length && (tokens[i].equalsIgnoreCase("and") || tokens[i].equalsIgnoreCase("or"))) {
                    i++;
                }
            }
        }
    
        List<OrderByClauseElement> orderByClauseElements = new ArrayList<>();
        if (i < tokens.length && tokens[i].equalsIgnoreCase("order")) {
            i++; // Move past "order"
            if (i >= tokens.length || !tokens[i].equalsIgnoreCase("by")) {
                // throw error if this happens
                throw new IllegalArgumentException("Invalid ORDER BY clause.");
            }
            i++; // Move past "by"
    
            while (i < tokens.length) {
                orderByClauseElements.add(new OrderByClauseElement(tokens[i].replace(",", "").trim()));
                i++;
            }
        }
    
        return new SelectStatement(projection, table, whereClauseElements, orderByClauseElements);
    }
    
    private static ExpressionTypeEnum getExpressionFromString(String exp) {
        // First check by name for common expressions
        if (ExpressionTypeEnum.EQUALS.name.equalsIgnoreCase(exp)) {
            return ExpressionTypeEnum.EQUALS;
        }
        
        if (ExpressionTypeEnum.IN.name.equalsIgnoreCase(exp)) {
            return ExpressionTypeEnum.IN;
        }
        
        // Fall back to checking all enum values
        for (ExpressionTypeEnum type : ExpressionTypeEnum.values()) {
            if (type.name().equalsIgnoreCase(exp)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unsupported expression type: " + exp);
    }
}
