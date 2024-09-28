package com.vantar.database.sql;

import com.vantar.util.datetime.DateTime;


public class SqlUtil {

    public static Object getDateTimeAsSql(Object value) {
        DateTime dateTime = (DateTime) value;
        switch (dateTime.getType()) {
            case DateTime.TIMESTAMP:
                return dateTime.getAsSqlTimestamp();
            case DateTime.DATE:
                return dateTime.getAsSqlDate();
            case DateTime.TIME:
                return dateTime.getAsSqlTime();
        }
        return null;
    }
}
