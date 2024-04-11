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





        // data fetch
        tokens.put(VantarKey.FETCH_FAIL, "خطا هنگام خواندن داده ها");
        tokens.put(VantarKey.NO_CONTENT, "داده پیدانشد");

        // data write
        tokens.put(VantarKey.UPLOAD_SUCCESS, "\"{0}\" فایل با موفقیت آپلود شذ");
        tokens.put(VantarKey.UPLOAD_FAIL, "\"{0}\" فایل با موفقیت آپلود نشذ");
        tokens.put(VantarKey.INSERT_SUCCESS, "داده ها با موفقیت وارد گردید");
        tokens.put(VantarKey.INSERT_MANY_SUCCESS, "\"{0}\" رکورد با موفقیت وارد گردید");
        tokens.put(VantarKey.INSERT_FAIL, "ورود اطلاعات دچار خطا شد");
        tokens.put(VantarKey.UPDATE_SUCCESS, "data updated successfully");
        tokens.put(VantarKey.UPDATE_MANY_SUCCESS, "(\"{0}\") items updated successfully");
        tokens.put(VantarKey.UPDATE_FAIL, "ویرایش اطلاعات دچار خطا شد");
        tokens.put(VantarKey.DELETE_SUCCESS, "data deleted successfully");
        tokens.put(VantarKey.DELETE_MANY_SUCCESS, "(\"{0}\") items deleted successfully");
        tokens.put(VantarKey.DELETE_FAIL, "پاک کردن اطلاعات دچار خطا شد");
        tokens.put(VantarKey.IMPORT_FAIL, "data import failed");
        tokens.put(VantarKey.INVALID_JSON_DATA, "ساختار ج ی س و ن نادرست است");


        // business
        tokens.put(VantarKey.BUSINESS_WRITTEN_COUNT, "تعداد رکورد جدید");
        tokens.put(VantarKey.BUSINESS_ERROR_COUNT, "تعداد خطا");
        tokens.put(VantarKey.BUSINESS_DUPLICATE_COUNT, "تعداد تکراری ها");
        tokens.put(VantarKey.BUSINESS_SERIAL_MAX, "آخرین مقدار شناسه");

        tokens.put(VantarKey.ADMIN_LIST_OPTION_ACTION_LOG, "لاگ");

        tokens.put(VantarKey.ADMIN_PAGING, "صفحه بندی");


        tokens.put(VantarKey.ADMIN_ACTION_LOG, "لاگ کاربران");
        tokens.put(VantarKey.ADMIN_MEMORY, "حافظه");
        tokens.put(VantarKey.ADMIN_DISK_SPACE, "دیسک");
        tokens.put(VantarKey.ADMIN_PROCESSOR, "پردازنده");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD, "آپلور فایل بکاپ");
        tokens.put(VantarKey.ADMIN_BACKUP_UPLOAD_FILE, "فایل بکاپ");

        tokens.put(VantarKey.SELECT_ALL, "انتخاب همه");
        tokens.put(VantarKey.ADMIN_RESET_SIGNIN_FAILS, "ریست کاربران غیرفعال");

        tokens.put(VantarKey.ADMIN_FAIL, "");
        tokens.put(VantarKey.ADMIN_FAIL_MSG, "");
        tokens.put(VantarKey.ADMIN_SUCCESS, "");
        tokens.put(VantarKey.ADMIN_SUCCESS_MSG, "");

        tokens.put(VantarKey.ADMIN_WEBSERVICE_INDEX_TITLE, "ایندکس وب سرویس ها");
        tokens.put(VantarKey.ADMIN_WEBSERVICE, "وب سرویس ها");

        // admin
        tokens.put(VantarKey.ADMIN_MENU_HOME, "خانه");
        tokens.put(VantarKey.ADMIN_MENU_MONITORING, "دیده‌بانی");
        tokens.put(VantarKey.ADMIN_MENU_DATA, "داده‌ها");
        tokens.put(VantarKey.ADMIN_MENU_ADVANCED, "پیشرفته");
        tokens.put(VantarKey.ADMIN_MENU_SCHEDULE, "برنامه زمانی");
        tokens.put(VantarKey.ADMIN_MENU_PATCH, "وصله");
        tokens.put(VantarKey.ADMIN_MENU_QUERY, "جستار‌ها");
        tokens.put(VantarKey.ADMIN_MENU_DOCUMENTS, "مستندها");
        tokens.put(VantarKey.ADMIN_MENU_BUGGER, "گزارش باگ");

        tokens.put(VantarKey.ADMIN_USERS, "کاربران");
        tokens.put(VantarKey.ADMIN_SYSTEM_ERRORS, "خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_SERVICES_BEAT, "آخرین کارکرد سرویس");
        tokens.put(VantarKey.ADMIN_SERVICES_STATUS, "وضعیت سرویس ها");
        tokens.put(VantarKey.ADMIN_SHORTCUTS, "لینک های میانبر");

        tokens.put(VantarKey.ADMIN_SYSTEM_ADMIN, "مدیریت سیستم");

        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_ME, "سرویس های روی این سرور");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_OTHER, "سرویس های روی سرورهای دیگر");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_LOGS, "لاگهای سرویس ها");
        tokens.put(VantarKey.ADMIN_RUNNING_SERVICES_DATA_SOURCE, "منابع داده");



        tokens.put(VantarKey.ADMIN_BACKUP, "پشتیبانی داده ها");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE, "ساخت کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_RESTORE, "بازیابی کپی پشتیبان");
        tokens.put(VantarKey.ADMIN_BACKUP_FILES, "مدیریت فایل های پشتیبانی");

        tokens.put(VantarKey.ADMIN_DATABASE, "مدیریت پایگاه داده");
        tokens.put(VantarKey.ADMIN_DATABASE_STATUS, "وضعیت");
        tokens.put(VantarKey.ADMIN_DATABASE_CREATE_SEQUENCE, "ساختن سریال");
        tokens.put(VantarKey.ADMIN_DATA_PURGE, "نابودی");
        tokens.put(VantarKey.ADMIN_DELETE_OPTIONAL, "پاک سازی انتخابی داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH, "همگام سازی");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_DEF, "تعاریف ایندکس ها");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "عمل گرهای ایندکس");

        tokens.put(VantarKey.ADMIN_QUEUE, "مدیریت صف ها");

        tokens.put(VantarKey.ADMIN_SYSTEM_AND_SERVICES, "مدیریت سیستم و سرویس ها");
        tokens.put(VantarKey.ADMIN_STARTUP, "راه اندازی");
        tokens.put(VantarKey.ADMIN_SERVICE_START, "روشن کردن سرویس");
        tokens.put(VantarKey.ADMIN_FACTORY_RESET, "بازگردانی سیستم به حالا ابتدایی");

        tokens.put(VantarKey.ADMIN_SETTINGS, "مدیریت تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_RELOAD, "بارگذاری دوباره");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "ویرایش Config");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "ویرایش Tune");

        tokens.put(VantarKey.ADMIN_BACKUP_FILE_PATH, "آدرس فایل پشتیبانی پایگاه داده");
        tokens.put(VantarKey.ADMIN_DATE_FROM, "از تاریخ");
        tokens.put(VantarKey.ADMIN_DATE_TO, "تا تاریخ");
        tokens.put(VantarKey.ADMIN_BACKUP_CREATE_START, "پشتیبان گیری");
        tokens.put(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "داده های کنونی پاک شوند");
        tokens.put(VantarKey.ADMIN_DELETE_DO, "پاک شود");

        tokens.put(VantarKey.ADMIN_DATA_FIELDS, "فیلدها");
        tokens.put(VantarKey.ADMIN_DATA_LIST, "لیست داده ها");
        tokens.put(VantarKey.ADMIN_INSERT, "جدید");
        tokens.put(VantarKey.ADMIN_IMPORT, "ایمپورت");
        tokens.put(VantarKey.ADMIN_EXPORT, "اکسپورت");
        tokens.put(VantarKey.ADMIN_DELETE, "پاک کردن");
        tokens.put(VantarKey.ADMIN_UNDELETE, "بازیابی");
        tokens.put(VantarKey.ADMIN_UPDATE, "بروز رسانی");
        tokens.put(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "همگام سازی پایگاه داده های");
        tokens.put(VantarKey.ADMIN_FINISHED, "پایان!");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "ساخت ایندکس های");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "پاک کردن ایندکس ها در صورت وجود");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START, "آغاز ساختن ایندکس ها");
        tokens.put(VantarKey.ADMIN_DATABASE_INDEX, "ایندکس های داده ها");
        tokens.put(VantarKey.ADMIN_DATABASE_SEQUENCE, "سریال های داده ها");
        tokens.put(VantarKey.ADMIN_DELAY, "مکث به ثانیه");
        tokens.put(VantarKey.ADMIN_IMPORT_TITLE, "ایمپورت داده ها");
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
        tokens.put(VantarKey.ADMIN_AUTH_TOKEN, "توکن");
        tokens.put(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP, "کدهای موقت ثبتنام");
        tokens.put(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP, "کدهای موقت ورود به سیستم");
        tokens.put(VantarKey.ADMIN_RECOVER_TOKEN_TEMP, "کدهای موقت بازیابی و شناسایی");

        tokens.put(VantarKey.ADMIN_SERVICE, "سرویس");
        tokens.put(VantarKey.ADMIN_MENU_QUERY_TITLE, "جستارها (queries)");
        tokens.put(VantarKey.ADMIN_QUERY_NEW, "جستار جدید");
        tokens.put(VantarKey.ADMIN_TITLE, "عنوان");
        tokens.put(VantarKey.ADMIN_CONFIRM, "از انجام کار اطمینان دارم");
        tokens.put(VantarKey.ADMIN_QUERY, "جستار (queries)");
        tokens.put(VantarKey.ADMIN_EDIT, "ویرایش");
        tokens.put(VantarKey.ADMIN_NEW, "جدید");
        tokens.put(VantarKey.ADMIN_DELETE_QUEUE, "پاک کردن صف ها");
        tokens.put(VantarKey.ADMIN_ATTEMPTS, "تعداد تلاش");
        tokens.put(VantarKey.ADMIN_NO_QUEUE, "صفی وجود ندارد");
        tokens.put(VantarKey.ADMIN_ERRORS_DELETE, "پاک کردن خطاهای سیستم");
        tokens.put(VantarKey.ADMIN_SETTINGS_EDIT, "ویرایش تنظیمات");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG1, "تنظیمات قابل ویرایش و نوشتن در فایل نیستند");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG2, "تنضیمات این فایل برای هر سرویس متفاوت است و تنها بر روی این سرویس دهنده بروز خواهند شد.");
        tokens.put(VantarKey.ADMIN_SETTINGS_MSG3, "تنضیمات این فایل برای تمام سرویس ها یکسان است و بر روی آنها نیز ویرایش اعمال خواهد شد.");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATED, "تنظیمات بروز شدند");
        tokens.put(VantarKey.ADMIN_SETTINGS_LOADED, "تنظیمات بارگذاری شدند");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT, "پیام ویرایش تنظیمات به سرویس های دیگر فرستاده شد");
        tokens.put(VantarKey.ADMIN_SETTINGS_UPDATE_FAILED, "تنظیمات بروز نشدند");
        tokens.put(VantarKey.ADMIN_SERVICE_ALL_SERVERS, "تمامی سرورها");
        tokens.put(VantarKey.ADMIN_SERVICE_STOPPED, "تمام سرویس ها به درستی خاموش شدند");//"all services are stopped successfully"
        tokens.put(VantarKey.ADMIN_SERVICE_STARTED, "تمام سرویس ها به درستی روشن شدند");//"all services are started successfully"
        tokens.put(VantarKey.ADMIN_SYSYEM_OBJECTS, "ابجکت های سیستم");
        tokens.put(VantarKey.ADMIN_DELETE_ALL_CONFIRM, "آیا تمام داده ها حذف شوند؟");
        tokens.put(VantarKey.ADMIN_SORT, "سورت");
        tokens.put(VantarKey.ADMIN_SEARCH, "جستجو");
        tokens.put(VantarKey.ADMIN_ADMIN_PANEL, "پنل مدیریت سیستم");
        tokens.put(VantarKey.ADMIN_SUBMIT, "انجام شود");
        tokens.put(VantarKey.ADMIN_CACHE, "داده های کش شده");

        tokens.put(VantarKey.ADMIN_SIGN_IN, "ورود به مدیریت سیستم ونتار");

        tokens.put(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, "تکرار");
        tokens.put(VantarKey.ADMIN_SCHEDULE_START_AT, "شروع");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN, "اجرای دستی هم اکنون");
        tokens.put(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS, "با موفقیت اجرا شد");
        tokens.put(VantarKey.ADMIN_REFRESH, "بارگذاری");

        tokens.put(VantarKey.ADMIN_ACTION, "عملیات");
        tokens.put(VantarKey.ADMIN_RUN_TIME, "زمان اجرا");


        tokens.put(VantarKey.ADMIN_SERVICE_IS_OFF, "خاموش است {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ON, "روشن است {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_DISABLED, "فعال نشده {0}");
        tokens.put(VantarKey.ADMIN_SERVICE_IS_ENABLED, "فعال شده {0}");

        tokens.put(VantarKey.ADMIN_SERVICE_ACTION, "روشن خاموش کردن سرویس");
        tokens.put(VantarKey.ADMIN_FAILED, "نشد");
        tokens.put(VantarKey.ADMIN_INCLUDE, "شامل باشند");
        tokens.put(VantarKey.ADMIN_EXCLUDE, "شامل نباشند");

        tokens.put(VantarKey.ADMIN_IGNORE_DEPENDENCIES, "از وابستگی ها چشم پوشی شود");

    }


    public static String getString(LangKey key) {
        return tokens.get(key);
    }
}
