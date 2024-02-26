package com.vantar.admin.Dto;

import com.vantar.database.dto.*;

@Mongo
public class Bugger extends DtoBase {

    public Long id;
    @Required
    public String environment;
    @Required
    public String url;
    public String data;
    @Required
    public String description;
}