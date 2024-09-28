package com.vantar.database.query;

import com.vantar.util.object.ObjectUtil;


public class QueryGroup {

    public final String[] columns;
    public final QueryGroupType groupType;


    public QueryGroup(QueryGroupType groupType, String[] columns) {
        this.groupType = groupType;
        this.columns = columns;
    }

    /**
     * @param columns ---> columns(0:last-1) As columns(last)
     */
    public QueryGroup(String... columns) {
        this.columns = columns;
        this.groupType = QueryGroupType.MAP;
    }

    public QueryGroup(QueryGroupType groupType, String column) {
        columns = new String[2];
        this.columns[0] = column;
        this.columns[1] = null;
        this.groupType = groupType;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }
}
