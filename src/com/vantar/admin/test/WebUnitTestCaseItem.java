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

    public Boolean assertCheckHeaders;
    public Map<String, String> assertHeaders;
    public AssertCheckMethod assertHeadersCheckMethod;
    public Set<String> assertHeadersInclude;
    public Set<String> assertHeadersExclude;

    public String assertResponse;
    public String assertResponseObjectClass;
    public AssertCheckMethod assertResponseCheckMethod;
    public Set<String> assertResponseInclude;
    public Set<String> assertResponseExclude;


    public enum AssertCheckMethod {
        exact,
        assertInResponse,
        responseInAssert,
    }
}