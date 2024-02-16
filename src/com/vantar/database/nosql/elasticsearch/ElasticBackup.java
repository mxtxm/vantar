package com.vantar.database.nosql.elasticsearch;

import com.vantar.business.CommonRepoElastic;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.util.datetime.DateTimeRange;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.*;
import com.vantar.util.string.*;
import com.vantar.web.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


public class ElasticBackup {

    public static void dump(String dumpPath, WebUi ui) {
        dump(dumpPath, null, ui);
    }

    public static void dump(String dumpPath, DateTimeRange dateRange, WebUi ui) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(dumpPath);
        } catch (FileNotFoundException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zos = new ZipOutputStream(bos);

        if (ui != null) {
            ui.addHeading(2, "ElasticSearch > " + dumpPath).write();
        }

        long startTime = System.currentTimeMillis();
        long r = 0;

        CommonRepoElastic repo = new CommonRepoElastic();
        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                long startItemTime = System.currentTimeMillis();
                Dto dto = info.getDtoInstance();
                String collection = dto.getStorage();

                zos.putNextEntry(new ZipEntry(collection + ".dump"));
                List<Dto> data;
                int l = 0;

                try {
                    if (dateRange == null) {
                        data = CommonRepoElastic.getAll(dto);
                    } else {
                        String dateField = null;
                        String dateFieldUpdate = null;
                        for (String name : dto.getProperties()) {
                            if (dto.hasAnnotation(name, CreateTime.class)) {
                                dateField = name;
                            }
                            if (dto.hasAnnotation(name, UpdateTime.class)) {
                                dateFieldUpdate = name;
                            }
                        }
                        if (dateFieldUpdate != null) {
                            dateField = dateFieldUpdate;
                        }

                        if (dateField == null) {
                            data = CommonRepoElastic.getAll(dto);
                        } else {
                            QueryBuilder q = new QueryBuilder(dto);
                            q.condition().between("", dateRange);
                            data = CommonRepoElastic.getData(q);
                        }
                    }
                } catch (NoContentException e) {
                    continue;
                } catch (DatabaseException e) {
                    if (ui != null) {
                        ui.addErrorMessage(e);
                    }
                    continue;
                }

                for (Dto item : data) {
                    zos.write((Json.d.toJson(item) + "\n").getBytes());
                    ++l;
                }
                long elapsed = (System.currentTimeMillis() - startItemTime) / 1000;
                if (ui != null) {
                    ui.addKeyValue(collection, l + " records,    " + (elapsed * 10 / 10d) + "s").write();
                }
                r += l;
            }
            zos.closeEntry();
            if (ui != null) {
                ui.addBlock("pre",
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                        "\n" + r + " records" +
                        "\n" + FileUtil.getSizeReadable(dumpPath)
                );
            }
        } catch (IOException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                if (ui != null) {
                    ui.addErrorMessage(e);
                }
            }
        }

        if (ui != null) {
            ui.blockEnd().write();
        }
    }

    public static void restore(String zipPath, boolean deleteData, WebUi ui) {
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(zipPath);
        } catch (IOException e) {
            ui.addErrorMessage(e).write();
            return;
        }

        ui.addHeading(2, zipPath + " > ElasticSearch").write();

        long startTime = System.currentTimeMillis();
        long r = 0;

        CommonRepoElastic write = new CommonRepoElastic();
        write.startTransaction();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            long startCollectionTime = System.currentTimeMillis();
            ZipEntry entry = entries.nextElement();
            String[] parts = StringUtil.split(entry.getName(), '/');
            String collection = StringUtil.replace(parts[parts.length - 1], ".dump", "");
            DtoDictionary.Info info = DtoDictionary.get(collection);
            if (info == null) {
                ui.addErrorMessage(" وجود ندارد " + collection);
                continue;
            }
            Dto dto = info.getDtoInstance();

            try {
                InputStream stream = zipFile.getInputStream(entry);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    if (deleteData) {
                        ElasticWrite.purgeData(collection);
                    }

                    int i = 1;
                    while (reader.ready()) {
                        dto.reset();
                        dto.set(Json.d.mapFromJson(reader.readLine(), String.class, Object.class), Dto.Action.INSERT);
                        write.insert(dto);
                    }
                    write.commit();

                    long elapsed = (System.currentTimeMillis() - startCollectionTime) / 1000;
                    ui.addKeyValue(collection, (i-1) + " records,    " + (elapsed * 10 / 10d) + "s").write();
                    r += i - 1;

                } catch (IOException | DatabaseException e) {
                    ui.addErrorMessage(e);
                }
            } catch (IOException e) {
                ui.addErrorMessage(e);
            }
        }

        ui  .addBlock("pre",
                "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                "\n" + r + " records" +
                "\n" + FileUtil.getSizeReadable(zipPath)
            )
            .blockEnd()
            .write();
    }
}
