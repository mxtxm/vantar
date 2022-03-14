package com.vantar.admin.Dto;

import com.vantar.database.dto.*;

@Mongo
public class QueryDictionary extends DtoBase {

    public Long id;
    @Required
    public String group;
    @Required
    public String title;
    @Required
    public String q;
}