package com.vantar.database.dto;

import java.util.List;


public class ManyToManyDefinition {

    public String storage;
    public String fkLeft;
    public String fkRight;
    public Long fkLeftValue;
    public List<Long> fkRightValue;
}
