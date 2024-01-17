package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.*;

@Archive("500000R")
@Elastic
@Mongo
@Index({"userId:1", "className:1", "classNameSimple:1", "objectId:1", "action:1", "time:1", "url:1",})
public class UserLog extends DtoBase {

    public Long id;

    public Long userId;
    public Long threadId;
    public String action;
    public Integer status;
    public String requestType;
    public String url;
    public String ip;
    @Timestamp
    @CreateTime
    public DateTime time;

    public String className;
    public String classNameSimple;

    public List<String> uploadedFiles;
    @StoreString
    public Map<String, String> headers;

    public Long objectId;
    public String object;

    public Map<String, Object> extraData;



    public static class Mini extends DtoBase {

        public Long id;

        public Long userId;
        public Long threadId;
        public String action;
        public Integer status;
        public String url;
        public String ip;
        public DateTime time;

        public String className;
        public String classNameSimple;
        public Long objectId;
    }


    public static class View extends DtoBase {

        public Long id;

        public Long userId;
        public Long threadId;
        public String action;
        public Integer status;
        public String requestType;
        public String url;
        public String ip;
        @Timestamp
        @CreateTime
        public DateTime time;

        public String className;

        public List<String> uploadedFiles;
        @StoreString
        public Map<String, String> headers;

        public Long objectId;

        public Map<String, Object> extraData;
    }
}