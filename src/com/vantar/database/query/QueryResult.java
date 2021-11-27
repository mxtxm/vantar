package com.vantar.database.query;

import com.vantar.database.common.KeyValueData;
import com.vantar.database.dto.Dto;
import com.vantar.exception.DatabaseException;
import com.vantar.exception.NoContentException;
import java.util.*;


public interface QueryResult {

    <T extends QueryResult> T setLocale(String... locales);

    <T extends Dto> T first() throws NoContentException, DatabaseException;

    <T extends Dto> T getNext() throws DatabaseException, NoContentException;

    <T extends Dto> T get();

    boolean next() throws DatabaseException;

    <T extends Dto> List<T> asList() throws NoContentException, DatabaseException;

    Map<String, String> asKeyValue(String keyField, String valueField) throws DatabaseException, NoContentException;

    Map<String, String> asKeyValue(KeyValueData definition) throws DatabaseException, NoContentException;

    Object peek(String field) throws NoContentException, DatabaseException;

    void close();

}