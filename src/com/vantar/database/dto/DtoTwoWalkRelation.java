package com.vantar.database.dto;


public class DtoTwoWalkRelation {

    final public Dto relationDto;
    final public Dto relationDtoTwo;
    final public String fieldNameBaseId;
    final public String fieldNameRelationId;
    final public String fieldNameRelationIdTwo;
    final public String fieldNameDestination;


    public DtoTwoWalkRelation(
        Dto relationDto,
        Dto relationDtoTwo,
        String fieldNameBaseId,
        String fieldNameRelationId,
        String fieldNameRelationIdTwo,
        String fieldNameDestination) {

        this.relationDto = relationDto;
        this.relationDtoTwo = relationDtoTwo;
        this.fieldNameBaseId = fieldNameBaseId;
        this.fieldNameRelationId = fieldNameRelationId;
        this.fieldNameRelationIdTwo = fieldNameRelationIdTwo;
        this.fieldNameDestination = fieldNameDestination;
    }
}
