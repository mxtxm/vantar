package com.vantar.locale;


public enum VantarKey implements LangKey {
    // auth
    USER_PASSWORD_EMPTY,
    USER_NOT_EXISTS,
    USER_DISABLED,
    USER_DISABLED_MAX_FAILED,
    WRONG_PASSWORD,
    MISSING_AUTH_TOKEN,
    INVALID_AUTH_TOKEN,
    EXPIRED_AUTH_TOKEN,
    NO_ACCESS,
    USER_REPO_NOT_SET,
    USER_ALREADY_SIGNED_IN,

    // datetime
    INVALID_TIME,
    INVALID_TIMEZONE,
    INVALID_DATE,
    INVALID_DATETIME,

    // validation
    REQUIRED,
    REQUIRED_OR,
    REQUIRED_XOR,
    DATA_TYPE,
    UNIQUE,
    REFERENCE,
    PARENT_CHILD,
    ILLEGAL,
    EMPTY_ID,
    INVALID_ID,
    REGEX,
    STRING_LENGTH_EXCEED,
    MAX_EXCEED,
    MIN_EXCEED,
    INVALID_VALUE,
    INVALID_FIELD,
    EVENT_REJECT,
    IO_ERROR,
    FILE_SIZE,
    FILE_TYPE,
    SEARCH_PARAM_INVALID_CONDITION_TYPE,
    SEARCH_PARAM_COL_MISSING,
    SEARCH_PARAM_VALUE_MISSING,
    SEARCH_PARAM_VALUE_INVALID,
    INVALID_GEO_LOCATION,

    // data fetch
    FETCH_FAIL,
    NO_CONTENT,

    // data write
    UPLOAD_SUCCESS,
    UPLOAD_FAIL,
    INSERT_SUCCESS,
    INSERT_MANY_SUCCESS,
    INSERT_FAIL,
    UPDATE_SUCCESS,
    UPDATE_MANY_SUCCESS,
    UPDATE_FAIL,
    DELETE_SUCCESS,
    DELETE_MANY_SUCCESS,
    DELETE_FAIL,
    IMPORT_FAIL,
    BATCH_INSERT_FAIL,
    INVALID_JSON_DATA,

    // system errors
    UNEXPECTED_ERROR,
    METHOD_UNAVAILABLE,
    CAN_NOT_CREATE_DTO,
    ARTA_FILE_CREATE_ERROR,

