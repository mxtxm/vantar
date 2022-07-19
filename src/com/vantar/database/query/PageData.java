package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class PageData {

    public List<String> columns;
    public final List<? extends Dto> data;
    public final int page;
    public final int length;
    public final long total;


    public <T extends Dto> PageData(List<T> data, int page, int length, long total) {
        this.data = data;
        this.page = page;
        this.length = length;
        this.total = total;
    }

    public <T extends Dto> PageData(List<T> data) {
        this.data = data;
        this.page = 1;
        this.length = data.size();
        this.total = this.length;
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
