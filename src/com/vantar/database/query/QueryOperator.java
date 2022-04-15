package com.vantar.database.query;

public enum QueryOperator {
    AND,
    OR,
    NOR,
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
    BETWEEN,
    NOT_BETWEEN,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_EQUAL,
    GREATER_THAN_EQUAL,
    IS_NULL,
    IS_NOT_NULL,
    IS_EMPTY,
    IS_NOT_EMPTY,
    CONTAINS_ALL,
    NEAR,
    WITHIN,

    IN_LIST,
    IN_DTO,
}
