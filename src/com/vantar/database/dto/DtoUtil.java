package com.vantar.database.dto;

import com.vantar.util.json.Json;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.Field;
import java.util.*;


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

    /**
     * Returns the last field, only dto fields will be traversed
     * @param dto the dto to start traverse on
     * @param traversable i.e "user.role.title"
     * @return null if field-name is invalid or invalid
     */
    @SuppressWarnings("unchecked")
    public static Field getLastTraversedField(Dto dto, String traversable) {
        if (!StringUtil.contains(traversable, '.')) {
            return dto.getField(traversable);
        }

        Field field = null;
        Dto dtoX = dto;
        for (String propertyName : StringUtil.split(traversable, '.')) {
            field = dtoX.getField(propertyName);
            if (field == null) {
                return null;
            }

            Class<?> fieldType = field.getType();

            if (ClassUtil.isInstantiable(fieldType, Dto.class)) {
                dtoX = DtoDictionary.getInstance((Class<? extends Dto>) fieldType);
                if (dtoX == null) {
                    return null;
                }
                continue;
            }

            return field;
        }
        return field;
    }
}