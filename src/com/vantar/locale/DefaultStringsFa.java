package com.vantar.locale;

import java.util.*;


public class DefaultStringsFa {

    private static final Map<LangKey, String> tokens;

    static {
        tokens = new HashMap<>(300, 1);

        // auth
        tokens.put(VantarKey.USERNAME, "نام کاربری");
        tokens.put(VantarKey.PASSWORD, "رمز");
        tokens.put(VantarKey.SIGN_IN, "ورود");
        tokens.put(VantarKey.SIGN_OUT, "خروج");
        tokens.put(VantarKey.USER_OR_PASSWORD_EMPTY, "نام کاربری یا رمز پر نشده است");
        tokens.put(VantarKey.USER_NOT_EXISTS, "کاربر در سیستم وجود ندارد");
        tokens.put(VantarKey.USER_DISABLED, "کاربر غیر فعال است");
        tokens.put(VantarKey.USER_DISABLED_MAX_FAILED, "بعلت تلاش های ناموفق بیش از حد کاربر غیر فعال شد");
        tokens.put(VantarKey.USER_REPO_NOT_SET, "رپوی کاربر برای سرویس احراز هویت ست نشده است");
        tokens.put(VantarKey.USER_ALREADY_SIGNED_IN, "کاربر هم اکنون در یک دیوایس دیگر وارد شده است");
        tokens.put(VantarKey.USER_WRONG_PASSWORD, "رمز نادرست است");
        tokens.put(VantarKey.NO_ACCESS, "دسترسی ندارید");
        tokens.put(VantarKey.MISSING_AUTH_TOKEN, "توکن احراز هویت داده نشده است");
        tokens.put(VantarKey.INVALID_AUTH_TOKEN, "توکن احراز هویت نادرست است");
        tokens.put(VantarKey.EXPIRED_AUTH_TOKEN, "توکن احراز هویت باطل شده است");

        // datetime
        tokens.put(VantarKey.INVALID_TIME, "زمان نادرست است");
        tokens.put(VantarKey.INVALID_DATE, "تاریخ نادرست است");
        tokens.put(VantarKey.INVALID_DATETIME, "تاریخ و زمان نادرست است");
        tokens.put(VantarKey.INVALID_TIMEZONE, "منطقه زمانی نادرست است");

        // validation
        tokens.put(VantarKey.REQUIRED, "\"{0}\": باید پر شود");
        tokens.put(VantarKey.REQUIRED_OR, "\"{0}\": پر کردن یکی از فیلدها اجباری است");
        tokens.put(VantarKey.REQUIRED_XOR, "\"{0}\": پر کردن تنها یکی از فیلدها اجباری است");
        tokens.put(VantarKey.UNIQUE, "\"{0}\": باید در سیستم یکتا باشد، این مقدار هم اکنون وجود دارد");
        tokens.put(VantarKey.INVALID_FIELD, "\"{0}\": فیلد نادرست است");
        tokens.put(VantarKey.INVALID_VALUE, "\"{0}\": مقدار نادرست است");
        tokens.put(VantarKey.INVALID_VALUE_TYPE, "\"{0}\": نوع داده نادرست است");
        tokens.put(VantarKey.INVALID_ID, "\"{0}\": شناسه نادرست است");
        tokens.put(VantarKey.INVALID_GEO_LOCATION, "\"{0}\": مکان جغرافیایی نادرست است");
        tokens.put(VantarKey.INVALID_FORMAT, "\"{0}\": فرمت مقدار نادرست است");
        tokens.put(VantarKey.INVALID_LENGTH, "\"{0}\": زیادی بلند است");
        tokens.put(VantarKey.MAX_EXCEED, "\"{0}\": زیادی بزرگ است");
        tokens.put(VantarKey.MIN_EXCEED, "\"{0}\": زیادی کوچک است");
        tokens.put(VantarKey.MISSING_REFERENCE, "\"{0}\": رفرنس (\"{1}\") وجود ندارد");
        tokens.put(VantarKey.ILLEGAL_FIELD, "\"{0}\": به فیلد دسترسی ندارید");
        tokens.put(VantarKey.IO_ERROR, "خظای ورودی یا خروجی فایل یا داده ها");
        tokens.put(VantarKey.FILE_TYPE, "\"{0}\": نوع فایل نادرست است- فایل های مجاز برای آپلود (\"{1}\")");
        tokens.put(VantarKey.FILE_SIZE, "\"{0}\": سایز فایل مشکوک است- حجم قابل قبول (\"{1}\")");
        tokens.put(VantarKey.CUSTOM_EVENT_ERROR, "\"{0}\": خطای داده های شخصی");
        tokens.put(VantarKey.SEARCH_COL_MISSING, "\"{0}\": فیلد جستجو جا افتاده اشت");
        tokens.put(VantarKey.SEARCH_VALUE_INVALID, "\"{0}\": مقدار جستحو نادرست است");
        tokens.put(VantarKey.SEARCH_VALUE_MISSING, "\"{0}\": مقدار جستجو چا افتاده است");
        tokens.put(VantarKey.SEARCH_CONDITION_TYPE_INVALID, "\"{0}\": نوع شرط جستجو جا افتاده یا نادرست است");

        // other validation and errors
        tokens.put(VantarKey.METHOD_UNAVAILABLE, "خطای سرور: متد(\"{0}\") وجود ندارد");
        tokens.put(VantarKey.METHOD_CALL_TIME_LIMIT, "برای اجرای دوباره نیاز به سپری شدن \"{0}\"دقیقه است");
        tokens.put(VantarKey.HTTP_METHOD_INVALID, "متد درخواست نادرست است");
        tokens.put(VantarKey.HTTP_POST_MULTIPART, "متد رکویست نادرست است باید POST multipart/form-data باشد");
        tokens.put(VantarKey.UNEXPECTED_ERROR, "خطای ناخواسته بر روی سرور رخ داد");
        tokens.put(VantarKey.CAN_NOT_CREATE_DTO, "خطای سروی: ابجکت dto ساخته نشد");
        tokens.put(VantarKey.ARTA_FILE_CREATE_ERROR, "خطای سروی: Arta SQL database synch failed.");

        // data
        tokens.put(VantarKey.FAIL_FETCH, "خطا در خواندن داده ها");
        tokens.put(VantarKey.NO_CONTENT, "هیچ داده ای پیدانشد");
        tokens.put(VantarKey.SUCCESS_INSERT, "داده ها با موفقیت نوشته شدند");
        tokens.put(VantarKey.FAIL_INSERT, "خظا در نوشتن داده ها");
        tokens.put(VantarKey.SUCCESS_UPDATE, "داده ها با موفقیت بروز شدند");
        tokens.put(VantarKey.FAIL_UPDATE, "خطا در ویرایش داده ها");
        tokens.put(VantarKey.SUCCESS_DELETE, "داده ها با موفقیت پاک شدند");
        tokens.put(VantarKey.FAIL_DELETE, "حطا در پاک شدن داده ها");
        tokens.put(VantarKey.INVALID_JSON_DATA, "ساختار داده های جیسون درست نیست");
        tokens.put(VantarKey.FAIL_IMPORT, "خظا در ایمپورت داده ها");
        tokens.put(VantarKey.SUCCESS_UPLOAD, "\"{0}\" فایل با موفقیت آپلود شذ");
        tokens.put(VantarKey.FAIL_UPLOAD, "\"{0}\" فایل با خطا آپلود نشذ");
        tokens.put(VantarKey.DELETE_DEPENDANT_DATA_ERROR, "بعلت وجود وابستگی حذف امکان پذیر نیست. ({0}, {1})");
        tokens.put(VantarKey.SUCCESS_COUNT, "تعداد رکورد نوشته شده");
        tokens.put(VantarKey.FAIL_COUNT, "تعداد خطا");
        tokens.put(VantarKey.DUPLICATE_COUNT, "تعداد تکراری ها");
        tokens.put(VantarKey.AUTO_INCREMENT_MAX, "آخرین مقدار شناسه");

        // admin - menu
        tokens.put(VantarKey.ADMIN_MENU_HOME, "خانه");
        tokens.put(VantarKey.ADMIN_MENU_MONITORING, "دیده‌بانی");
        tokens.put(VantarKey.ADMIN_MENU_DATA, "داده‌ها");
        tokens.put(VantarKey.ADMIN_MENU_ADVANCED, "پیشرفته");
        tokens.put(VantarKey.ADMIN_MENU_SCHEDULE, "برنامه زمانی");
        tokens.put(VantarKey.ADMIN_MENU_PATCH, "پچ");
        tokens.put(VantarKey.ADMIN_MENU_QUERY, "جستار‌ها");
        tokens.put(VantarKey.ADMIN_MENU_TEST, "تست");
        tokens.put(VantarKey.ADMIN_MENU_DOCUMENTS, "مستندها");
        tokens.put(VantarKey.ADMIN_MENU_BUGGER, "گزارش باگ");

        // admin
        tokens.put(VantarKey.ADMIN_SYSTEM_ADMINISTRATION, "داشبورد مدیریت سیستم");
        tokens.put(VantarKey.ADMIN_SHORTCUTS, "لینک های میانبر");
        tokens.put(VantarKey.ADMIN_MEMORY, "حافظه");
        tokens.put(VantarKey.ADMIN_ACTION, "کارها");
        tokens.put(VantarKey.ADMIN_TITLE, "عنوان");
        tokens.put(VantarKey.ADMIN_CONFIRM, "از انجام کار اطمینان دارم");
        tokens.put(VantarKey.ADMIN_SUBMIT, "اجرا کن");
        tokens.put(VantarKey.ADMIN_DISK_SPACE, "دیسک");
        tokens.put(VantarKey.ADMIN_PROCESSOR, "پردازنده");
        tokens.put(VantarKey.ADMIN_DELAY, "مکث به ثانیه");
        tokens.put(VantarKey.ADMIN_FAILED, "کار انجام نشد");
        tokens.put(VantarKey.ADMIN_ATTEMPT_COUNT, "تعداد تلاش");
        tokens.put(VantarKey.ADMIN_INCLUDE, "شامل باشند");
        tokens.put(VantarKey.ADMIN_EXCLUDE, "شامل نباشند");

        tokens.put(VantarKey.ADMIN_USER, "کاربران");
        tokens.put(VantarKey.ADMIN_USER_ADMIN_SIGN_IN_TITLE, "وارد شدن به داشبورد مدیریت سیستم ونتار");
        tokens.put(VantarKey.ADMIN_USER_ONLINE, "کاربران آنلاین");
        tokens.put(VantarKey.ADMIN_USER_ACTIVITY, "کارکرد کاربر");
        tokens.put(VantarKey.ADMIN_USER_RESET_SIGNIN_FAILS, "آزادسازی کاربران ققل شده");
        tokens.put(VantarKey.ADMIN_USER_AUTH_TOKEN, "توکن");
        tokens.put(VantarKey.ADMIN_USER_DELETE_TOKEN, "حذف توکن احراز هویت");
        tokens.put(VantarKey.ADMIN_USER_SIGNUP_TOKEN_TEMP, "کدهای موقت ثبتنام");
        tokens.put(VantarKey.ADMIN_USER_SIGNIN_TOKEN_TEMP, "کدهای موقت ورود به سیستم");
        tokens.put(VantarKey.ADMIN_USER_RECOVER_TOKEN_TEMP, "کدهای موقت بازیابی و شناسایی");

        // admin - monitoring
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS, "خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS_DELETE, "پاک کردن خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_SYSTEM_HEALTH_WEBSERVICE, "وب سرویس های گزارش سلامت سیستم");
        tokens.put(VantarKey.ADMIN_SYSTEM_OBJECTS, "ابجکت های سیستم");

