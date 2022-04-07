package com.vantar.database.dto;

import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.json.*;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import org.slf4j.*;
import java.lang.reflect.Field;
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

    /**
     * Returns the last field, only dto fields will be traversed
     * @param dto the dto to start traverse on
     * @param traversable i.e "user.role.title"
     * @return null if field-name is invalid or invalid
     */
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
                dtoX = (Dto) DtoDictionary.getInstance(fieldType);
                if (dtoX == null) {
                    return null;
                }
                continue;
            }

            return field;
        }
        return field;
    }
    private static final Logger log = LoggerFactory.getLogger(Params.class);

    /**
     * todo: travers collections
     */
    public static Class<?> getLastTraversedPropertyType(Dto dto, String traversable) {
        Field field;
        if (!StringUtil.contains(traversable, '.')) {
            field = dto.getField(traversable);

            if (field == null) {
                return null;
            }
            Class<?> fieldType = field.getType();

            if (CollectionUtil.isCollection(fieldType)) {
                Class<?> genericType = dto.getPropertyGenericTypes(field)[0];
                if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                    dto = (Dto) DtoDictionary.getInstance(genericType);
                    if (dto == null) {
                        return null;
                    }
                }
                // todo: travers collections
                return genericType;
            }

            if (CollectionUtil.isMap(fieldType)) {
                Class<?> genericType = dto.getPropertyGenericTypes(field)[1];
                if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                    dto = (Dto) DtoDictionary.getInstance(genericType);
                    if (dto == null) {
                        return null;
                    }
                }
                return genericType;
                // todo: travers collections
            }

            return field.getType();
        }

        field = null;
        Dto dtoX = dto;
        String[] split = StringUtil.split(traversable, '.');
        for (int i = 0, l = split.length; i < l; i++) {
            String propertyName = split[i];
            field = dtoX.getField(propertyName);
            if (field == null) {
                return null;
            }

            Class<?> fieldType = field.getType();

            if (ClassUtil.isInstantiable(fieldType, Dto.class)) {
                dtoX = (Dto) DtoDictionary.getInstance(fieldType);
                if (dtoX == null) {
                    return null;
                }
                continue;
            }

            if (CollectionUtil.isCollection(fieldType)) {
                Class<?> genericType = dtoX.getPropertyGenericTypes(field)[0];
                if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                    dtoX = (Dto) DtoDictionary.getInstance(genericType);
                    if (dtoX == null) {
                        return null;
                    }
                    continue;
                }
                return genericType;
                // todo: travers collections
            }

            if (CollectionUtil.isMap(fieldType)) {
                Class<?> genericType = dtoX.getPropertyGenericTypes(field)[1];
                if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                    dtoX = (Dto) DtoDictionary.getInstance(genericType);
                    if (dtoX == null) {
                        return null;
                    }
                    continue;
                }
                return genericType;
                // todo: travers collections
            }

            return fieldType;
        }

        if (field == null) {
            return null;
        }
        Class<?> fieldType = field.getType();

        if (CollectionUtil.isCollection(fieldType)) {
            Class<?> genericType = dtoX.getPropertyGenericTypes(field)[0];
            if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                dtoX = (Dto) DtoDictionary.getInstance(genericType);
                if (dtoX == null) {
                    return null;
                }
            }
            // todo: travers collections
            return genericType;
        }

        if (CollectionUtil.isMap(fieldType)) {
            Class<?> genericType = dtoX.getPropertyGenericTypes(field)[1];
            if (ClassUtil.isInstantiable(genericType, Dto.class)) {
                dtoX = (Dto) DtoDictionary.getInstance(genericType);
                if (dtoX == null) {
                    return null;
                }
            }
            return genericType;
            // todo: travers collections
        }

        return field.getType();
    }
}
