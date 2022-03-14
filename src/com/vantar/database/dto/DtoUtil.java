package com.vantar.database.dto;

import com.vantar.util.json.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DtoUtil {

    public static String toJson(Dto dto) {
        return Json.d.toJson(dto);
    }

    public static String toJson(Dto dto, Map<String, String> propertyNameMap) {
        return Json.d.toJson(dto.getPropertyValues(false, false, propertyNameMap));
    }

    public static String toJsonSnakeCaseKeys(Dto dto, Map<String, String> propertyNameMap) {
        return Json.d.toJson(dto.getPropertyValues(false, true, propertyNameMap));
    }

    public static String toJson(List<? extends Dto> dtos) {
        return toJson(dtos, null);
    }

    public static String toJson(List<? extends Dto> dtos, Map<String, String> propertyNameMap) {
        List<Map<String, Object>> mappedDtos = new ArrayList<>(dtos.size());
        for (Dto dto : dtos) {
            mappedDtos.add(dto.getPropertyValues(false, false, propertyNameMap));
        }
        return Json.d.toJson(mappedDtos);
    }

    public static String toJsonSnakeCaseKeys(List<? extends Dto> dtos) {
        return toJsonSnakeCaseKeys(dtos, null);
    }

    public static String toJsonSnakeCaseKeys(List<? extends Dto> dtos, Map<String, String> propertyNameMap) {
        List<Map<String, Object>> mappedDtos = new ArrayList<>(dtos.size());
        for (Dto dto : dtos) {
            mappedDtos.add(dto.getPropertyValues(false, true, propertyNameMap));
        }
        return Json.d.toJson(mappedDtos);
    }

    public static List<Map<String, Object>> getPropertyValues(List<? extends Dto> dtos, Map<String, String> propertyNameMap) {
        List<Map<String, Object>> mappedDtos = new ArrayList<>(dtos.size());
        for (Dto dto : dtos) {
            mappedDtos.add(dto.getPropertyValues(false, false, propertyNameMap));
        }
        return mappedDtos;
    }

    public static List<Map<String, Object>> getPropertyValuesSnakeCaseKeys(List<? extends Dto> dtos, Map<String, String> propertyNameMap) {
        List<Map<String, Object>> mappedDtos = new ArrayList<>(dtos.size());
        for (Dto dto : dtos) {
            mappedDtos.add(dto.getPropertyValues(false, true, propertyNameMap));
        }
        return mappedDtos;
    }
}
