package com.vantar.database.query;

public enum QueryOperator {
    AND,
    OR,
    NOR,
    XOR,
    NOT,
    EXISTS,

    QUERY,
    EQUAL,
    NOT_EQUAL,
    LIKE,
    NOT_LIKE,
    FULL_SEARCH,
    IN,
    NOT_IN,
    CONTAINS_ALL,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_EQUAL,
    GREATER_THAN_EQUAL,
    BETWEEN,
    NOT_BETWEEN,
    IS_NULL,
    IS_NOT_NULL,
    IS_EMPTY,
    IS_NOT_EMPTY,
    NEAR,
    //FAR, (pseudo operator)
    WITHIN,

    MAP_KEY_EXISTS,
    IN_LIST,
    IN_DTO,
}
