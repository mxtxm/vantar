package com.vantar.admin.Dto;

import com.vantar.database.dto.*;

@Mongo
public class StoredQuery extends DtoBase {

    public Long id;
    @Required
    @Unique
    public String title;
    @Required
    public String dtoName;
    @Required
    public String q;
}