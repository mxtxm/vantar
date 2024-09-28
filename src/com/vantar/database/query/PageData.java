package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class PageData {

    public List<String> columns;
    public List<? extends Dto> data;
    public int page;
    // page length
    public int length;
    // result record count for this page ---> recordCount =< length
    public int recordCount;
    // total record count
    public long total;
    public String errors;


    public <T extends Dto> PageData(List<T> data, int page, int length, long total) {
        this.data = data;
        this.page = page;
        this.length = length;
        this.total = total;
        if (data != null) {
            recordCount = data.size();
        }
    }

    public <T extends Dto> PageData(List<T> data) {
        this.data = data;
        this.page = 1;
        this.length = data.size();
        this.total = this.length;
        recordCount = data.size();
    }

    public <T extends Dto> PageData() {
        this.data = new ArrayList<>(1);
        this.page = 1;
        this.length = 0;
        this.total = 0;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }
}
