package com.vantar.database.query;

import com.vantar.database.dto.Dto;


public class QueryJoin {

    public static final String INNER_JOIN = "JOIN";
    public static final String LEFT_JOIN = "LEFT JOIN";
    public static final String FULL_JOIN = "FULL JOIN";
    public static final String SELF_JOIN = ",";
    public static final String CROSS_JOIN = "CROSS JOIN";
    public static final String NATURAL_JOIN = "NATURAL JOIN";

    private final String joinType;
    private final String table;
    private final String keyLeft;
    private final String keyRight;
    private Dto dto;
    private String tClass;
    private String as;


    public QueryJoin(String joinType, Dto dto, String keyLeft, String keyRight) {
        this.joinType = joinType;
        this.dto = dto;
        table = dto.getStorage();
        tClass = dto.getClass().getSimpleName();
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
    }

    public QueryJoin(String joinType, String table, String keyLeft, String keyRight) {
        this.joinType = joinType;
        this.table = table;
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
    }

    public QueryJoin(String joinType, Dto dto, String as, String keyLeft, String keyRight) {
        this.joinType = joinType;
        this.dto = dto;
        this.as = as;
        table = dto.getStorage();
        tClass = dto.getClass().getSimpleName();
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
    }

    public Dto getDto() {
        return dto;
    }

    public String getTable() {
        return table;
    }

    public String gettClass() {
        return tClass;
    }

    public String getKeyLeft() {
        return keyLeft;
    }

    public String getKeyRight() {
        return keyRight;
    }

    public String getJoin() {
        return joinType;
    }

    public String getAs() {
        return as;
    }
}
