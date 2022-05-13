package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.util.object.ObjectUtil;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings({"unchecked"})
abstract public class QueryResultBase {

    protected static final Logger log = LoggerFactory.getLogger(QueryResultBase.class);
    protected Dto dto;
    protected Field[] fields;
    protected Set<String> exclude;
    protected String[] locales;
    protected Event event;


    public <T extends QueryResult> T setEvent(Event event) {
        this.event = event;
        return (T) this;
    }

    public <T extends QueryResult> T setLocale(String... locales) {
        this.locales = locales;
        return (T) this;
    }

    protected String[] getLocales() {
        if (ObjectUtil.isEmpty(locales)) {
            locales = new String[] { Locale.getDefaultLocale() };
        }
        return locales;
    }

    protected String getLocale() {
        return ObjectUtil.isEmpty(locales) ? null : locales[0];
    }

    public <T extends Dto> T get() {
        return (T) dto;
    }

    public <T extends Dto> T getNext() throws DatabaseException, NoContentException {
        if (next()) {
            return (T) dto;
        }
        throw new NoContentException();
    }

    public <T extends Dto> List<T> asList() throws NoContentException, DatabaseException {
        try {
            List<T> data = new ArrayList<>();
            while (next()) {
                data.add((T) dto);
                if (event != null) {
                    event.afterSetData(dto, data);
                }
                dto = dto.getClass().getConstructor().newInstance();
            }

            if (data.isEmpty()) {
                throw new NoContentException();
            }

            return data;

        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("! data > dto({})", dto, e);
            return new ArrayList<>();
        } finally {
            close();
        }
    }


    abstract public boolean next() throws DatabaseException;


    abstract public void close();


    public interface Event {

        void afterSetData(Dto dto);

        void afterSetData(Dto dto, List<?> data);
    }
}