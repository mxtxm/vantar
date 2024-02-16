package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.*;

@Mongo
@Elastic
@Archive("500000R")
@Index({"userId:1", "className:1", "classNameSimple:1", "objectId:1", "objectX:1", "extraData:1", "action:1", "time:1", "url:1",})
public class UserLog extends DtoBase {

    public Long id;

    public Long userId;
    public String userName;
    public Long threadId;
    public String action;
    public String url;
    @Timestamp
    @CreateTime
    public DateTime time;

    public String className;
    public String classNameSimple;
    public Long objectId;

    public String object;
    public Map<String, Object> objectX;
    public Map<String, Object> extraData;


    public static class Mini extends DtoBase {

        public Long id;

        public Long userId;
        public String userName;
        public Long threadId;
        public String action;
        public String url;
        public DateTime time;

        public String className;
        public String classNameSimple;
        public Long objectId;
    }


    public static class View extends DtoBase {

        public Long id;

        public Long userId;
        public String userName;
        public Long threadId;
        public String action;
        public String url;
        @Timestamp
        public DateTime time;

        public String className;
        public String classNameSimple;
        public Long objectId;

        public Map<String, Object> extraData;
    }
}