    // admin
    ADMIN_MENU_HOME,
    ADMIN_MENU_MONITORING,
    ADMIN_MENU_DATA,
    ADMIN_MENU_ADVANCED,
    ADMIN_MENU_SCHEDULE,
    ADMIN_MENU_QUERY,
    ADMIN_MENU_DOCUMENTS,
    ADMIN_MENU_SIGN_OUT,
    ADMIN_USERS,
    ADMIN_SYSTEM_ERRORS,
    ADMIN_SERVICES_LAST_RUN,
    ADMIN_SERVICES_STATUS,
    ADMIN_BACKUP_SQL,
    ADMIN_BACKUP_MONGO,
    ADMIN_BACKUP_ELASTIC,
    ADMIN_SHORTCUTS,
    ADMIN_SYSTEM_ADMIN,
    ADMIN_RUNNING_SERVICES,
    ADMIN_RUNNING_SERVICES_COUNT,
    ADMIN_RUNNING_SERVICES_ON_THIS_SERVER,
    ADMIN_BACKUP,
    ADMIN_BACKUP_CREATE,
    ADMIN_BACKUP_RESTORE,
    ADMIN_BACKUP_FILES,
    ADMIN_DATABASE,
    ADMIN_DATABASE_STATUS,
    ADMIN_DATABASE_CREATE_INDEX,
    ADMIN_DATABASE_DELETE_ALL,
    ADMIN_DATABASE_SYNCH,
    ADMIN_DATABASE_INDEX_DEF,
    ADMIN_DATABASE_INDEX_SETTINGS,
    ADMIN_QUEUE,
    ADMIN_DATABASE_DELETE_OPTIONAL,
    ADMIN_SYSTEM_AND_SERVICES,
    ADMIN_STARTUP,
    ADMIN_SERVICE_STOP,
    ADMIN_SERVICE_START,
    ADMIN_FACTORY_RESET,
    ADMIN_SETTINGS,
    ADMIN_SETTINGS_RELOAD,
    ADMIN_SETTINGS_EDIT_CONFIG,
    ADMIN_SETTINGS_EDIT_TUNE,
    ADMIN_BACKUP_MSG1,
    ADMIN_BACKUP_MSG2,
    ADMIN_BACKUP_FILE_PATH,
    ADMIN_DATE_FROM,
    ADMIN_DATE_TO,
    ADMIN_BACKUP_CREATE_START,
    ADMIN_RESTORE_MSG1,
    ADMIN_RESTORE_MSG2,
    ADMIN_RESTORE_MSG3,
    ADMIN_RESTORE_DELETE_CURRENT_DATA,
    ADMIN_DOWNLOAD,
    ADMIN_REMOVE_BACKUP_FILE,
    ADMIN_DELETE_DO,
    ADMIN_IMPORT,
    ADMIN_EXPORT,
    ADMIN_NEW_RECORD,
    ADMIN_DATA_FIELDS,
    ADMIN_DATA_LIST,
    ADMIN_DATABASE_TITLE,
    ADMIN_DELETE,
    ADMIN_UPDATE,
    ADMIN_STATUS,
    ADMIN_RECORD_COUNT,
    ADMIN_DATABASE_SYNCH_TITLE,
    ADMIN_DATABASE_SYNCH_CONFIRM,
    ADMIN_DATABASE_SYNCH_RUNNING,
    ADMIN_FINISHED,
    ADMIN_DATABASE_INDEX_CREATE,
    ADMIN_DATABASE_INDEX_VIEW,
    ADMIN_DATABASE_INDEX_REMOVE,
    ADMIN_DATABASE_INDEX_CREATE_START,
    ADMIN_INDEX_TITLE,
    ADMIN_DATABASE_REMOVE,
    ADMIN_DELAY,
    ADMIN_DELETE_EXCLUDE,
    ADMIN_DELETE_INCLUDE,
    ADMIN_IGNORE,
    ADMIN_IMPORT_TITLE,
    ADMIN_IMPORT_EXCLUDE,
    ADMIN_IMPORT_INCLUDE,
    ADMIN_ELASTIC_INDEX_DEF,
    ADMIN_ELASTIC_SETTINGS,
    ADMIN_ELASTIC_SETTINGS_MSG1,
    ADMIN_ELASTIC_SETTINGS_CLASS_NAME,
    ADMIN_ELASTIC_SETTINGS_DESTINATION,
    ADMIN_ELASTIC_SETTINGS_ERR4,
    ADMIN_ELASTIC_SETTINGS_ERR3,
    ADMIN_ELASTIC_SETTINGS_ERR2,
    ADMIN_ELASTIC_SETTINGS_ERR1,
    ADMIN_DATABASE_STATUS_PARAM,
    ADMIN_QUEUE_STATUS,
    ADMIN_SERVICES,
    ADMIN_ONLINE_USERS,
    ADMIN_ENABLED_SERVICES_THIS,
    ADMIN_SERVICE, ADMIN_IS_ON,
    ADMIN_MENU_QUERY_TITLE,
    ADMIN_QUERY_NEW,
    ADMIN_QUERY_WRITE,
    ADMIN_GROUP,
    ADMIN_TITLE,
    ADMIN_HELP,
    ADMIN_CONFIRM,
    ADMIN_QUERY_DELETE_TITLE,
    ADMIN_QUERY,
    ADMIN_NEW,
    ADMIN_EDIT,
    ADMIN_DELETE2,
    ADMIN_IX,
    ADMIN_DELETE_QUEUE,
    ADMIN_TRIES,
    ADMIN_QUEUE_DELETE_EXCLUDE,
    ADMIN_QUEUE_DELETE_INCLUDE,
    ADMIN_NO_QUEUE,
    ADMIN_RABBIT_IS_ON,
    ADMIN_RABBIT_IS_OFF,
    ADMIN_NO_ERROR,
    ADMIN_ERRORS_DELETE,
    ADMIN_RECORDS,
    ADMIN_SETTINGS_EDIT,
    ADMIN_SETTINGS_MSG1,
    ADMIN_SETTINGS_MSG3,
    ADMIN_SETTINGS_MSG2,
    ADMIN_SETTINGS_LOADED,
    ADMIN_SETTINGS_UPDATED,
    ADMIN_SETTINGS_UPDATE_MSG_SENT,
    ADMIN_SETTINGS_UPDATE_FAILED,
    ADMIN_SERVICE_INCLUDE_ALL_SERVICES,
    ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES,
    ADMIN_SERVICE_CLASSES_TO_RESERVE,
    ADMIN_SERVICE_START_SERVICES_AT_END,
    ADMIN_DO,
    ADMIN_SERVICES_ARE_STOPPED,
    ADMIN_SERVICE_ALL_STOPPED,
    ADMIN_SERVICE_ALL_STARTED,
    ADMIN_SYSYEM_CLASSES,
    ADMIN_DATA_ENTRY,
    ADMIN_DELETE_ALL_CONFIRM,
    ADMIN_BATCH_DELETE,
    ADMIN_BATCH_EDIT,
    ADMIN_JSON_OPTION,
    ADMIN_SORT,
    ADMIN_SEARCH,
    ADMIN_FROM,
    ADMIN_ADMIN_PANEL,
    ADMIN_SUBMIT,
    ADMIN_CACHE,
    ADMIN_SERVICE_IS_OFF,
    ADMIN_SIGN_IN,
    INVALID_METHOD,
    USERNAME,
    PASSWORD,
    SIGN_IN,

