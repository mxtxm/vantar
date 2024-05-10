package com.vantar.admin.test;

import com.vantar.database.dto.*;
import java.util.*;

@NoStore
public class WebUnitTestCaseItem extends DtoBase {

    @Required
    public Integer order;
    @Required
    public String url;
    @Required
    public String httpMethod;
    public Map<String, String> headers;
    public Map<String, Object> inputMap;
    public List<Object> inputList;
    public Map<String, String> fileUploads;

    public Integer assertStatusCode;
    public String assertResponse;
    public Map<String, String> assertHeaders;
    public Map<String, String> assertInfo;


    public Set<String> stackFieldsAdd;
    public Set<String> stackFieldsRemove;
}