package com.vantar.util.upload.dto;

import com.vantar.database.dto.*;
import com.vantar.util.string.StringUtil;
import java.util.*;

@NoStore
public class AttachmentRule extends DtoBase {

    @Required
    @Default("false")
    public Boolean multi;
    public String tag;
    @Required
    @Default("file")
    public String key;
    @Required
    public Set<MediaType> mediaTypes;
    @Required
    @Default("100K")
    public String minSize;
    @Required
    @Default("2M")
    public String maxSize;

    @Default("false")
    public Boolean required;
    public Integer minCount;
    public Integer maxCount;
    public Integer height;
    public Integer width;
    public String path;


    public void addMediaType(MediaType m) {
        if (mediaTypes == null) {
            mediaTypes = new HashSet<>(5, 1);
        }
        mediaTypes.add(m);
    }

    public int getMinSizeBytes() {
        Integer v = StringUtil.scrapeInteger(minSize);
        if (v == null) {
            return 1024;
        }
        if (minSize.endsWith("K")) {
            return v * 1024;
        }
        if (minSize.endsWith("M")) {
            return v * 1024 * 1024;
        }
        return v;
    }

    public int getMaxSizeBytes() {
        Integer v = StringUtil.scrapeInteger(maxSize);
        if (v == null) {
            return 1024 * 1024;
        }
        if (minSize.endsWith("K")) {
            return v * 1024;
        }
        if (minSize.endsWith("M")) {
            return v * 1024 * 1024;
        }
        return v;
    }
}
