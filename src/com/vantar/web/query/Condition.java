package com.vantar.web.query;

import com.vantar.database.query.QueryOperator;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.util.List;

/**
 * Condition definition
 */
public class Condition {

    public String operator;
    public List<ConditionItem> items;


    public QueryOperator getOperator() {
        if (StringUtil.isEmpty(operator)) {
            return QueryOperator.AND;
        }
        if ("or".equalsIgnoreCase(operator)) {
            return QueryOperator.OR;
        }
        if ("not".equals(operator.toLowerCase())) {
            return QueryOperator.NOT;
        }
        if ("nor".equals(operator.toLowerCase())) {
            return QueryOperator.NOR;
        }
        return QueryOperator.AND;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }
}
