package com.vantar.locale;

import java.util.*;


public class DefaultStringsEn {

    private static final Map<LangKey, String> tokens;

    static {
        tokens = new HashMap<>(300, 1);

        // auth
        tokens.put(VantarKey.USERNAME, "Username");
        tokens.put(VantarKey.PASSWORD, "Password");
        tokens.put(VantarKey.SIGN_IN, "Sign in");
        tokens.put(VantarKey.SIGN_OUT, "Sign out");
        tokens.put(VantarKey.USER_OR_PASSWORD_EMPTY, "Username or password is empty.");
        tokens.put(VantarKey.USER_NOT_EXISTS, "User does not exists.");
        tokens.put(VantarKey.USER_DISABLED, "User is disabled.");
        tokens.put(VantarKey.USER_DISABLED_MAX_FAILED, "User is disabled because of too many failed sign-in attempts, please contact administrator to unlock the user.");
        tokens.put(VantarKey.USER_REPO_NOT_SET, "User repo event has not been not set to auth service.");
        tokens.put(VantarKey.USER_ALREADY_SIGNED_IN, "User is already signed-in on another device.");
        tokens.put(VantarKey.USER_WRONG_PASSWORD, "Wrong password.");
        tokens.put(VantarKey.NO_ACCESS, "No access.");
        tokens.put(VantarKey.MISSING_AUTH_TOKEN, "Missing authentication token.");
        tokens.put(VantarKey.INVALID_AUTH_TOKEN, "Invalid authentication token.");
        tokens.put(VantarKey.EXPIRED_AUTH_TOKEN, "Expired authentication token.");

        // datetime
        tokens.put(VantarKey.INVALID_TIME, "Invalid time.");
        tokens.put(VantarKey.INVALID_DATE, "Invalid date.");
        tokens.put(VantarKey.INVALID_DATETIME, "Invalid datetime.");
        tokens.put(VantarKey.INVALID_TIMEZONE, "Invalid timezone.");

        // validation
        tokens.put(VantarKey.REQUIRED, "\"{0}\": is required.");
        tokens.put(VantarKey.REQUIRED_OR, "\"{0}\": at least one field is required.");
        tokens.put(VantarKey.REQUIRED_XOR, "\"{0}\": one and only one field is required.");
        tokens.put(VantarKey.UNIQUE, "\"{0}\": must be unique.");
        tokens.put(VantarKey.INVALID_FIELD, "\"{0}\": field is not valid.");
        tokens.put(VantarKey.INVALID_VALUE, "\"{0}\": value is not valid.");
        tokens.put(VantarKey.INVALID_VALUE_TYPE, "\"{0}\": invalid data type.");
        tokens.put(VantarKey.INVALID_ID, "\"{0}\": invalid id.");
        tokens.put(VantarKey.INVALID_GEO_LOCATION, "\"{0}\": invalid location.");
        tokens.put(VantarKey.INVALID_LENGTH, "\"{0}\": invalid length.");
        tokens.put(VantarKey.INVALID_FORMAT, "\"{0}\": invalid data format.");
        tokens.put(VantarKey.MAX_EXCEED, "\"{0}\": value too big.");
        tokens.put(VantarKey.MIN_EXCEED, "\"{0}\": value too small.");
        tokens.put(VantarKey.MISSING_REFERENCE, "\"{0}\": reference ({1}) not exists.");
        tokens.put(VantarKey.ILLEGAL_FIELD, "\"{0}\": illegal access to field.");
        tokens.put(VantarKey.IO_ERROR, "IO, upload or download error.");
        tokens.put(VantarKey.FILE_TYPE, "\"{0}\": unsupported file type, allowed file-type(s)=({1}).");
        tokens.put(VantarKey.FILE_SIZE, "\"{0}\": suspicious file size, file-size=({1}).");
        tokens.put(VantarKey.CUSTOM_EVENT_ERROR, "\"{0}\": custom validation error.");
        tokens.put(VantarKey.SEARCH_COL_MISSING, "\"{0}\": search column missing.");
        tokens.put(VantarKey.SEARCH_VALUE_INVALID, "\"{0}\": search value is invalid.");
        tokens.put(VantarKey.SEARCH_VALUE_MISSING, "\"{0}\": search value is missing.");
        tokens.put(VantarKey.SEARCH_CONDITION_TYPE_INVALID, "\"{0}\": search condition type is missing or is not supported.");

        // other validation and errors
        tokens.put(VantarKey.METHOD_UNAVAILABLE, "Server error: method(\"{0}\") is unavailable.");
        tokens.put(VantarKey.METHOD_CALL_TIME_LIMIT, "Must wait \"{0}\"minutes. This request is costly and has limited access.");
        tokens.put(VantarKey.HTTP_METHOD_INVALID, "Invalid HTTP method.");
        tokens.put(VantarKey.HTTP_POST_MULTIPART, "Request must be POST (multipart/form-data encoded).");
        tokens.put(VantarKey.UNEXPECTED_ERROR, "Unexpected server error.");
        tokens.put(VantarKey.CAN_NOT_CREATE_DTO, "Server error: failed to create DTO data object.");
        tokens.put(VantarKey.ARTA_FILE_CREATE_ERROR, "Server error: Arta SQL database synch failed.");

        // data
        tokens.put(VantarKey.NO_CONTENT, "No content.");
        tokens.put(VantarKey.FAIL_FETCH, "Data fetch failed!");
        tokens.put(VantarKey.SUCCESS_INSERT, "Data inserted successfully.");
        tokens.put(VantarKey.FAIL_INSERT, "Data insert failed!");
        tokens.put(VantarKey.SUCCESS_UPDATE, "Data updated successfully.");
        tokens.put(VantarKey.FAIL_UPDATE, "Update failed!");
        tokens.put(VantarKey.SUCCESS_DELETE, "Data deleted successfully.");
        tokens.put(VantarKey.FAIL_DELETE, "Delete failed!");
        tokens.put(VantarKey.INVALID_JSON_DATA, "Invalid json data!");
        tokens.put(VantarKey.FAIL_IMPORT, "Data import failed!");
        tokens.put(VantarKey.SUCCESS_UPLOAD, "File uploaded successfully ({0})");
        tokens.put(VantarKey.FAIL_UPLOAD, "File uploaded failed ({0})!");

        // admin
        tokens.put(VantarKey.ADMIN_MENU_HOME, "Home");
        tokens.put(VantarKey.ADMIN_MENU_MONITORING, "Monitoring");
        tokens.put(VantarKey.ADMIN_MENU_DATA, "Data");
        tokens.put(VantarKey.ADMIN_MENU_ADVANCED, "Advanced");
        tokens.put(VantarKey.ADMIN_MENU_SCHEDULE, "Schedule");
        tokens.put(VantarKey.ADMIN_MENU_PATCH, "Patches");
        tokens.put(VantarKey.ADMIN_MENU_QUERY, "Queries");
        tokens.put(VantarKey.ADMIN_MENU_DOCUMENTS, "Documents");
        tokens.put(VantarKey.ADMIN_MENU_BUGGER, "Bug report");



        tokens.put(VantarKey.ADMIN_USERS, "Users");
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS, "System errors");
        tokens.put(VantarKey.ADMIN_SERVICES_BEAT, "Beat (messages)");
        tokens.put(VantarKey.ADMIN_SERVICES_STATUS, "Service status");
        tokens.put(VantarKey.ADMIN_SHORTCUTS, "Shortcut links");

