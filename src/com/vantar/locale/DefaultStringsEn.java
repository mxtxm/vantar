package com.vantar.locale;

import java.util.*;


public class DefaultStringsEn {

    private static final Map<LangKey, String> tokens = new HashMap<>();

    static {
        // auth
        tokens.put(VantarKey.USER_PASSWORD_EMPTY, "Username or password is empty");
        tokens.put(VantarKey.USER_NOT_EXISTS, "User does not exists");
        tokens.put(VantarKey.USER_DISABLED, "User is disabled");
        tokens.put(VantarKey.WRONG_PASSWORD, "Wrong password");
        tokens.put(VantarKey.MISSING_AUTH_TOKEN, "Missing authentication token");
        tokens.put(VantarKey.INVALID_AUTH_TOKEN, "Invalid authentication token");
        tokens.put(VantarKey.EXPIRED_AUTH_TOKEN, "Expired authentication token");
        tokens.put(VantarKey.NO_ACCESS, "No access");
        tokens.put(VantarKey.USER_REPO_NOT_SET, "User repo event not set, authentication requires repo to access database");

        // datetime
        tokens.put(VantarKey.INVALID_DATETIME, "Invalid datetime");
        tokens.put(VantarKey.INVALID_DATE, "Invalid date");
        tokens.put(VantarKey.INVALID_TIME, "Invalid time");
        tokens.put(VantarKey.INVALID_TIMEZONE, "Invalid timezone");

        // validation
        tokens.put(VantarKey.REQUIRED, "{0}: is required");
        tokens.put(VantarKey.DATA_TYPE, "{0}: invalid data type");
        tokens.put(VantarKey.UNIQUE, "{0}: must be unique");
        tokens.put(VantarKey.ILLEGAL, "{0}: illegal access");
        tokens.put(VantarKey.EMPTY_ID, "{0}: can not be empty");
        tokens.put(VantarKey.INVALID_ID, "{0}: invalid id");
        tokens.put(VantarKey.REGEX, "{0}: invalid data");
        tokens.put(VantarKey.STRING_LENGTH_EXCEED, "{0}: string to long");
        tokens.put(VantarKey.MAX_EXCEED, "{0}: value too big");
        tokens.put(VantarKey.MIN_EXCEED, "{0}: value too small");
        tokens.put(VantarKey.INVALID_VALUE, "{0}: value is not valid");
        tokens.put(VantarKey.INVALID_FIELD, "{0}: field is not valid");
        tokens.put(VantarKey.EVENT_REJECT, "{0}: custom validation error");
        tokens.put(VantarKey.IO_ERROR, "{0}: file upload system error");
        tokens.put(VantarKey.FILE_SIZE, "{0}: file upload, allowed file-size ({1})");
        tokens.put(VantarKey.FILE_TYPE, "{0}: unsupported file type, allowed file-type ({1})");
        tokens.put(VantarKey.NO_SEARCH_COMMAND, "no search commands");
        tokens.put(VantarKey.INVALID_METHOD, "invalid HTTP method");
        tokens.put(VantarKey.SEARCH_PARAM_INVALID_CONDITION_TYPE, "search condition requires condition type");
        tokens.put(VantarKey.SEARCH_PARAM_COL_MISSING, "search condition requires search column");
        tokens.put(VantarKey.INVALID_GEO_LOCATION, "invalid location");

        // data fetch
        tokens.put(VantarKey.FETCH_FAIL, "Data fetch failed");
        tokens.put(VantarKey.NO_CONTENT, "No content");

        // data write
        tokens.put(VantarKey.UPLOAD_SUCCESS, "File uploaded successfully");
        tokens.put(VantarKey.INSERT_SUCCESS, "Data inserted successfully");
        tokens.put(VantarKey.INSERT_MANY_SUCCESS, "{0} items inserted successfully");
        tokens.put(VantarKey.INSERT_FAIL, "Insert failed");
        tokens.put(VantarKey.UPDATE_SUCCESS, "Data updated successfully");
        tokens.put(VantarKey.UPDATE_MANY_SUCCESS, "({0}) items updated successfully");
        tokens.put(VantarKey.UPDATE_FAIL, "Update failed");
        tokens.put(VantarKey.DELETE_SUCCESS, "Data deleted successfully");
        tokens.put(VantarKey.DELETE_MANY_SUCCESS, "({0}) items deleted successfully");
        tokens.put(VantarKey.DELETE_FAIL, "Delete failed");
        tokens.put(VantarKey.IMPORT_FAIL, "Data import failed");
        tokens.put(VantarKey.BATCH_INSERT_FAIL, "Batch insert failed");
        tokens.put(VantarKey.INVALID_JSON_DATA, "Invalid json data");

        // system errors
        tokens.put(VantarKey.UNEXPECTED_ERROR, "Unexpected server error");
        tokens.put(VantarKey.METHOD_UNAVAILABLE, "Server error: method({0}) is unavailable");
        tokens.put(VantarKey.CAN_NOT_CREATE_DTO, "Server error: failed to create data object");
        tokens.put(VantarKey.ARTA_FILE_CREATE_ERROR, "Server error: Arta SQL database synch failed");

        // admin
        tokens.put(VantarKey.ADMIN_MENU_HOME, "Home");
        tokens.put(VantarKey.ADMIN_MENU_MONITORING, "Monitoring");
        tokens.put(VantarKey.ADMIN_MENU_DATA, "Data");
        tokens.put(VantarKey.ADMIN_MENU_ADVANCED, "Advanced");
        tokens.put(VantarKey.ADMIN_MENU_SCHEDULE, "Schedule");
        tokens.put(VantarKey.ADMIN_MENU_QUERY, "Queries");
        tokens.put(VantarKey.ADMIN_MENU_DOCUMENTS, "Documents");
        tokens.put(VantarKey.ADMIN_MENU_SIGN_OUT, "Sign out");

        tokens.put(VantarKey.ADMIN_USERS, "Users");
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS, "System errors");
        tokens.put(VantarKey.ADMIN_SERVICES_LAST_RUN, "Services last run");
        tokens.put(VantarKey.ADMIN_SERVICES_STATUS, "Service status");
        tokens.put(VantarKey.ADMIN_BACKUP_SQL, "SQL backup");
        tokens.put(VantarKey.ADMIN_BACKUP_MONGO, "Mongo backup");
        tokens.put(VantarKey.ADMIN_BACKUP_ELASTIC, "Elastic backup");
        tokens.put(VantarKey.ADMIN_SHORTCUTS, "Shortcut links");

