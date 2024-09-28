package com.vantar.service.patch.dto;

import com.vantar.database.dto.*;
import com.vantar.util.datetime.DateTime;
import java.util.List;

@Mongo
public class PatchHistory extends DtoBase {

    public Long id;
    public String patchClass;
    @CreateTime
    public DateTime executedTime;
    @NoList
    public List<String> success;
    @NoList
    public List<String> fail;
    public Integer successCount;
    public Integer failCount;
}
