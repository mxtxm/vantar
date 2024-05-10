package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.*;

@Mongo
@Elastic
@Archive("500000R")
@Index({
    "_id:-1",
    "timeDay:1,threadId:1,userId:1,url:1,objectId:1,action:1,_id:-1",
    "classNameSimple:1,action:1,_id:-1",
    "userId:1,action:1,url:1,time:-1,_id:-1",
    "objectX:1"
})
public class UserLog extends DtoBase {

    public Long id;

    public Long userId;
    public String userName;
    public Long timeDay;
    public Long threadId;
    public String action;
    public String url;
    @Timestamp
    public DateTime time;

    public String className;
    public String classNameSimple;
    public Long objectId;
    @NoList
    public String object;
    @NoList
    public Map<String, Object> objectX;
    @NoList
    public Map<String, Object> extraData;



    public static class Mini extends DtoBase {

        public Long id;

        public Long userId;
        public String userName;
        public Long threadId;
        public String action;
        public String url;
        public Long timeDay;
        public DateTime time;
        public String className;
        public String classNameSimple;
        public Long objectId;
    }
}