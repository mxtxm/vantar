package com.vantar.admin.documentation;

import com.vantar.database.dto.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.*;
import java.util.*;

/**
 * System DTO manifest and properties
 * as details
 * as json samples
 */
public class DtoManifest {

    @SuppressWarnings({"unchecked"})
    public static String get() {
        StringBuilder d = new StringBuilder(100000);
        d   .append("# Dto objects #\n\n")
            .append("The manifest of all data objects and properties.\n")
            .append("Darker green = a main object that is stored inside the database.\n")
            .append("Lighter green = a sub version of the main object.\n")
            .append("Dull green = an object that is not stored in the database.\n");

        for (DtoDictionary.Info info : DtoDictionary.getAll()) {
            Dto dto = info.getDtoInstance();
            StringBuilder enums = new StringBuilder(1000);
            String className = info.dtoClass.getSimpleName();
            if (info.dtoClass.isAnnotationPresent(NoStore.class)) {
                d   .append("<label id='").append(className).append("'></label>\n")
                    .append("###### ").append(className).append(" <label class='database'/>(no store)</label> ######\n");
            } else {
                d   .append("<label id='").append(className).append("'></label>\n")
                    .append("## ").append(className).append(" <label class='database'/>(").append(info.dbms).append(")</label> ##\n");
            }

            for (Field field : dto.getClass().getFields()) {
                int m = field.getModifiers();
                if (Modifier.isFinal(m) || Modifier.isStatic(m) || field.isAnnotationPresent(NoStore.class)) {
                    continue;
                }

                Class<?> type = field.getType();
                String tClassName = type.getSimpleName();
                d.append("* ").append(tClassName);

                if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                    Class<?> gType = dto.getPropertyGenericTypes(field.getName())[0];
                    String gClassName = gType.getSimpleName();
                    d.append("&lt;").append(gClassName).append("&gt;");
                    if (gType.isEnum()) {
                        enums
                            .append("\n<label id='").append(info.dtoClass.getSimpleName()).append('-')
                            .append(gClassName).append("' class='enum'></label>\n")
                            .append("<h5 class='enum'>enum: ").append(gClassName).append("</h5><ul class='enum'>");
                        Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) gType;
                        for (Enum<?> x : enumType.getEnumConstants()) {
                            enums.append("<li>").append(x.toString()).append("</li>");
                        }
                        enums.append("</ul>\n\n");
                    }

                } else if (type.equals(Map.class)) {
                    Class<?>[] genericTypes = dto.getPropertyGenericTypes(field.getName());
                    d   .append("&lt;").append(genericTypes[0].getSimpleName()).append(", ")
                        .append(genericTypes[1].getSimpleName()).append("&gt;");

                } else if (type.isEnum()) {
                    enums
                        .append("\n<label id='").append(info.dtoClass.getSimpleName()).append('-')
                        .append(tClassName).append("' class='enum'></label>\n")
                        .append("<h5 class='enum'>enum: ").append(tClassName).append("</h5><ul class='enum'>");
                    Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                    for (Enum<?> x : enumType.getEnumConstants()) {
                        enums.append("<li>").append(x.toString()).append("</li>");
                    }
                    enums.append("</ul>\n\n");
                }
                d.append(" ").append(field.getName()).append("\n");
            }

            if (enums.length() > 0) {
                d.append(enums);
            }

            // > > > inner class
            String parentClass = info.dtoClass.getSimpleName();
            for (DtoDictionary.Info infoSub : DtoDictionary.getSubClasses(parentClass)) {
                Class<? extends Dto> innerClass = infoSub.dtoClass;
                String href = parentClass + '-' + innerClass.getSimpleName();
                String name = parentClass + '.' + innerClass.getSimpleName();

                d.append("\n<label id='").append(href).append("'></label>\n");
                d.append("#### ").append(name).append(" ####\n");
                for (Field field : innerClass.getFields()) {
                    if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    Class<?> type = field.getType();
                    String tClassName = type.getSimpleName();
                    d.append("* ").append(tClassName);

                    if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                        Class<?>[] g = ClassUtil.getGenericTypes(field);
                        if (g == null || g.length != 1) {
                            ServiceLog.log.warn("! invalid generics ({}.{})", name, field.getName());
                            continue;
                        }
                        String[] parts = StringUtil.split(g[0].getTypeName(), '.');
                        d   .append("&lt;").append(parts[parts.length - 1]).append("&gt;");

                    } else if (type.equals(Map.class)) {
                        Class<?>[] g = ClassUtil.getGenericTypes(field);
                        if (g == null || g.length != 2) {
                            ServiceLog.log.warn("! invalid generics ({}.{})", name, field.getName());
                            continue;
                        }
                        String[] partsK = StringUtil.splitTrim(g[0].getTypeName(), '.');
                        String[] partsV = StringUtil.splitTrim(g[1].getTypeName(), '.');
                        d   .append("&lt;")
                            .append(partsK[partsK.length - 1]).append(", ").append(partsV[partsV.length - 1])
                            .append("&gt;");

                    } else if (type.isEnum()) {
                        d   .append("\n<label id='").append(innerClass.getSimpleName()).append('-')
                            .append(tClassName).append("' class='enum'></label>\n");

                        enums.append("<h5 class='enum'>enum: ").append(tClassName).append("</h5><ul class='enum'>");
                        final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                        for (Enum<?> x : enumType.getEnumConstants()) {
                            enums.append("<li>").append(x.toString()).append("</li>");
                        }
                        enums.append("</ul>\n");
                    }
                    d.append(" ").append(field.getName()).append("\n");
                }
            }
            // < < < inner class
        }

        return d.toString();
    }

    public static String getJson() {
        StringBuilder d = new StringBuilder(100000);
        d   .append("# Dto objects as JSON samples #\n\n")
            .append("The manifest of all data objects and properties.\n")
            .append("Darker green = a main object that is stored inside the database.\n")
            .append("Lighter green = a sub version of the main object.\n")
            .append("Dull green = an object that is not stored in the database.\n");

        for (DtoDictionary.Info info : DtoDictionary.getAll()) {
            Dto dto = info.getDtoInstance();
            String className = info.dtoClass.getSimpleName();
            if (info.dtoClass.isAnnotationPresent(NoStore.class)) {
                d   .append("<label id='").append(className).append("'></label>\n")
                    .append("###### ").append(className).append(" <label class='database'/>(no store)</label> ######\n");
            } else {
                d   .append("<label id='").append(className).append("'></label>\n")
                    .append("## ").append(className).append(" <label class='database'/>(").append(info.dbms).append(")</label> ##\n");
            }

            d.append("<pre>").append(Json.getWithNulls().toJsonPretty(DummyValue.getDummyDto(dto))).append("</pre>\n");

            // > > > inner class
            String parentClass = info.dtoClass.getSimpleName();
            for (DtoDictionary.Info infoSub : DtoDictionary.getSubClasses(parentClass)) {
                Class<? extends Dto> innerClass = infoSub.dtoClass;
                String iClassName = innerClass.getSimpleName();
                d   .append("\n<label id='").append(iClassName).append('-').append(iClassName).append("'></label>\n")
                    .append("#### ").append(iClassName).append('.').append(iClassName).append(" ####\n")
                    .append("<pre>")
                    .append(Json.getWithNulls().toJsonPretty(DummyValue.getDummyDto(ClassUtil.getInstance(innerClass))))
                    .append("</pre>\n");
            }
            // < < < inner class
        }

        return d.toString();
    }
}
