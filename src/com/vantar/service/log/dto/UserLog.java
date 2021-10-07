package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.Map;

@Elastic
@Mongo
@Index({"userId:1", "transactionId:1", "className:1", "objectId:1", "action:1", "time:1",})
public class UserLog extends DtoBase {

    public Long id;

    public Long userId;
    public String action;

    public String className;
    public Long objectId;
    public String object;

    @StoreString
    public Map<String, String> headers;
    public String url;
    public String ip;

    public String description;

    @Timestamp
    @CreateTime
    public DateTime time;

}