    BUSINESS_WRITTEN_COUNT,
    BUSINESS_ERROR_COUNT,
    BUSINESS_DUPLICATE_COUNT,
    BUSINESS_SERIAL_MAX,

    NO_SEARCH_COMMAND,

    ADMIN_DELETE_TOKEN,
    ADMIN_DELETE_TOKEN_DESCRIPTION,
    ADMIN_AUTH_TOKEN,
    ADMIN_SIGNUP_TOKEN_TEMP,
    ADMIN_SIGNIN_TOKEN_TEMP,
    ADMIN_RECOVER_TOKEN_TEMP,

    ADMIN_REQUIRES_ROOT,

    ADMIN_DATA,

    ADMIN_SCHEDULE_REPEAT_AT,
    ADMIN_SCHEDULE_START_AT,
    ADMIN_SCHEDULE_RUN,
    ADMIN_SCHEDULE_RUN_SUCCESS,
    ADMIN_SCHEDULE_RUN_FAIL,

    SHOW_NOT_DELETED,
    SHOW_DELETED,
    SHOW_ALL,

    LOGICAL_DELETED,
    LOGICAL_DELETED_UNDO,
    ADMIN_ACTION_LOG,

    ADMIN_PAGING,

    DELETE_FAIL_HAS_DEPENDENCIES,

    ADMIN_AUTH_FAILED,

    ADMIN_ACTION_LOG_USER,
    ADMIN_ACTION_LOG_REQUEST,
    ADMIN_MEMORY,

    ADMIN_BACKUP_UPLOAD,
    ADMIN_BACKUP_UPLOAD_FILE,

    SELECT_ALL,
    ADMIN_RESET_SIGNIN_FAILS,

    METHOD_CALL_TIME_LIMIT,

}
