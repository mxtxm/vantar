package com.vantar.database.dto;

import java.util.List;


public interface HasManyRelation {

    List<DtoOneWalkRelation> getOneWalkRelations();

    List<DtoTwoWalkRelation> getTwoWalkRelations();

    int getIdValue();
}
