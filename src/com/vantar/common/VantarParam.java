package com.vantar.common;


public class VantarParam {

    public static final String VERSION = "2.46.9";

    public static final String QUEUE_NAME_USER_ACTION_LOG = "user-action-log";

    public static final int MESSAGE_SERVICES_START = 7001;
    public static final int MESSAGE_SERVICES_STOP = 7002;
    public static final int MESSAGE_SERVICE_STARTED = 7003;
    public static final int MESSAGE_SERVICE_STOPPED = 7004;
    public static final int MESSAGE_SETTINGS_UPDATED = 7005;
    public static final int MESSAGE_UPDATE_SETTINGS = 7006;
    public static final int MESSAGE_SERVICE_ENABLED_COUNT = 7007;
    public static final int MESSAGE_DATABASE_UPDATED = 7008;

    public static final char SEPARATOR_COMMON = ',';
    public static final char SEPARATOR_URL = '|';
    public static final char SEPARATOR_KEY_PHRASE = ',';
    public static final char SEPARATOR_BLOCK = ';';
    public static final char SEPARATOR_KEY_VAL = ':';
    public static final char SEPARATOR_DOT = '.';
    public static final String SEPARATOR_NEXT = ">>";
    public static final String SEPARATOR_BLOCK_COMPLEX = ";;;";
    public static final String SEPARATOR_COMMON_COMPLEX = ",,,";

    public static final long INVALID_ID = 0;
    public static final String AUTH_TOKEN = "x";
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;

    public static final String LANG = "lang";
    public static final String ID = "id";
    public static final String TOTAL_COUNT = "totalcount";
    public static final String URL = "url";
    public static final String DTO = "dto";
    public static final String EXCLUDE_PROPERTIES = "__excludeProperties";
    public static final String NULL_PROPERTIES = "__nullProperties";
    public static final String SET_ACTION = "__action";
    public static final String USER_NAME = "username";
    public static final String PASSWORD = "password";

    public static final String OPERATOR_AND = "and";
    public static final String OPERATOR_OR = "or";
    public static final String OPERATOR_PHRASE = "phrase";

    public static final String HEADER_AUTH_TOKEN = "X-Auth-Token";
    public static final String HEADER_LANG = "X-Lang";

    public static final String LOGICAL_DELETED_UNDO = "logicaldeletedundo";
    public static final String COORDINATE = "coordinates";
}