        tokens.put(VantarKey.ADMIN_SYSTEM_ADMIN, "System administration");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES, "Started services");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_COUNT, " Service (count)");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_ON_THIS_SERVER, "On this server");

        tokens.put(VantarKey.ADMIN_BACKUP, "Data backup");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE, "Create backup");
        tokens.put(VantarKey.ADMIN_BACKUP_RESTORE, "Restore backup");
        tokens.put(VantarKey.ADMIN_BACKUP_FILES, "Backup files");

        tokens.put(VantarKey.ADMIN_DATABASE, "Database administrator");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS, "status");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "status {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_CREATE_INDEX, "Create indexes");
        tokens.put(VantarKey.ADMIN_DATABASE_IMPORT, "Import init data");
        tokens.put(VantarKey.ADMIN_DATABASE_DELETE_ALL, "Delete all data");
        tokens.put(VantarKey.ADMIN_DATABASE_DELETE_OPTIONAL, "Delete selected data");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH, "Synch");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_DEF, "Index definition");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "Index settings");

        tokens.put(VantarKey.ADMIN_QUEUE, "Queue administration");

        tokens.put(VantarKey.ADMIN_SYSTEM_AND_SERVICES, "System and services administrator");
        tokens.put(VantarKey.ADMIN_STARTUP, "Startup");
        tokens.put(VantarKey.ADMIN_SERVICE_STOP, "Stop service");
        tokens.put(VantarKey.ADMIN_SERVICE_START, "Start service");
        tokens.put(VantarKey.ADMIN_FACTORY_RESET, "Factory reset");

        tokens.put(VantarKey.ADMIN_SETTINGS, "Settings");
        tokens.put(VantarKey.ADMIN_SETTINGS_RELOAD, "Reload");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "Edit Config");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "Edit Tune");

        tokens.put(VantarKey.ADMIN_BACKUP_MSG1, "* Creating backup may take a long time");
        tokens.put(VantarKey.ADMIN_BACKUP_MSG2, "* Turn off services before creating backup");
        tokens.put(VantarKey.ADMIN_BACKUP_FILE_PATH, "Backup file path on server");
        tokens.put(VantarKey.ADMIN_DATE_FROM, "From date");
        tokens.put(VantarKey.ADMIN_DATE_TO, "To date");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE_START, "Create backup");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG1, "* All data will be deleted");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG2, "* Restoring huge amount of data is time consuming");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG3, "* Turn off services before data restoration");
        tokens.put(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "Delete all data");
        tokens.put(VantarKey.ADMIN_DOWNLOAD, " Download backup file ");
        tokens.put(VantarKey.ADMIN_REMOVE_BACKUP_FILE, " Delete backup files ");
        tokens.put(VantarKey.ADMIN_DELETE_DO, "Delete");
        tokens.put(VantarKey.ADMIN_DATA_FIELDS, "Fields");
        tokens.put(VantarKey.ADMIN_DATA_LIST, "List");
        tokens.put(VantarKey.ADMIN_NEW_RECORD, "New");
        tokens.put(VantarKey.ADMIN_IMPORT, "Import");
        tokens.put(VantarKey.ADMIN_DATABASE_TITLE, "Database");
        tokens.put(VantarKey.ADMIN_DELETE, "Delete");
        tokens.put(VantarKey.ADMIN_UPDATE, "Update");
        tokens.put(VantarKey.ADMIN_STATUS, "{0} status");
        tokens.put(VantarKey.ADMIN_RECORD_COUNT, "{0} records");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "{0} synch");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_CONFIRM, "Start synch");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_RUNNING, "Synchronizing database with system objects...");
        tokens.put(VantarKey.ADMIN_FINISHED, "Finished");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "Create indexes for {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_VIEW, "View indexes...");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "Delete indexes if exists");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START, "Creating indexed");
        tokens.put(VantarKey.ADMIN_INDEX_TITLE, "Indexes of {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_REMOVE, "Removing database {0} ...");
        tokens.put(VantarKey.ADMIN_DELAY, "Delay in seconds");
        tokens.put(VantarKey.ADMIN_DELETE_EXCLUDE, "Exclude objects");
        tokens.put(VantarKey.ADMIN_DELETE_INCLUDE, "Include objects");
        tokens.put(VantarKey.ADMIN_IGNORE, "Pass on");
        tokens.put(VantarKey.ADMIN_IMPORT_TITLE, "Import init data {0} ...");
        tokens.put(VantarKey.ADMIN_IMPORT_EXCLUDE, "Exclude objects");
        tokens.put(VantarKey.ADMIN_ELASTIC_INDEX_DEF, "ELASTIC index definition ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS, "ELASTIC index settings ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1, "You may shrink, clone or refresh target indexes");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME, "Target index class name");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION, "Destination index name");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1, "Target index class is required");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2, "Index copied successfully");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3, "Index shrinked successfully");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4, "Index refreshed successfully");
        tokens.put(VantarKey.ADMIN_QUEUE_STATUS, "Queue status");
        tokens.put(VantarKey.ADMIN_SERVICES, "Services");
        tokens.put(VantarKey.ADMIN_ONLINE_USERS, "Online users");
        tokens.put(VantarKey.ADMIN_AUTH_IS_DISABLED, "Authentication service is disabled");
        tokens.put(VantarKey.ADMIN_DELETE_TOKEN, "Delete authentication token");
        tokens.put(VantarKey.ADMIN_DELETE_TOKEN_DESCRIPTION, "Delete authentication token description");
        tokens.put(VantarKey.ADMIN_AUTH_TOKEN, "Authentication token");
        tokens.put(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP, "Temp signup tokens");
        tokens.put(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP, "Temp signin tokens");
        tokens.put(VantarKey.ADMIN_RECOVER_TOKEN_TEMP, "Temp recovery tokens");

        tokens.put(VantarKey.ADMIN_ENABLED_SERVICES_THIS, "Services enabled on this server");
        tokens.put(VantarKey.ADMIN_SERVICE, "Service");
        tokens.put(VantarKey.ADMIN_IS_ON, "Is on");
        tokens.put(VantarKey.ADMIN_MENU_QUERY_TITLE, "Queries");
        tokens.put(VantarKey.ADMIN_QUERY_NEW, "New query");
        tokens.put(VantarKey.ADMIN_QUERY_WRITE, "Create query");
        tokens.put(VantarKey.ADMIN_GROUP, "Group");
        tokens.put(VantarKey.ADMIN_TITLE, "Title");
        tokens.put(VantarKey.ADMIN_HELP, "Help...");
        tokens.put(VantarKey.ADMIN_CONFIRM, "I'm sure");
        tokens.put(VantarKey.ADMIN_QUERY_DELETE_TITLE, "Delete queries");
        tokens.put(VantarKey.ADMIN_QUERY, "Queries");
        tokens.put(VantarKey.ADMIN_EDIT, "Update");
        tokens.put(VantarKey.ADMIN_DELETE2, "Delete");
        tokens.put(VantarKey.ADMIN_NEW, "New");
        tokens.put(VantarKey.ADMIN_IX, "Index");
        tokens.put(VantarKey.ADMIN_DELETE_QUEUE, "Delete queues");
        tokens.put(VantarKey.ADMIN_TRIES, "Try count");
        tokens.put(VantarKey.ADMIN_QUEUE_DELETE_EXCLUDE, "Exclude queues");
        tokens.put(VantarKey.ADMIN_QUEUE_DELETE_INCLUDE, "Include queues");
        tokens.put(VantarKey.ADMIN_NO_QUEUE, "No queues");
        tokens.put(VantarKey.ADMIN_RABBIT_IS_ON, "RabbitMQ is up");
        tokens.put(VantarKey.ADMIN_RABBIT_IS_OFF, "RabbitMQ is or down or connection is broken");
        tokens.put(VantarKey.ADMIN_NO_ERROR, "There are not errors");
        tokens.put(VantarKey.ADMIN_ERRORS_DELETE, "Remove system errors");
        tokens.put(VantarKey.ADMIN_RECORDS, " :record ");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT, "Update settings");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG1, "Settings can not be written to files");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG2, "The settings in this file are server specific and will only be updated on this server");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG3, "The settings in this file are common across all servers and will be updated on all servers");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATED, "Settings updated");
        tokens.put(VantarKey.ADMIN_SETTINGS_LOADED, "Settings loaded");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT, "Update settings message sent to other servers");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_FAILED, "Settings failed to update");
        tokens.put(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES, "Include all services");
        tokens.put(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES, "Include database and queue services/connections");
        tokens.put(VantarKey.ADMIN_SERVICE_CLASSES_TO_RESERVE, "Objetcs to be preserved");
        tokens.put(VantarKey.ADMIN_SERVICE_START_SERVICES_AT_END, "Turn on services at the end");
        tokens.put(VantarKey.ADMIN_DO, "DO");
        tokens.put(VantarKey.ADMIN_SERVICES_ARE_STOPPED, "Services are stopped");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_STOPPED, "All services are stopped successfully");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_STARTED, "All services are started successfully");
        tokens.put(VantarKey.ADMIN_SYSYEM_CLASSES, "System classes");
        tokens.put(VantarKey.ADMIN_DATA_ENTRY, "Data entry");
        tokens.put(VantarKey.ADMIN_DELETE_ALL_CONFIRM, "Confirm delete all?");
        tokens.put(VantarKey.ADMIN_BATCH_EDIT, "Batch update");
        tokens.put(VantarKey.ADMIN_BATCH_DELETE, "Batch delete");
        tokens.put(VantarKey.ADMIN_JSON_OPTION, "JSON/option");
        tokens.put(VantarKey.ADMIN_PAGE, "Page");
        tokens.put(VantarKey.ADMIN_SORT, "Sort");
        tokens.put(VantarKey.ADMIN_SEARCH, "Search");
        tokens.put(VantarKey.ADMIN_FROM, "From");
        tokens.put(VantarKey.ADMIN_ADMIN_PANEL, "Administrator dashboard");
        tokens.put(VantarKey.ADMIN_SUBMIT, "Submit");
        tokens.put(VantarKey.ADMIN_CACHE, "Cached data");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_OFF, "{0} is not enabled");
        tokens.put(VantarKey.ADMIN_SIGN_IN, "Sign into Administrator dashboard");
        tokens.put(VantarKey.ADMIN_DATA, "Data");

        tokens.put(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, "repeat at");
        tokens.put(VantarKey.ADMIN_SCHEDULE_START_AT, "start at");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN, "run manually now");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_FAIL, "run failed");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS, "run successfully");

        tokens.put(VantarKey.USERNAME, "Username");
        tokens.put(VantarKey.PASSWORD, "Password");
        tokens.put(VantarKey.SIGN_IN, "Sign in");

        // business
        tokens.put(VantarKey.BUSINESS_WRITTEN_COUNT,   "Added records    ({0})");
        tokens.put(VantarKey.BUSINESS_ERROR_COUNT,     "Error count      ({0})");
        tokens.put(VantarKey.BUSINESS_DUPLICATE_COUNT, "Duplicate record ({0})");
        tokens.put(VantarKey.BUSINESS_SERIAL_MAX,      "Last primary key ({0})");

        tokens.put(VantarKey.SHOW_DELETED, "Show deleted");
        tokens.put(VantarKey.LOGICAL_DELETED, "Soft delete (mark record as deleted)");
        tokens.put(VantarKey.LOGICAL_DELETED_UNDO, "Undelete");
        tokens.put(VantarKey.ADMIN_ACTION_LOG, "Log");

        tokens.put(VantarKey.ADMIN_N_PER_PAGE, "n per page");

        tokens.put(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, "Not deleted because of dependencies");
    }

    public static String getString(LangKey key) {
        return tokens.get(key);
    }
}
