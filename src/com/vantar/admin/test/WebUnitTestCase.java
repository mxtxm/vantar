package com.vantar.admin.test;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.*;

@Mongo
@Index({"tag:1,order:1",})
public class WebUnitTestCase extends DtoBase {

    public Long id;

    @Required
    public String tag;
    @Required
    public String title;
    @Required
    public Integer order;
    public Integer testCount;

    @NoList
    public List<WebUnitTestCaseItem> tests;

    @CreateTime
    public DateTime createT;


    @Override
    public boolean beforeUpdate() {
        return beforeInsert();
    }

    @Override
    public boolean beforeInsert() {
        removeNullProperties("testCount");
        testCount = tests == null ? 0 : tests.size();
        return true;
    }
}