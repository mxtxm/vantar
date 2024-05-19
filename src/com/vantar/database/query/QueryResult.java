package com.vantar.database.query;

import com.vantar.database.common.KeyValueData;
import com.vantar.database.dto.Dto;
import com.vantar.exception.*;
import java.util.*;


public interface QueryResult {

    <T extends QueryResult> T setEvent(QueryResultBase.Event event);

    <T extends QueryResult> T setLocale(String... locales);

    <T extends Dto> T first() throws VantarException;

    <T extends Dto> T getNext() throws VantarException;

    <T extends Dto> T get();

    boolean next() throws VantarException;

    <T extends Dto> List<T> asList() throws VantarException;

    Collection<Object> asPropertyList(String property) throws VantarException;

    Map<Long, Object> asPropertyMap(String property) throws VantarException;

    void forEach(QueryResultBase.EventForeach event) throws VantarException;

    <T extends Dto> Map<Object, T> asMap(String keyField) throws VantarException;

    Map<String, String> asKeyValue(String keyField, String valueField) throws VantarException;

    Map<String, String> asKeyValue(KeyValueData definition) throws VantarException;

    Object peek(String field) throws VantarException;

    void close();

}