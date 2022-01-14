package com.vantar.admin.model.document;

import com.vantar.admin.model.*;
import com.vantar.common.Settings;
import com.vantar.database.dto.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.*;
import java.util.*;


public class DtoDocumentCreator {

    @SuppressWarnings({"unchecked"})
    public static void create() {
        StringBuilder document = new StringBuilder();
        document.append("# Dto objects #\n\n");

        List<DtoDictionary.Info> dtos = new ArrayList<>(DtoDictionary.getAll());

        for (DtoDictionary.Info info : dtos) {
            Dto dto = info.getDtoInstance();
            StringBuilder enums = new StringBuilder();

            if (info.dtoClass.isAnnotationPresent(NoStore.class)) {
                document.append("\n<label id='").append(info.dtoClass.getSimpleName()).append("'></label>\n###### ")
                    .append(info.dtoClass.getSimpleName()).append(" <label class='database'/>(not stored)</label> ######\n");
            } else {
                document.append("\n<label id='").append(info.dtoClass.getSimpleName()).append("'></label>\n## ")
                    .append(info.dtoClass.getSimpleName()).append(" <label class='database'/>(").append(info.dbms)
                    .append(")</label> ##\n");
            }

            List<Field> fields = new ArrayList<>();
            for (Field f : dto.getClass().getFields()) {
                int m = f.getModifiers();
                if (Modifier.isFinal(m) || Modifier.isStatic(m)) {
                    continue;
                }
                if (f.isAnnotationPresent(NoStore.class)) {
                    continue;
                }
                fields.add(f);
            }

            for (Field field : fields) {
                Class<?> type = field.getType();
                document.append("* ").append(type.getSimpleName());

                if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                    Class<?> gType = dto.getPropertyGenericTypes(field.getName())[0];
                    document.append("&lt;").append(gType.getSimpleName()).append("&gt;");
                    if (gType.isEnum()) {
                        enums.append("\n<label id='").append(info.dtoClass.getSimpleName()).append('-').append(gType.getSimpleName()).append("'></label>\n");
                        enums.append("##### enum: ").append(gType.getSimpleName()).append(" #####\n");
                        Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) gType;
                        for (Enum<?> x : enumType.getEnumConstants()) {
                            enums.append("* ").append(x.toString()).append("\n");
                        }
                        enums.append("\n\n");
                    }

                } else if (type.equals(Map.class)) {
                    Class<?>[] genericTypes = dto.getPropertyGenericTypes(field.getName());
                    document
                        .append("&lt;").append(genericTypes[0].getSimpleName()).append(", ")
                        .append(genericTypes[1].getSimpleName()).append("&gt;");

                } else if (type.isEnum()) {
                    enums.append("\n<label id='").append(info.dtoClass.getSimpleName()).append('-').append(type.getSimpleName()).append("'></label>\n");
                    enums.append("##### enum: ").append(type.getSimpleName()).append(" #####\n");
                    Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                    for (Enum<?> x : enumType.getEnumConstants()) {
                        enums.append("* ").append(x.toString()).append("\n");
                    }
                    enums.append("\n\n");
                }
                document.append(" ").append(field.getName()).append("\n");
            }

            if (enums.length() > 0) {
                document.append(enums);
            }

            // > > > inner class
            for (Class<?> innerClass : dto.getClass().getDeclaredClasses()) {
                String href = info.dtoClass.getSimpleName() + '-' + innerClass.getSimpleName();
                String name = info.dtoClass.getSimpleName() + '.' + innerClass.getSimpleName();

                document.append("\n<label id='").append(href).append("'></label>\n");
                document.append("#### ").append(name).append(" ####\n");
                for (Field field : innerClass.getFields()) {
                    if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    Class<?> type = field.getType();
                    document.append("* ").append(type.getSimpleName());

                    if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                        Class<?>[] g = ClassUtil.getGenericTypes(field);
                        if (g == null || g.length != 1) {
                            AdminDocument.log.warn("! invalid generics ({}.{})", dto.getClass().getSimpleName(), field.getName());
                            continue;
                        }
                        String[] parts = StringUtil.split(g[0].getTypeName(), '.');
                        document
                            .append("&lt;")
                            .append(parts[parts.length - 1])
                            .append("&gt;");

                    } else if (type.equals(Map.class)) {
                        Class<?>[] g = ClassUtil.getGenericTypes(field);
                        if (g == null || g.length != 2) {
                            AdminDocument.log.warn("! invalid generics ({}.{})", dto.getClass().getSimpleName(), field.getName());
                            continue;
                        }
                        String[] partsK = StringUtil.split(g[0].getTypeName(), '.');
                        String[] partsV = StringUtil.split(g[1].getTypeName(), '.');
                        document
                            .append("&lt;")
                            .append(partsK[partsK.length - 1])
                            .append(", ")
                            .append(partsV[partsV.length - 1])
                            .append("&gt;");

                    } else if (type.isEnum()) {
                        document.append("\n<label id='").append(innerClass.getSimpleName()).append('-').append(type.getSimpleName()).append("'></label>\n");
                        enums.append("##### enum: ").append(type.getSimpleName()).append(" #####\n");
                        final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                        for (Enum<?> x : enumType.getEnumConstants()) {
                            enums.append("* ").append(x.toString()).append("\n");
                        }
                        enums.append("\n\n");
                    }
                    document.append(" ").append(field.getName()).append("\n");
                }
            }
            // < < < inner class
        }

        FileUtil.makeDirectory(Settings.config.getProperty("documents.dir"));
        FileUtil.write(Settings.config.getProperty("documents.dir") + "objects.md", document.toString());
    }

}
