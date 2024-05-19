package com.vantar.database.sql;

import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.exception.DatabaseException;
import com.vantar.locale.VantarKey;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;


public class SqlArtaSynch {

    private static final Logger log = LoggerFactory.getLogger(SqlArtaSynch.class);
    private final String workDir;
    private final SqlConfig config;


    public SqlArtaSynch(SqlConfig config) {
        this.config = config;
        workDir = this.getClass().getResource("/arta/app/").getPath();
    }

    public SqlArtaSynch cleanup() {
        DirUtil.removeDirectory(workDir + "models");
        DirUtil.removeDirectory(workDir + "_temp");
        return this;
    }

    public SqlArtaSynch createFiles() throws DatabaseException {
        createArtaConfig();
        for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
            Dto dto = info.getDtoInstance();
            createArtaModel(dto);
        }
        return this;
    }

    public String build() {
        try {
            BufferedReader input = new BufferedReader(
                new InputStreamReader(Runtime.getRuntime().exec("php " + workDir + "build.php").getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = input.readLine()) != null) {
                output.append(line).append("\n");
            }
            input.close();
            return output.toString();
        }
        catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    private void createArtaConfig() throws DatabaseException {
        String[] server = StringUtil.split(config.getDbServer(), VantarParam.SEPARATOR_KEY_VAL);
        String content =
            "[arta]\n" +
                "temp_path = APP_DIR/_temp\n" +
                "debug = on\n" +
                "build = on\n" +
                "\n" +
                "[database:dbo]\n" +
                "Engine = " + SqlConnection.getDbEngine(config) + "\n" +
                "server = " + server[0] + "\n" +
                "port = " + server[1] + "\n" +
                "dbname = " + config.getDbDatabase() + "\n" +
                "user = " + config.getDbUser() + "\n" +
                "password = "+ config.getDbPassword() + "\n" +
                "\n" +
                "[model:m]\n" +
                "database = dbo\n" +
                "path = APP_DIR/models\n" +
                "dropsql = on\n" +
                "build = on\n" +
                "optimize_debug = on\n" +
                "optimize = on\n";

        String filepath = StringUtil.rtrim(workDir, '/') + "/config.ini";
        if (FileUtil.write(filepath, content)) {
            log.info("({}) created", filepath);
        } else {
            throw new DatabaseException(VantarKey.ARTA_FILE_CREATE_ERROR, filepath);
        }
    }

    private void createArtaModel(Dto dto) throws DatabaseException {
        String dtoFk = StringUtil.toSnakeCase(dto.getStorage()) + "_id";

        StringBuilder content = new StringBuilder();
        content.append("<?php\n// created by Vantar\n\n");
        content.append("class ").append(dto.getClass().getSimpleName()).append(" extends Model {\n");
        content.append("\n    public $__table__  = '").append(dto.getStorage()).append("';\n\n");

        for (Map.Entry<String, Class<?>> entry : dto.getPropertyTypes().entrySet()) {
            String name = entry.getKey();
            Class<?> type = entry.getValue();

            Field field = dto.getField(name);
            if (field.isAnnotationPresent(NoStore.class) || field.isAnnotationPresent(ManyToManyGetData.class)) {
                continue;
            }

            name = StringUtil.toSnakeCase(name);

            if (field.isAnnotationPresent(ManyToManyStore.class)) {
                String[] parts = StringUtil.split(field.getAnnotation(ManyToManyStore.class).value(), VantarParam.SEPARATOR_NEXT);
                String table = parts[0];
                String className = parts[1];
                createArtaJunctionModel(table, dto.getClass().getSimpleName(), dtoFk, className, name);
                continue;
            }

            if (name.equals(VantarParam.ID)) {
                content.append("    public $id = array(\n" +
                    "        'key' => true,\n" +
                    "    );\n\n"
                );
                continue;
            }

            if (name.contains("_id")) {
                String storageName = StringUtil.remove(name, "_id");
                Dto dtoRelation = null;
                for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
                    Dto dtoRelationCandidate = info.getDtoInstance();
                    if (dtoRelationCandidate.getStorage().equals(storageName)) {
                        dtoRelation = dtoRelationCandidate;
                        break;
                    }
                }

                if (dtoRelation != null) {
                    content
                        .append("    public $").append(name).append(" = array(\n")
                        .append("        'relation' => '").append(dtoRelation.getClass().getSimpleName()).append("',\n")
                        .append("    );\n\n");
                    continue;
                }
            }

            String typeStr;
            if (type.equals(Long.class)) {
                typeStr = "LONG";
            } else if (type.equals(Integer.class)) {
                typeStr = "INT";
            } else if (type.equals(Double.class)) {
                typeStr = "DOUBLE";
            } else if (type.equals(Float.class)) {
                typeStr = "FLOAT";
            } else if (type.equals(Boolean.class)) {
                typeStr = "BOOL";
            } else if (type.equals(Character.class)) {
                typeStr = "CHAR";
            } else if (type.equals(DateTime.class)) {
                if (field.isAnnotationPresent(Date.class)) {
                    typeStr = "DATE";
                } else if (field.isAnnotationPresent(Time.class)) {
                    typeStr = "TIME";
                } else {
                    typeStr = "TIMESTAMP";
                }
            } else {
                typeStr = config.getDbDriverClass().contains("postgr") ? "TEXT" : "VARCHAR";
            }

            String defaultValue = field.isAnnotationPresent(Default.class) ? field.getAnnotation(Default.class).value() : null;
            if (type.equals(DateTime.class) && name.contains("create")) {
                defaultValue = "NOW()";
            }

            String limit = field.isAnnotationPresent(Limit.class) ? field.getAnnotation(Limit.class).value() : null;
            if (limit != null) {
                String[] parts = StringUtil.split(limit, VantarParam.SEPARATOR_COMMON);
                if (type.equals(String.class)) {
                    if (parts.length == 2) {
                        Integer min = StringUtil.toInteger(parts[0]);
                        Integer max = StringUtil.toInteger(parts[1]);
                        if (min != null && max != null) {
                            limit = "        'len' => array(" + min + ", " + max + "),\n";
                        }
                    } else {
                        Integer len = StringUtil.toInteger(limit);
                        if (len != null) {
                            limit = "        'len' => " + len + ",\n";
                        }
                    }
                } else {
                    if (parts.length == 2) {
                        Integer min = StringUtil.toInteger(parts[0]);
                        Integer max = StringUtil.toInteger(parts[1]);
                        if (min != null && max != null) {
                            limit = "    'min' => " + min + "),\n";
                            limit += "        'max' => " + max + "),\n";
                        }
                    }
                }
            }
            content
                .append("    public $").append(name).append(" = array(\n")
                .append("        'type' => ").append(typeStr).append(",\n")
                .append(field.isAnnotationPresent(Required.class) ? "        'required' => true,\n" : "")
                .append(field.isAnnotationPresent(Unique.class) ? "        'unique' => true,\n" : "")
                .append(defaultValue == null ? "" : "        'default' => '" + defaultValue + "',\n")
                .append(limit == null ? "" : limit)
                .append("    );\n\n");
        }

        content.append("    public function __construct($params=null) {\n" +
            "        parent::__construct('m', $params);\n" +
            "    }\n" +
            "}"
        );

        String filepath = StringUtil.rtrim(workDir, '/') + "/models/" + dto.getClass().getSimpleName() + ".php";
        if (FileUtil.write(filepath, content.toString())) {
            log.info("({}) created", filepath);
        } else {
            throw new DatabaseException(VantarKey.ARTA_FILE_CREATE_ERROR, filepath);
        }
    }

    private void createArtaJunctionModel(String table, String classA, String idA, String classB, String idB) throws DatabaseException {
        StringBuilder content = new StringBuilder();
        content.append("<?php\n// created by Vantar\n\n");
        content.append("class ").append(table).append(" extends Model {\n");
        content.append("\n    public $__table__  = '").append(table).append("';\n\n");

        content.append("    public $id = array(\n" +
            "        'key' => true,\n" +
            "    );\n\n"
        );

        content
            .append("    public $").append(idA).append(" = array(\n")
            .append("        'relation' => '").append(classA).append("',\n")
            .append("    );\n\n");

        content
            .append("    public $").append(idB).append(" = array(\n")
            .append("        'relation' => '").append(classB).append("',\n")
            .append("    );\n\n");

        content.append("    public function __construct($params=null) {\n" +
            "        parent::__construct('m', $params);\n" +
            "    }\n" +
            "}"
        );

        String filepath = StringUtil.rtrim(workDir, '/') + "/models/" + table + ".php";
        if (FileUtil.write(filepath, content.toString())) {
            log.info("({}) created", filepath);
        } else {
            throw new DatabaseException(VantarKey.ARTA_FILE_CREATE_ERROR, filepath);
        }
    }
}