package com.vantar.service.log.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import java.util.*;

@Mongo
@Archive("500000R")
@Index({"tag:1", "createT:1"})
public class Log extends DtoBase {

    public Long id;
    public String tag;
    public String level;
    public String message;
    public List<String> objects;
    @CreateTime
    @Timestamp
    public DateTime createT;


    public Log() {

    }

    public Log(String tag, String level, String message, Object... objects) {
        this.tag = tag;
        this.level = level;
        this.message = message;
        if (objects == null) {
            return;
        }
        this.objects = new ArrayList<>(objects.length);
        for (Object o : objects) {
            this.objects.add(ObjectUtil.toString(o));
        }
    }
}
