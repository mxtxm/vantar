package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.util.object.ObjectUtil;
import java.util.List;


public class PageData {

    public final List<Dto> data;
    public final int page;
    public final int length;
    public final long total;


    public PageData(List<Dto> data, int page, int length, long total) {
        this.data = data;
        this.page = page;
        this.length = length;
        this.total = total;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }
}
