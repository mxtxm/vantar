package com.vantar.util.upload.dto;

import com.vantar.database.dto.*;

@Mongo
public class AttachmentCache extends DtoBase {

    public Long id;
    public Attachment attachment;
}