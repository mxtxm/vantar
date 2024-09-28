package com.vantar.locale;


public enum VantarKey implements LangKey {








    // auth
    USERNAME,
    PASSWORD,
    SIGN_IN,
    SIGN_OUT,
    USER_OR_PASSWORD_EMPTY,
    USER_NOT_EXISTS,
    USER_DISABLED,
    USER_DISABLED_MAX_FAILED,
    USER_REPO_NOT_SET,
    USER_ALREADY_SIGNED_IN,
    USER_WRONG_PASSWORD,
    NO_ACCESS,
    MISSING_AUTH_TOKEN,
    INVALID_AUTH_TOKEN,
    EXPIRED_AUTH_TOKEN,

    // datetime
    INVALID_TIME,
    INVALID_DATE,
    INVALID_DATETIME,
    INVALID_TIMEZONE,

    // validation
    REQUIRED,
    REQUIRED_OR,
    REQUIRED_XOR,
    UNIQUE,
    INVALID_FIELD,
    INVALID_VALUE,
    INVALID_VALUE_TYPE,
    INVALID_ID,
    INVALID_GEO_LOCATION,
    INVALID_LENGTH,
    INVALID_FORMAT,
    MAX_EXCEED,
    MIN_EXCEED,
    MISSING_REFERENCE,
    ILLEGAL_FIELD,
    IO_ERROR,
    FILE_TYPE,
    FILE_SIZE,
    CUSTOM_EVENT_ERROR,
    SEARCH_COL_MISSING,
    SEARCH_VALUE_MISSING,
    SEARCH_VALUE_INVALID,
    SEARCH_CONDITION_TYPE_INVALID,

    // other validation and errors
    METHOD_UNAVAILABLE,
    METHOD_CALL_TIME_LIMIT,
    HTTP_METHOD_INVALID,
    HTTP_POST_MULTIPART,
    UNEXPECTED_ERROR,
    CAN_NOT_CREATE_DTO,
    ARTA_FILE_CREATE_ERROR,
    DELETE_DEPENDANT_DATA_ERROR,

    // data
    FAIL_FETCH,
    NO_CONTENT,
    SUCCESS_INSERT,
    FAIL_INSERT,
    SUCCESS_UPDATE,
    FAIL_UPDATE,
    SUCCESS_DELETE,
    FAIL_DELETE,
    INVALID_JSON_DATA,
    FAIL_IMPORT,
    SUCCESS_UPLOAD,
    FAIL_UPLOAD,
    SUCCESS_COUNT,
    FAIL_COUNT,
    DUPLICATE_COUNT,
    AUTO_INCREMENT_MAX,

    // admin - menu
    ADMIN_MENU_HOME,
    ADMIN_MENU_MONITORING,
    ADMIN_MENU_DATA,
    ADMIN_MENU_ADVANCED,
    ADMIN_MENU_SCHEDULE,
    ADMIN_MENU_PATCH,
    ADMIN_MENU_QUERY,
    ADMIN_MENU_TEST,
    ADMIN_MENU_DOCUMENTS,
    ADMIN_MENU_BUGGER,

    // admin
    ADMIN_SYSTEM_ADMINISTRATION,
    ADMIN_SHORTCUTS,
    ADMIN_MEMORY,
    ADMIN_ACTION,
    ADMIN_TITLE,
    ADMIN_CONFIRM,
    ADMIN_SUBMIT,
    ADMIN_DISK_SPACE,
    ADMIN_PROCESSOR,
    ADMIN_DELAY,
    ADMIN_FAILED,
    ADMIN_ATTEMPT_COUNT,
    ADMIN_EXCLUDE,
    ADMIN_INCLUDE,

    ADMIN_USER,
    ADMIN_USER_ADMIN_SIGN_IN_TITLE,
    ADMIN_USER_ONLINE,
    ADMIN_USER_ACTIVITY,
    ADMIN_USER_RESET_SIGNIN_FAILS,
    ADMIN_USER_AUTH_TOKEN,
    ADMIN_USER_DELETE_TOKEN,
    ADMIN_USER_SIGNUP_TOKEN_TEMP,
    ADMIN_USER_SIGNIN_TOKEN_TEMP,
    ADMIN_USER_RECOVER_TOKEN_TEMP,

    // admin - monitoring
    ADMIN_SYSTEM_ERRORS,
    ADMIN_SYSTEM_ERRORS_DELETE,
    ADMIN_SYSTEM_HEALTH_WEBSERVICE,
    ADMIN_SYSTEM_OBJECTS,

    ADMIN_SERVICE,
    ADMIN_SERVICE_ACTION,
    ADMIN_SERVICE_STOPPED,
    ADMIN_SERVICE_STARTED,
    ADMIN_SERVICE_PAUSED,
    ADMIN_SERVICE_RESUMED,
    ADMIN_SERVICE_IS_ON,
    ADMIN_SERVICE_IS_OFF,
    ADMIN_SERVICE_IS_DISABLED,
    ADMIN_SERVICE_IS_ENABLED,
    ADMIN_SERVICE_ALL_SERVERS,

