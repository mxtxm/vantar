package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.*;

@Mongo
@Elastic
@Archive("500000R")
@Index({"userId:1", "requestType:1", "action:1", "time:1", "url:1", "classNameSimple:1", "objectId:1",})
public class UserWebLog extends DtoBase {

    public Long id;

    public Long userId;
    public String userName;
    public Long threadId;
    public String action;
    public Integer status;
    public String requestType;
    public String url;
    public String ip;
    @Timestamp
    @CreateTime
    public DateTime time;

    @ExcludeList
    public List<String> uploadedFiles;
    @StoreString
    @ExcludeList
    public Map<String, String> headers;

    public Long objectId;
    public String className;
    public String classNameSimple;
    public String params;
    @ExcludeList
    public Map<String, Object> paramsX;

    @ExcludeList
    public Map<String, Object> extraData;



    public static class Mini extends DtoBase {

        public Long id;

        public Long userId;
        public String userName;
        public Long threadId;
        public String action;
        public Integer status;
        public String requestType;
        public String url;
        public String ip;
        public DateTime time;
        public Long objectId;
        public String className;
        public String classNameSimple;
    }
}