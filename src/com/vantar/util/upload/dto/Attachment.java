package com.vantar.util.upload.dto;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;

@NoStore
public class Attachment extends DtoBase {

    public DateTime dateTime;
    public String comments;
    public Location location;
    public String username;
    public String userFullName;
    public String tag;
    public Long userId;
    public String path;
    public String originalFilename;
}