        tokens.put(VantarKey.ADMIN_SYSTEM_ADMIN, "System administration");

        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_ME, "Services on this server");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_OTHER, "Services on other servers");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_LOGS, "Service logs");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_DATA_SOURCE, "Data sources");


        tokens.put(VantarKey.ADMIN_BACKUP, "Data backup");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE, "Create backup");
        tokens.put(VantarKey.ADMIN_RESTORE, "Restore backup");
        tokens.put(VantarKey.ADMIN_BACKUP_FILES, "Backup files");

        tokens.put(VantarKey.ADMIN_DATABASE, "Database administrator");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS, "Status");
        tokens.put(VantarKey.ADMIN_DATABASE_CREATE_SEQUENCE, "Create sequences");
        tokens.put(VantarKey.ADMIN_DATA_PURGE, "Purge");
        tokens.put(VantarKey.ADMIN_DELETE_OPTIONAL, "Delete selected data");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH, "Synch");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_DEF, "Index definition");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "Index settings");

        tokens.put(VantarKey.ADMIN_QUEUE, "Queue administration");

        tokens.put(VantarKey.ADMIN_SYSTEM_AND_SERVICES, "System and services administrator");
        tokens.put(VantarKey.ADMIN_STARTUP, "Startup");
        tokens.put(VantarKey.ADMIN_SERVICE_START, "Start service");
        tokens.put(VantarKey.ADMIN_FACTORY_RESET, "Factory reset");

        tokens.put(VantarKey.ADMIN_SETTINGS, "Settings");
        tokens.put(VantarKey.ADMIN_SETTINGS_RELOAD, "Reload");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "Edit Config");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "Edit Tune");

        tokens.put(VantarKey.ADMIN_BACKUP_FILE_PATH, "Backup file path on server");
        tokens.put(VantarKey.ADMIN_DATE_FROM, "From date");
        tokens.put(VantarKey.ADMIN_DATE_TO, "To date");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE_START, "Create backup");
        tokens.put(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "Delete all data");
        tokens.put(VantarKey.ADMIN_DELETE_DO, "Delete");
        tokens.put(VantarKey.ADMIN_DATA_FIELDS, "Fields");
        tokens.put(VantarKey.ADMIN_DATA_LIST, "List");
        tokens.put(VantarKey.ADMIN_INSERT, "New");
        tokens.put(VantarKey.ADMIN_IMPORT, "Import");
        tokens.put(VantarKey.ADMIN_EXPORT, "Export");
        tokens.put(VantarKey.ADMIN_DELETE, "Delete");
        tokens.put(VantarKey.ADMIN_UNDELETE, "Undelete");
        tokens.put(VantarKey.ADMIN_UNDELETED, "Undeleted {0} id {1}");
        tokens.put(VantarKey.ADMIN_REVERTED, "Reverted {0} id {1}");
        tokens.put(VantarKey.ADMIN_UPDATE, "Update");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "Synch database with definition");
        tokens.put(VantarKey.ADMIN_FINISHED, "Finished");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "Create indexes");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "Delete indexes if exists");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START, "Creating indexed");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX, "Database indexes");
        tokens.put(VantarKey.ADMIN_DATABASE_SEQUENCE, "Database sequences");
        tokens.put(VantarKey.ADMIN_DELAY, "Delay in seconds");
        tokens.put(VantarKey.ADMIN_ELASTIC_INDEX_DEF, "ELASTIC index definition ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS, "ELASTIC index settings ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1, "You may shrink, clone or refresh target indexes");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME, "Target index class name");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION, "Destination index name");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1, "Target index class is required");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2, "Index copied successfully");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3, "Index shrunk successfully");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4, "Index refreshed successfully");
        tokens.put(VantarKey.ADMIN_QUEUE_STATUS, "Queue status");
        tokens.put(VantarKey.ADMIN_SERVICES, "Services");
        tokens.put(VantarKey.ADMIN_ONLINE_USERS, "Online users");
        tokens.put(VantarKey.ADMIN_DELETE_TOKEN, "Delete authentication token");
        tokens.put(VantarKey.ADMIN_AUTH_TOKEN, "Authentication token");
        tokens.put(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP, "Temp signup tokens");
        tokens.put(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP, "Temp signin tokens");
        tokens.put(VantarKey.ADMIN_RECOVER_TOKEN_TEMP, "Temp recovery tokens");

        tokens.put(VantarKey.ADMIN_SERVICE, "Service");
        tokens.put(VantarKey.ADMIN_QUERY_NEW, "New query");
        tokens.put(VantarKey.ADMIN_TITLE, "Title");
        tokens.put(VantarKey.ADMIN_CONFIRM, "I'm sure");
        tokens.put(VantarKey.ADMIN_QUERY, "Queries");
        tokens.put(VantarKey.ADMIN_EDIT, "Update");
        tokens.put(VantarKey.ADMIN_NEW, "New");
        tokens.put(VantarKey.ADMIN_DELETE_QUEUE, "Delete queues");
        tokens.put(VantarKey.ADMIN_ATTEMPTS, "Attempts count");
        tokens.put(VantarKey.ADMIN_NO_QUEUE, "No queues");
        tokens.put(VantarKey.ADMIN_ERRORS_DELETE, "Remove system errors");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT, "Update settings");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG1, "Settings can not be written to files");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG2, "The settings in this file are server specific and will only be updated on this server");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG3, "The settings in this file are common across all servers and will be updated on all servers");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATED, "Settings updated");
        tokens.put(VantarKey.ADMIN_SETTINGS_LOADED, "Settings loaded");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT, "Update settings message sent to other servers");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_FAILED, "Settings failed to update");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_SERVERS, "On all servers");
        tokens.put(VantarKey.ADMIN_SERVICE_STOPPED, "Service(s) is(are) stopped");
        tokens.put(VantarKey.ADMIN_SERVICE_STARTED, "Service(s) is(are) started");
        tokens.put(VantarKey.ADMIN_SYSYEM_OBJECTS, "System objects");
        tokens.put(VantarKey.ADMIN_DELETE_ALL_CONFIRM, "Confirm delete all?");

        tokens.put(VantarKey.ADMIN_SORT, "Sort");
        tokens.put(VantarKey.ADMIN_SEARCH, "Search");
        tokens.put(VantarKey.ADMIN_ADMIN_PANEL, "Administrator dashboard");
        tokens.put(VantarKey.ADMIN_SUBMIT, "Submit");
        tokens.put(VantarKey.ADMIN_CACHE, "Cached data");


        tokens.put(VantarKey.ADMIN_SERVICE_IS_OFF, "{0} is down");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ON, "{0} is up");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_DISABLED, "{0} is not enabled");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ENABLED, "{0} is enabled");

        tokens.put(VantarKey.ADMIN_SIGN_IN, "Sign into Vantar Administrator dashboard");

        tokens.put(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, "repeat at");
        tokens.put(VantarKey.ADMIN_SCHEDULE_START_AT, "start at");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN, "run manually now");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS, "run successfully");

        tokens.put(VantarKey.ADMIN_RUN_TIME, "run time");



        // business
        tokens.put(VantarKey.BUSINESS_WRITTEN_COUNT,   "Added records");
        tokens.put(VantarKey.BUSINESS_ERROR_COUNT,     "Error count");
        tokens.put(VantarKey.BUSINESS_DUPLICATE_COUNT, "Duplicate records");
        tokens.put(VantarKey.BUSINESS_SERIAL_MAX,      "Last primary key");


        tokens.put(VantarKey.ADMIN_LIST_OPTION_ACTION_LOG, "Log");
        tokens.put(VantarKey.ADMIN_LIST_OPTION_USER_ACTIVITY, "Activity");

        tokens.put(VantarKey.ADMIN_PAGING, "paging");



        tokens.put(VantarKey.ADMIN_ACTION_LOG, "Action logs");
        tokens.put(VantarKey.ADMIN_USER_ACTIVITY, "User activity");
        tokens.put(VantarKey.ADMIN_MEMORY, "Memory");
        tokens.put(VantarKey.ADMIN_DISK_SPACE, "Disk space");
        tokens.put(VantarKey.ADMIN_PROCESSOR, "Processor");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD, "Upload backup file");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD_FILE, "Backup file");

        tokens.put(VantarKey.SELECT_ALL, "Select all");
        tokens.put(VantarKey.ADMIN_RESET_SIGNIN_FAILS, "Reset locked users caused by failed signins");


        tokens.put(VantarKey.ADMIN_FAIL, "Fail count");
        tokens.put(VantarKey.ADMIN_FAIL_MSG, "fail message");
        tokens.put(VantarKey.ADMIN_SUCCESS, "Success count");
        tokens.put(VantarKey.ADMIN_SUCCESS_MSG, "Success message");
        tokens.put(VantarKey.ADMIN_REFRESH, "Refresh");

        tokens.put(VantarKey.ADMIN_WEBSERVICE_INDEX_TITLE, "Webservice index");

        tokens.put(VantarKey.ADMIN_WEBSERVICE, "Health report webservices");

        tokens.put(VantarKey.ADMIN_ACTION, "Action");


        tokens.put(VantarKey.ADMIN_SERVICE_ACTION, "Service start/stop/restart");
        tokens.put(VantarKey.ADMIN_FAILED, "FAILED");
        tokens.put(VantarKey.ADMIN_INCLUDE, "Include");
        tokens.put(VantarKey.ADMIN_EXCLUDE, "Exclude");

        tokens.put(VantarKey.DELETE_DEPENDANT_DATA_ERROR, "Can not delete. ({0}, {1}) depends on this data record.");
        tokens.put(VantarKey.ADMIN_IGNORE_DEPENDENCIES, "Ignore dependencies");
        tokens.put(VantarKey.ADMIN_DEPENDENCIES, "Dependencies");
        tokens.put(VantarKey.ADMIN_DELETE_CASCADE, "Cascade delete");
        tokens.put(VantarKey.ADMIN_VIEW, "View");
        tokens.put(VantarKey.ADMIN_REVERT, "Revert");
        tokens.put(VantarKey.ADMIN_LOG_DIFFERENCES, "View differences");
        tokens.put(VantarKey.ADMIN_LOG_WEB, "Log web");
        tokens.put(VantarKey.ADMIN_WEB, "Web");
        tokens.put(VantarKey.ADMIN_MENU_TEST, "Test");
        tokens.put(VantarKey.ADMIN_UPDATE_PROPERTY, "Update property");

    }

    public static String getString(LangKey key) {
        return tokens.get(key);
    }
}
