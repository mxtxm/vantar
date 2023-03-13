package com.vantar.locale;

import java.util.*;


public class DefaultStringsFa {

    private static final Map<LangKey, String> tokens;

    static {
        tokens = new HashMap<>(300, 1);
        // auth
        tokens.put(VantarKey.USER_PASSWORD_EMPTY, "نام کاربری و یا رمز عبور خالی است");
        tokens.put(VantarKey.USER_NOT_EXISTS, "کاربر در سیستم وجود ندارد");
        tokens.put(VantarKey.USER_DISABLED, "کاربر غیر فعال است");
        tokens.put(VantarKey.USER_DISABLED_MAX_FAILED, "بعلت تلاش های ناموفق بیش از حد کاربر غیر فعال شد");
        tokens.put(VantarKey.WRONG_PASSWORD, "رمز عبور نادرست است");
        tokens.put(VantarKey.MISSING_AUTH_TOKEN, "شناسه هویت کاربر پیدا نشد، دوباره به سیستم وارد شوید");
        tokens.put(VantarKey.INVALID_AUTH_TOKEN, "شناسه هویت کاربر نادرست است، دوباره به سیستم وارد شوید");
        tokens.put(VantarKey.EXPIRED_AUTH_TOKEN, "شناسه هویت کاربر باطل شده است، دوباره به سیستم وارد شوید");
        tokens.put(VantarKey.NO_ACCESS, "کاربر شما به این صفحه دسترسی ندارد");
        tokens.put(VantarKey.USER_REPO_NOT_SET, "user repo event not set, authentication requires repo to access database");
        tokens.put(VantarKey.USER_ALREADY_SIGNED_IN, "کاربر هم اکنون در سیسم وارد شده.");

        // datetime
        tokens.put(VantarKey.INVALID_DATETIME, "{0}: تاریخ و زمان نادرست است");
        tokens.put(VantarKey.INVALID_DATE, "{0}: تاریخ نادرست است");
        tokens.put(VantarKey.INVALID_TIME, "{0}: زمان نادرست است");
        tokens.put(VantarKey.INVALID_TIMEZONE, "{0}: محدوده زمانی نادرست است");

        // validation
        tokens.put(VantarKey.REQUIRED, "{0}: وارد کردن اجباری است");
        tokens.put(VantarKey.REQUIRED_XOR, "{0}: وارد کردن تنها یکی از فیلدها اجباری است");
        tokens.put(VantarKey.REQUIRED_OR, "{0}: وارد کردن یکی از فیلدها اجباری است");
        tokens.put(VantarKey.DATA_TYPE, "{0}: نوع داده نادرست است");
        tokens.put(VantarKey.UNIQUE, "{0}: باید در سیستم یکتا باشد، این مقدار هم اکنون در سیستم وارد شده است");
        tokens.put(VantarKey.REFERENCE, "{0}: مبدا ({1}) وجود ندارد");
        tokens.put(VantarKey.PARENT_CHILD, "{0}: ارتباط فرزندی ناممکن است");
        tokens.put(VantarKey.ILLEGAL, "{0}: دسترسی ممنوع");
        tokens.put(VantarKey.EMPTY_ID, "{0}: نباید خالی باشد");
        tokens.put(VantarKey.INVALID_ID, "{0}: شناسه نادرست است");
        tokens.put(VantarKey.REGEX, "{0}: مقدار نادرست است");
        tokens.put(VantarKey.STRING_LENGTH_EXCEED, "{0}: زیادی بلند است");
        tokens.put(VantarKey.MAX_EXCEED, "{0}: زیادی بزرگ است");
        tokens.put(VantarKey.MIN_EXCEED, "{0}: زیادی کوچک است");
        tokens.put(VantarKey.INVALID_VALUE, "{0}: مقدار نادرست است");
        tokens.put(VantarKey.INVALID_FIELD, "{0}: فیلد نادرست است");
        tokens.put(VantarKey.EVENT_REJECT, "{0}: خطای شخصی");
        tokens.put(VantarKey.IO_ERROR, "{0}: خطای سیسمتی هنگام آپلود فایل");
        tokens.put(VantarKey.FILE_SIZE, "{0}: خطای آپلود فایل - سایز مجاز برای فایل ({1})");
        tokens.put(VantarKey.FILE_TYPE, "{0}: خطای آپلود فایل - فایل های مجاز برای آپلود ({1})");
        tokens.put(VantarKey.NO_SEARCH_COMMAND, "دستورات جستجتو فرستاده نشدند");
        tokens.put(VantarKey.INVALID_METHOD, "متد درخواست نادرست است");
        tokens.put(VantarKey.SEARCH_PARAM_INVALID_CONDITION_TYPE, "{0}: شرط جستجو باید نوع شرط داشته باشد");
        tokens.put(VantarKey.SEARCH_PARAM_COL_MISSING, "{0}: شرط جستجو باید فیلد جستجو داشته باشد");
        tokens.put(VantarKey.SEARCH_PARAM_VALUE_INVALID, "{0}: مقدار شرط جستحو نادرست است");
        tokens.put(VantarKey.SEARCH_PARAM_VALUE_MISSING, "{0}: مقدار شرط جستجو چا افتاده است");
        tokens.put(VantarKey.INVALID_GEO_LOCATION, "مکان جغرافیایی نادرست است");

        // data fetch
        tokens.put(VantarKey.FETCH_FAIL, "خطا هنگام خواندن داده ها");
        tokens.put(VantarKey.NO_CONTENT, "بدون محتوا");

        // data write
        tokens.put(VantarKey.UPLOAD_SUCCESS, "فایل با موفقیت آپلود شذ");
        tokens.put(VantarKey.UPLOAD_FAIL, "فایل با موفقیت آپلود نشذ");
        tokens.put(VantarKey.INSERT_SUCCESS, "داده ها با موفقیت وارد گردید");
        tokens.put(VantarKey.INSERT_MANY_SUCCESS, "{0} رکورد با موفقیت وارد گردید");
        tokens.put(VantarKey.INSERT_FAIL, "ورود اطلاعات دچار خطا شد");
        tokens.put(VantarKey.UPDATE_SUCCESS, "data updated successfully");
        tokens.put(VantarKey.UPDATE_MANY_SUCCESS, "({0}) items updated successfully");
        tokens.put(VantarKey.UPDATE_FAIL, "ویرایش اطلاعات دچار خطا شد");
        tokens.put(VantarKey.DELETE_SUCCESS, "data deleted successfully");
        tokens.put(VantarKey.DELETE_MANY_SUCCESS, "({0}) items deleted successfully");
        tokens.put(VantarKey.DELETE_FAIL, "پاک کردن اطلاعات دچار خطا شد");
        tokens.put(VantarKey.IMPORT_FAIL, "data import failed");
        tokens.put(VantarKey.BATCH_INSERT_FAIL, "ورود اطلاعات دسته ای دچار خطا شد");
        tokens.put(VantarKey.INVALID_JSON_DATA, "ساختار ج ی س و ن نادرست است");

        // system errors
        tokens.put(VantarKey.UNEXPECTED_ERROR, "خطای ناخواسته در بک اند رخ داد");
        tokens.put(VantarKey.METHOD_UNAVAILABLE, "server error: method({0}) is unavailable");
        tokens.put(VantarKey.CAN_NOT_CREATE_DTO, "server error: failed to create data object");
        tokens.put(VantarKey.ARTA_FILE_CREATE_ERROR, "server error: Arta SQL database synch failed");

        // admin
        tokens.put(VantarKey.ADMIN_MENU_HOME, "خانه");
        tokens.put(VantarKey.ADMIN_MENU_MONITORING, "دیده‌بانی");
        tokens.put(VantarKey.ADMIN_MENU_DATA, "داده‌ها");
        tokens.put(VantarKey.ADMIN_MENU_ADVANCED, "پیشرفته");
        tokens.put(VantarKey.ADMIN_MENU_SCHEDULE, "برنامه زمانی");
        tokens.put(VantarKey.ADMIN_MENU_QUERY, "جستار‌ها");
        tokens.put(VantarKey.ADMIN_MENU_DOCUMENTS, "مستندها");
        tokens.put(VantarKey.ADMIN_MENU_SIGN_OUT, "خروج");

        tokens.put(VantarKey.ADMIN_USERS, "کاربران");
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS, "خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_SERVICES_LAST_RUN, "زمان آخرین کارکرد سرویس ها");
        tokens.put(VantarKey.ADMIN_SERVICES_STATUS, "وضعیت سرویس ها");
        tokens.put(VantarKey.ADMIN_BACKUP_SQL, "ساخت کپی پشتیبان SQL");
        tokens.put(VantarKey.ADMIN_BACKUP_MONGO, "ساخت کپی پشتیبان Mongo");
        tokens.put(VantarKey.ADMIN_BACKUP_ELASTIC, "ساخت کپی پشتیبان Elastic");
        tokens.put(VantarKey.ADMIN_SHORTCUTS, "لینک های میانبر");

        tokens.put(VantarKey.ADMIN_SYSTEM_ADMIN, "مدیریت سیستم");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES, "سرویس های روشن");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_COUNT, " سرویس (تعداد روشن)");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_ON_THIS_SERVER, "بر روی این سرور");

        tokens.put(VantarKey.ADMIN_BACKUP, "پشتیبانی داده ها");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE, "ساخت کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_BACKUP_RESTORE, "بازیابی کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_BACKUP_FILES, "مدیریت فایل های پشتیبانی");

        tokens.put(VantarKey.ADMIN_DATABASE, "مدیریت پایگاه داده");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS, "وضعیت");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "وضعیت {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_CREATE_INDEX, "ساختن ایندکس ها");
        tokens.put(VantarKey.ADMIN_DATABASE_DELETE_ALL, "پاک سازی تمام داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_DELETE_OPTIONAL, "پاک سازی انتخابی داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH, "همگام سازی");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_DEF, "تعاریف ایندکس ها");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "عمل گرهای ایندکس");

        tokens.put(VantarKey.ADMIN_QUEUE, "مدیریت صف ها");

        tokens.put(VantarKey.ADMIN_SYSTEM_AND_SERVICES, "مدیریت سیستم و سرویس ها");
        tokens.put(VantarKey.ADMIN_STARTUP, "راه اندازی");
        tokens.put(VantarKey.ADMIN_SERVICE_STOP, "خاموش کردن سرویس");
        tokens.put(VantarKey.ADMIN_SERVICE_START, "روشن کردن سرویس");
        tokens.put(VantarKey.ADMIN_FACTORY_RESET, "بازگردانی سیستم به حالا ابتدایی");

        tokens.put(VantarKey.ADMIN_SETTINGS, "مدیریت تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_RELOAD, "بارگذاری دوباره");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "ویرایش Config");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "ویرایش Tune");

        tokens.put(VantarKey.ADMIN_BACKUP_MSG1, "* پشتیبان گیری از داده های زیاد بسیار زمان گیر می باشد.");
        tokens.put(VantarKey.ADMIN_BACKUP_MSG2, "* بهتر است قبل از پشتیبان گیری سرویس ها خاموش شوند.");
        tokens.put(VantarKey.ADMIN_BACKUP_FILE_PATH, "آدرس فایل پشتیبانی پایگاه داده");
        tokens.put(VantarKey.ADMIN_DATE_FROM, "از تاریخ");
        tokens.put(VantarKey.ADMIN_DATE_TO, "تا تاریخ");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE_START, "پشتیبان گیری");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG1, "* داده های کنونی سیستم پاک خواهند شد.");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG2, "* بازیابی داده های سنگین بسیار زمان گیر می باشد.");
        tokens.put(VantarKey.ADMIN_RESTORE_MSG3, "* توصیه می شود قبل از بازیابی سرویس ها خاموش شوند.");
        tokens.put(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "داده های کنونی پاک شوند");
        tokens.put(VantarKey.ADMIN_DOWNLOAD, " دانلود فایل کپی پشتیبان ");
        tokens.put(VantarKey.ADMIN_REMOVE_BACKUP_FILE, " نابود کردن فایل کپی پشتیبان ");
        tokens.put(VantarKey.ADMIN_DELETE_DO, "پاک شود");

        tokens.put(VantarKey.ADMIN_DATA_FIELDS, "فیلدها");
        tokens.put(VantarKey.ADMIN_DATA_LIST, "لیست داده ها");
        tokens.put(VantarKey.ADMIN_NEW_RECORD, "رکورد جدید");
        tokens.put(VantarKey.ADMIN_IMPORT, "ایمپورت");
        tokens.put(VantarKey.ADMIN_EXPORT, "اکسپورت");
        tokens.put(VantarKey.ADMIN_DATABASE_TITLE, "دیتابیس");
        tokens.put(VantarKey.ADMIN_DELETE, "پاک کردن");
        tokens.put(VantarKey.ADMIN_UPDATE, "بروز رسانی");
        tokens.put(VantarKey.ADMIN_STATUS, "وضغیت {0}");
        tokens.put(VantarKey.ADMIN_RECORD_COUNT, "تعداد رکوردهای {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "همگام سازی پایگاه داده های {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_CONFIRM, "همگام سازی آغاز شود");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_RUNNING, "همگام سازی پایگاه داده ها با آخرین نسخه کلاس های سیستم...");
        tokens.put(VantarKey.ADMIN_FINISHED, "پایان!");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "ساخت ایندکس های {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_VIEW, "دیدن ایندکس ها...");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "پاک کردن ایندکس ها در صورت وجود");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START, "آغاز ساختن ایندکس ها");
        tokens.put(VantarKey.ADMIN_INDEX_TITLE, "ایندکس های {0}");
        tokens.put(VantarKey.ADMIN_DATABASE_REMOVE, " پاک سازی داده های {0} ...");
        tokens.put(VantarKey.ADMIN_DELAY, "مکث به ثانیه");
        tokens.put(VantarKey.ADMIN_DELETE_EXCLUDE, "جدول یا مجموعه هایی که پاک نشوند");
        tokens.put(VantarKey.ADMIN_DELETE_INCLUDE, "جدول ها یا مجموعه هایی که از بین بروند");
        tokens.put(VantarKey.ADMIN_IGNORE, "رد داده شد");
        tokens.put(VantarKey.ADMIN_IMPORT_TITLE, "ایمپورت داده های ابتدایی {0} ...");
        tokens.put(VantarKey.ADMIN_IMPORT_EXCLUDE, "جدول یا مجموعه هایی که ایمپورت نشوند");
        tokens.put(VantarKey.ADMIN_ELASTIC_INDEX_DEF, "تعاریف ایندکس های ELASTIC ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS, "عمل گر های قابل اجرا بروی ایندکس ها ELASTIC ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1, "می توانید ایندکس مبدا را clone, shrink و یا refresh کنید.");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME, "نام کلاس ایندکس مبدا");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION, "نام مجموعه مقصد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1, "کلاس مبدا باید انتخاب شود");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2, "ایندکس با موفقیت کپی شد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3, "ایندکس با موفقیت شرینک شد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4, "ایندکس با موفقیت بروز رسانی شد");
        tokens.put(VantarKey.ADMIN_QUEUE_STATUS, "وضعیت صف ها");
        tokens.put(VantarKey.ADMIN_SERVICES, "سرویس ها");
        tokens.put(VantarKey.ADMIN_ONLINE_USERS, "کاربران آنلاین");
        tokens.put(VantarKey.ADMIN_DELETE_TOKEN, "حذف توکن احراز هویت");
        tokens.put(VantarKey.ADMIN_DELETE_TOKEN_DESCRIPTION, "با حذف توکن کاربر از سیستم خارج خواهد شد.");
        tokens.put(VantarKey.ADMIN_AUTH_TOKEN, "توکن");
        tokens.put(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP, "کدهای موقت ثبتنام");
        tokens.put(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP, "کدهای موقت ورود به سیستم");
        tokens.put(VantarKey.ADMIN_RECOVER_TOKEN_TEMP, "کدهای موقت بازیابی و شناسایی");

        tokens.put(VantarKey.ADMIN_REQUIRES_ROOT, "ممتوع! این درخواست نیاز به دسترسی root دارد.");

        tokens.put(VantarKey.ADMIN_ENABLED_SERVICES_THIS, "سرویس های فعال شده بر روی این سرور");
        tokens.put(VantarKey.ADMIN_SERVICE, "سرویس");
        tokens.put(VantarKey.ADMIN_IS_ON, "روشن بودن");
        tokens.put(VantarKey.ADMIN_MENU_QUERY_TITLE, "جستارها (queries)");
        tokens.put(VantarKey.ADMIN_QUERY_NEW, "جستار جدید");
        tokens.put(VantarKey.ADMIN_QUERY_WRITE, "نوشتن جستار (queries)");
        tokens.put(VantarKey.ADMIN_GROUP, "گروه");
        tokens.put(VantarKey.ADMIN_TITLE, "عنوان");
        tokens.put(VantarKey.ADMIN_HELP, "راهنما...");
        tokens.put(VantarKey.ADMIN_CONFIRM, "از انجام کار اطمینان دارم");
        tokens.put(VantarKey.ADMIN_QUERY_DELETE_TITLE, "پاک کردن جستار (queries)");
        tokens.put(VantarKey.ADMIN_QUERY, "جستار (queries)");
        tokens.put(VantarKey.ADMIN_EDIT, "ویرایش");
        tokens.put(VantarKey.ADMIN_DELETE2, "پاک کردن");
        tokens.put(VantarKey.ADMIN_NEW, "جدید");
        tokens.put(VantarKey.ADMIN_IX, "فهرست");
        tokens.put(VantarKey.ADMIN_DELETE_QUEUE, "پاک کردن صف ها");
        tokens.put(VantarKey.ADMIN_TRIES, "تعداد تلاش");
        tokens.put(VantarKey.ADMIN_QUEUE_DELETE_EXCLUDE, "صف هایی که پاک نشوند");
        tokens.put(VantarKey.ADMIN_QUEUE_DELETE_INCLUDE, "انتخاب صف هایی که پاک شوند");
        tokens.put(VantarKey.ADMIN_NO_QUEUE, "صفی وجود ندارد");
        tokens.put(VantarKey.ADMIN_RABBIT_IS_ON, "RabbitMQ روشن است");
        tokens.put(VantarKey.ADMIN_RABBIT_IS_OFF, "RabbitMQ وصل نیست یا سرویس آن خاموش است");
        tokens.put(VantarKey.ADMIN_NO_ERROR, "خطایی وجود ندارد");
        tokens.put(VantarKey.ADMIN_ERRORS_DELETE, "پاک کردن خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_RECORDS, " :رکورد ");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT, "ویرایش تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG1, "تنظیمات قابل ویرایش و نوشتن در فایل نیستند");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG2, "تنضیمات این فایل برای هر سرویس متفاوت است و تنها بر روی این سرویس دهنده بروز خواهند شد.");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG3, "تنضیمات این فایل برای تمام سرویس ها یکسان است و بر روی آنها نیز ویرایش اعمال خواهد شد.");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATED, "تنظیمات بروز شدند");
        tokens.put(VantarKey.ADMIN_SETTINGS_LOADED, "تنظیمات بارگذاری شدند");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT, "پیام ویرایش تنظیمات به سرویس های دیگر فرستاده شد");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_FAILED, "تنظیمات بروز نشدند");
        tokens.put(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES, "شامل تمامی سرورها");
        tokens.put(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES, "شامل سرویس های دیتابیس و صف");
        tokens.put(VantarKey.ADMIN_SERVICE_CLASSES_TO_RESERVE, "کلاس هایی که نگه داشته شوند");
        tokens.put(VantarKey.ADMIN_SERVICE_START_SERVICES_AT_END, "در پایان سرویس ها روشن شوند");
        tokens.put(VantarKey.ADMIN_DO, "انجام کار");
        tokens.put(VantarKey.ADMIN_SERVICES_ARE_STOPPED, "سرویس ها خاموش هستند!");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_STOPPED, "تمام سرویس ها به درستی خاموش شدند");//"all services are stopped successfully"
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_STARTED, "تمام سرویس ها به درستی روشن شدند");//"all services are started successfully"
        tokens.put(VantarKey.ADMIN_SYSYEM_CLASSES, "کلاس هاس سیستم");
        tokens.put(VantarKey.ADMIN_DATA_ENTRY, "ورود اطلاعات");
        tokens.put(VantarKey.ADMIN_DELETE_ALL_CONFIRM, "آیا تمام داده ها حذف شوند؟");
        tokens.put(VantarKey.ADMIN_BATCH_EDIT, "ویرایش دسته ای");
        tokens.put(VantarKey.ADMIN_BATCH_DELETE, "حذف دسته ای");
        tokens.put(VantarKey.ADMIN_JSON_OPTION, "JSON/گزینه");
        tokens.put(VantarKey.ADMIN_SORT, "سورت");
        tokens.put(VantarKey.ADMIN_SEARCH, "جستجو");
        tokens.put(VantarKey.ADMIN_FROM, "از");
        tokens.put(VantarKey.ADMIN_ADMIN_PANEL, "پنل مدیریت سیستم");
        tokens.put(VantarKey.ADMIN_SUBMIT, "انجام شود");
        tokens.put(VantarKey.ADMIN_CACHE, "داده های کش شده");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_OFF, "{0} استفاده نشده است");
        tokens.put(VantarKey.ADMIN_SIGN_IN, "ورود به مدیریت سیستم");
        tokens.put(VantarKey.ADMIN_DATA, "داده ها");

        tokens.put(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, "تکرار");
        tokens.put(VantarKey.ADMIN_SCHEDULE_START_AT, "شروع");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN, "اجرای دستی هم اکنون");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_FAIL, "اجرا نشد");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS, "با موفقیت اجرا شد");

        tokens.put(VantarKey.USERNAME, "نام کاربری");
        tokens.put(VantarKey.PASSWORD, "رمز");
        tokens.put(VantarKey.SIGN_IN, "ورود");

        // business
        tokens.put(VantarKey.BUSINESS_WRITTEN_COUNT, "رکورد جدید نوشته شد" + " ({0})");
        tokens.put(VantarKey.BUSINESS_ERROR_COUNT, "تعداد خطا" + "           ({0})");
        tokens.put(VantarKey.BUSINESS_DUPLICATE_COUNT, "تعداد تکراری" + "        ({0})");
        tokens.put(VantarKey.BUSINESS_SERIAL_MAX, "آخرین شناسه" + "         ({0})");

        tokens.put(VantarKey.SHOW_NOT_DELETED, "نمایش پاک نشده ها");
        tokens.put(VantarKey.SHOW_DELETED, "نمایش پاک شده ها");
        tokens.put(VantarKey.SHOW_ALL, "نمایش همه");

        tokens.put(VantarKey.LOGICAL_DELETED, "حذف نرم (رکورد تنها نشانه زده شود)");
        tokens.put(VantarKey.LOGICAL_DELETED_UNDO, "برگرداندن");
        tokens.put(VantarKey.ADMIN_ACTION_LOG, "لاگ");

        tokens.put(VantarKey.ADMIN_PAGING, "صفحه بندی");

        tokens.put(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, "بعلت وجود وابستگی امکان حذف وجود ندارد");

        tokens.put(VantarKey.ADMIN_AUTH_FAILED, "خظای احراز هویت کاربر: session فاسد شده یا وارد داشبورد نشدید یا دسترسی ندارید");

        tokens.put(VantarKey.ADMIN_ACTION_LOG_USER, "لاگ کاربران");
        tokens.put(VantarKey.ADMIN_ACTION_LOG_REQUEST, "لاگ درخواست ها");
        tokens.put(VantarKey.ADMIN_MEMORY, "حافظه");
        tokens.put(VantarKey.ADMIN_DISK_SPACE, "دیسک");
        tokens.put(VantarKey.ADMIN_PROCESSOR, "پردازنده");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD, "آپلور فایل بکاپ");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD_FILE, "فایل بکاپ");

        tokens.put(VantarKey.SELECT_ALL, "انتخاب همه");
        tokens.put(VantarKey.ADMIN_RESET_SIGNIN_FAILS, "ریست کاربران غیرفعال");

        tokens.put(VantarKey.METHOD_CALL_TIME_LIMIT, "برای اجرای دوباره نیاز به سپری شدن {0}دقیقه است");

    }

    public static String getString(LangKey key) {
        return tokens.get(key);
    }
}