    ADMIN_SERVICES,
    ADMIN_SERVICES_BEAT,
    ADMIN_SERVICES_STATUS,
    ADMIN_SERVICES_RUNNING_ME,
    ADMIN_SERVICES_RUNNING_OTHER,
    ADMIN_SERVICES_RUNNING_LOGS,
    ADMIN_SERVICES_RUNNING_DATA_SOURCE,

    // admin - queue
    ADMIN_QUEUE,
    ADMIN_QUEUE_STATUS,
    ADMIN_QUEUE_DELETE,
    ADMIN_QUEUE_NO_QUEUE,
    ADMIN_QUEUE_SELECTIVE_DELETE,

    // admin - database
    ADMIN_DATABASE,
    ADMIN_DATABASE_AUTOINCREMENT,
    ADMIN_DATABASE_AUTOINCREMENT_CREATE,
    ADMIN_DATABASE_INDEX,
    ADMIN_DATABASE_INDEX_CREATE,
    ADMIN_DATABASE_INDEX_REMOVE,
    ADMIN_DATABASE_INDEX_SETTINGS,
    ADMIN_DATABASE_SYNCH_TITLE,
    ADMIN_DATABASE_SYNCH,
    ADMIN_DATABASE_STATUS,

    // admin - advanced
    ADMIN_SYSTEM_AND_SERVICES,
    ADMIN_STARTUP,
    ADMIN_FACTORY_RESET,

    ADMIN_SETTINGS,
    ADMIN_SETTINGS_EDIT,
    ADMIN_SETTINGS_EDIT_CONFIG,
    ADMIN_SETTINGS_EDIT_TUNE,
    ADMIN_SETTINGS_RELOAD,
    ADMIN_SETTINGS_LOADED,
    ADMIN_SETTINGS_UPDATE_MSG_SENT,
    ADMIN_SETTINGS_MSG1,
    ADMIN_SETTINGS_MSG3,
    ADMIN_SETTINGS_MSG2,

    // admin - schedule
    ADMIN_SCHEDULE_REPEAT_AT,
    ADMIN_SCHEDULE_START_AT,
    ADMIN_SCHEDULE_RUN,
    ADMIN_SCHEDULE_RUN_SUCCESS,

    // admin - patch
    ADMIN_PATCH_FAIL_MSG,
    ADMIN_PATCH_SUCCESS_MSG,
    ADMIN_PATCH_RUN_TIME,

    // admin - query
    ADMIN_QUERY,
    ADMIN_QUERY_NEW,

    // admin - test
    ADMIN_TEST_RUN,

    // admin - documentation
    ADMIN_DOCUMENTATION_WEBSERVICE_INDEX_TITLE,

    // admin - data
    ADMIN_VIEW,
    ADMIN_REVERT,
    ADMIN_REFRESH,
    ADMIN_CACHE,
    ADMIN_IMPORT,
    ADMIN_EXPORT,
    ADMIN_INSERT,
    ADMIN_UPDATE,
    ADMIN_UPDATE_PROPERTY,
    ADMIN_DATA_PURGE,
    ADMIN_UNDELETE,
    ADMIN_DELETE,
    ADMIN_DELETE_CASCADE,
    ADMIN_DELETE_IGNORE_DEPENDENCIES,
    ADMIN_DELETE_ALL_CONFIRM,
    ADMIN_DELETE_CONFIRM,
    ADMIN_DEPENDENCIES,

    ADMIN_UNDELETED,
    ADMIN_REVERTED,
    ADMIN_FINISHED,

    ADMIN_DATE_FROM,
    ADMIN_DATE_TO,
    ADMIN_DATA_FIELDS,
    ADMIN_DATA_LIST,
    ADMIN_SELECT_ALL,

    ADMIN_SORT,
    ADMIN_SEARCH,
    ADMIN_PAGING,

    ADMIN_WEB,
    ADMIN_LOG_DIFFERENCES,
    ADMIN_LOG_WEB,
    ADMIN_LOG_ACTION,
    ADMIN_LIST_OPTION_ACTION_LOG,
    ADMIN_LIST_OPTION_USER_ACTIVITY,

    ADMIN_BACKUP,
    ADMIN_BACKUP_CREATE,
    ADMIN_BACKUP_FILES,
    ADMIN_BACKUP_FILE_PATH,
    ADMIN_BACKUP_UPLOAD,
    ADMIN_BACKUP_UPLOAD_FILE,
    ADMIN_BACKUP_CREATE_START,
    ADMIN_RESTORE,
    ADMIN_RESTORE_DELETE_CURRENT_DATA,

    ADMIN_ELASTIC_INDEX_DEF,
    ADMIN_ELASTIC_SETTINGS,
    ADMIN_ELASTIC_SETTINGS_MSG1,
    ADMIN_ELASTIC_SETTINGS_CLASS_NAME,
    ADMIN_ELASTIC_SETTINGS_DESTINATION,
    ADMIN_ELASTIC_SETTINGS_ERR4,
    ADMIN_ELASTIC_SETTINGS_ERR3,
    ADMIN_ELASTIC_SETTINGS_ERR2,
    ADMIN_ELASTIC_SETTINGS_ERR1,

}
