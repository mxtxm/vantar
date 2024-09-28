package com.vantar.database.dto;


public class DtoOneWalkRelation {

    final public Dto relationDto;
    final public String fieldNameBaseId;
    final public String fieldNameRelationId;


    public DtoOneWalkRelation(Dto relationDto, String fieldNameBaseId, String fieldNameRelationId) {
        this.relationDto = relationDto;
        this.fieldNameBaseId = fieldNameBaseId;
        this.fieldNameRelationId = fieldNameRelationId;
    }
}