        tokens.put(VantarKey.ADMIN_SERVICE, "سرویس");
        tokens.put(VantarKey.ADMIN_SERVICE_ACTION, "روشن خاموش کردن سرویس");
        tokens.put(VantarKey.ADMIN_SERVICE_STOPPED, "تمام سرویس ها به درستی خاموش شدند");
        tokens.put(VantarKey.ADMIN_SERVICE_STARTED, "تمام سرویس ها به درستی روشن شدند");
        tokens.put(VantarKey.ADMIN_SERVICE_PAUSED, "تمام سرویس ها غیر فعال شدند");
        tokens.put(VantarKey.ADMIN_SERVICE_RESUMED, "تمام سرویس ها فعال شدند");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_OFF, "خاموش است {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ON, "روشن است {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_DISABLED, "فعال نشده {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ENABLED, "فعال شده {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_SERVERS, "تمامی سرورها");

        tokens.put(VantarKey.ADMIN_SERVICES, "سرویس ها");
        tokens.put(VantarKey.ADMIN_SERVICES_BEAT, "آخرین کارکرد سرویس");
        tokens.put(VantarKey.ADMIN_SERVICES_STATUS, "وضعیت سرویس ها");
        tokens.put(VantarKey.ADMIN_SERVICES_RUNNING_ME, "سرویس های روی این سرور");
        tokens.put(VantarKey.ADMIN_SERVICES_RUNNING_OTHER, "سرویس های روی سرورهای دیگر");
        tokens.put(VantarKey.ADMIN_SERVICES_RUNNING_LOGS, "لاگهای سرویس ها");
        tokens.put(VantarKey.ADMIN_SERVICES_RUNNING_DATA_SOURCE, "دیتابیس ها و صف ها");

        // admin - queue
        tokens.put(VantarKey.ADMIN_QUEUE, "مدیریت صف ها");
        tokens.put(VantarKey.ADMIN_QUEUE_STATUS, "وضعیت صف ها");
        tokens.put(VantarKey.ADMIN_QUEUE_DELETE, "پاک کردن صف ها");
        tokens.put(VantarKey.ADMIN_QUEUE_NO_QUEUE, "صفی وجود ندارد");
        tokens.put(VantarKey.ADMIN_QUEUE_SELECTIVE_DELETE, "از بین بردن صف های انتخاب شده");

        // admin - database
        tokens.put(VantarKey.ADMIN_DATABASE, "مدیریت پایگاه داده");
        tokens.put(VantarKey.ADMIN_DATABASE_AUTOINCREMENT, "شمارنده اتوماتیک شناسه");
        tokens.put(VantarKey.ADMIN_DATABASE_AUTOINCREMENT_CREATE, "ساختن شمارنده اتوماتیک شناسه");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX, "ایندکس های داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "ساخت ایندکس ها");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "پاک کردن ایندکس ها در صورت وجود");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "تنظیم های ایندکس");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "همگام سازی پایگاه داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH, "همگام سازی");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS, "وضعیت پایگاه داده");

        // admin - advanced
        tokens.put(VantarKey.ADMIN_SYSTEM_AND_SERVICES, "مدیریت سیستم و سرویس");
        tokens.put(VantarKey.ADMIN_STARTUP, "راه اندازی");
        tokens.put(VantarKey.ADMIN_FACTORY_RESET, "بازگردانی سیستم به حالا ابتدایی");

        tokens.put(VantarKey.ADMIN_SETTINGS, "مدیریت تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT, "ویرایش تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_RELOAD, "بارگذاری دوباره");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "ویرایش Config");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "ویرایش Tune");
        tokens.put(VantarKey.ADMIN_SETTINGS_LOADED, "تنظیمات بارگذاری شدند");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT, "پیام ویرایش تنظیمات به سرویس های دیگر فرستاده شد");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG1, "تنظیمات قابل ویرایش و نوشتن در فایل نیستند");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG2, "تنضیمات این فایل برای هر سرویس متفاوت است و تنها بر روی این سرویس دهنده بروز خواهند شد.");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG3, "تنضیمات این فایل برای تمام سرویس ها یکسان است و بر روی آنها نیز ویرایش اعمال خواهد شد.");

        // admin - schedule
        tokens.put(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, "تکرار");
        tokens.put(VantarKey.ADMIN_SCHEDULE_START_AT, "شروع");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN, "اجرای دستی هم اکنون");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS, "با موفقیت اجرا شد");

        // admin - patch
        tokens.put(VantarKey.ADMIN_PATCH_FAIL_MSG, "پیام خطا");
        tokens.put(VantarKey.ADMIN_PATCH_SUCCESS_MSG, "پیام موفقیت");
        tokens.put(VantarKey.ADMIN_PATCH_RUN_TIME, "زمان اجرا");

        // admin - query
        tokens.put(VantarKey.ADMIN_QUERY_NEW, "جستار جدید");
        tokens.put(VantarKey.ADMIN_QUERY, "جستار (queries)");

        // admin - test
        tokens.put(VantarKey.ADMIN_TEST_RUN, "اجرای تست ها");

        // admin - documentation
        tokens.put(VantarKey.ADMIN_DOCUMENTATION_WEBSERVICE_INDEX_TITLE, "ایندکس وب سرویس ها");

        // admin - data
        tokens.put(VantarKey.ADMIN_VIEW, "مرور");
        tokens.put(VantarKey.ADMIN_REVERT, "بازیابی");
        tokens.put(VantarKey.ADMIN_REFRESH, "بارگذاری");
        tokens.put(VantarKey.ADMIN_CACHE, "داده های کش شده");
        tokens.put(VantarKey.ADMIN_IMPORT, "ایمپورت");
        tokens.put(VantarKey.ADMIN_EXPORT, "اکسپورت");
        tokens.put(VantarKey.ADMIN_INSERT, "جدید");
        tokens.put(VantarKey.ADMIN_UPDATE, "بروز رسانی");
        tokens.put(VantarKey.ADMIN_UPDATE_PROPERTY, "بروز رسانی property");
        tokens.put(VantarKey.ADMIN_DATA_PURGE, "نابودی");
        tokens.put(VantarKey.ADMIN_UNDELETE, "بازیابی");
        tokens.put(VantarKey.ADMIN_DELETE, "پاک کردن");
        tokens.put(VantarKey.ADMIN_DELETE_CASCADE, "حذف آبشاری");
        tokens.put(VantarKey.ADMIN_DELETE_IGNORE_DEPENDENCIES, "از وابستگی ها چشم پوشی شود");
        tokens.put(VantarKey.ADMIN_DELETE_ALL_CONFIRM, "آیا تمام داده ها حذف شوند؟");
        tokens.put(VantarKey.ADMIN_DELETE_CONFIRM, "پاک شود");
        tokens.put(VantarKey.ADMIN_DEPENDENCIES, "وابستگی ها");

        tokens.put(VantarKey.ADMIN_UNDELETED, "بازابی حذف {0} id {1}");
        tokens.put(VantarKey.ADMIN_REVERTED, "بازگشت {0} id {1}");
        tokens.put(VantarKey.ADMIN_FINISHED, "پایان!");

        tokens.put(VantarKey.ADMIN_DATE_FROM, "از تاریخ");
        tokens.put(VantarKey.ADMIN_DATE_TO, "تا تاریخ");
        tokens.put(VantarKey.ADMIN_DATA_FIELDS, "فیلدها");
        tokens.put(VantarKey.ADMIN_DATA_LIST, "لیست داده ها");
        tokens.put(VantarKey.ADMIN_SELECT_ALL, "انتخاب همه");

        tokens.put(VantarKey.ADMIN_SORT, "سورت");
        tokens.put(VantarKey.ADMIN_SEARCH, "جستجو");
        tokens.put(VantarKey.ADMIN_PAGING, "صفحه بندی");

        tokens.put(VantarKey.ADMIN_WEB, "وب");
        tokens.put(VantarKey.ADMIN_LOG_DIFFERENCES, "مرور فرق ها");
        tokens.put(VantarKey.ADMIN_LOG_WEB, "لاگ وب");
        tokens.put(VantarKey.ADMIN_LOG_ACTION, "لاگ کاربران");
        tokens.put(VantarKey.ADMIN_LIST_OPTION_ACTION_LOG, "لاگ");
        tokens.put(VantarKey.ADMIN_LIST_OPTION_USER_ACTIVITY, "کارکرد");

        tokens.put(VantarKey.ADMIN_BACKUP, "پشتیبانی داده ها");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE, "ساخت کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_BACKUP_FILES, "مدیریت فایل های پشتیبانی");
        tokens.put(VantarKey.ADMIN_BACKUP_FILE_PATH, "آدرس فایل پشتیبانی پایگاه داده");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD, "آپلور فایل بکاپ");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD_FILE, "فایل بکاپ");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE_START, "پشتیبان گیری");
        tokens.put(VantarKey.ADMIN_RESTORE, "بازیابی کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "داده های کنونی پاک شوند");

        tokens.put(VantarKey.ADMIN_ELASTIC_INDEX_DEF, "تعاریف ایندکس های ELASTIC ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS, "عمل گر های قابل اجرا بروی ایندکس ها ELASTIC ...");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1, "می توانید ایندکس مبدا را clone, shrink و یا refresh کنید.");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME, "نام کلاس ایندکس مبدا");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION, "نام مجموعه مقصد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1, "کلاس مبدا باید انتخاب شود");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2, "ایندکس با موفقیت کپی شد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3, "ایندکس با موفقیت شرینک شد");
        tokens.put(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4, "ایندکس با موفقیت بروز رسانی شد");
    }


    public static String getString(LangKey key) {
        return tokens.get(key);
    }
}
