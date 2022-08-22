package com.vantar.database.query;

import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;


public class QueryGroup {

    public final String[] columns;
    public final QueryGroupType groupType;


    public QueryGroup(QueryGroupType groupType, String[] columns) {
//        for (int i = 0, l = columns.length; i < l; ++i) {
//            columns[i] = StringUtil.toSnakeCase(columns[i]);
//        }
        this.groupType = groupType;
        this.columns = columns;
    }

    public QueryGroup(String column, String columnAs) {
        columns = new String[2];
        //columns[0] = StringUtil.toSnakeCase(column);
        columns[0] = column;
        //columns[1] = StringUtil.toSnakeCase(columnAs);
        columns[1] = columnAs;
        this.groupType = QueryGroupType.MAP;
    }

    public QueryGroup(QueryGroupType groupType, String column) {
        columns = new String[2];
        //this.columns[0] = StringUtil.toSnakeCase(column);
        this.columns[0] = column;
        this.columns[1] = null;
        this.groupType = groupType;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }
}
