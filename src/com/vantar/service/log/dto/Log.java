package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;

@Archive("500000R")
@Mongo
@Index({"tag:1", "createT:1"})
public class Log extends DtoBase {

    public Long id;
    public String tag;
    public String level;
    public String message;
    @CreateTime
    @Timestamp
    public DateTime createT;


    public Log() {

    }

    public Log(String tag, String level, String message) {
        this.tag = tag;
        this.level = level;
        this.message = message == null ? "run" : message;
    }
}
