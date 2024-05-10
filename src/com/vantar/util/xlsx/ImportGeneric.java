package com.vantar.util.xlsx;

import com.vantar.exception.VantarException;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.dhatim.fastexcel.reader.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class ImportGeneric {

    public static String FILES_DIR;
    public static String IMPORT_PATH;

    private static final Logger log = LoggerFactory.getLogger(ImportGeneric.class);


    public static void getTemplate(
        HttpServletResponse response,
        String filename,
        String title,
        SheetData... sheets) throws VantarException {

        Xlsx.Config xlxs = new Xlsx.Config(response, filename, title);

        for (SheetData sheetData : sheets) {
            if (sheetData == null) {
                continue;
            }
            xlxs.addWriteEvent(sheetData.title, (context) -> {
                context.sheet.fitToWidth((short) 1);
                context.sheet.rowHeight(0, 13 * 3);
                context.sheet.freezePane(0, 2);
                XlsxStyledBase.setHeader(context);
                Xlsx.setCell(context, sheetData.header);
                XlsxStyledBase.setHeaderType(context);
                Xlsx.setCell(context, sheetData.headerType);
                if (sheetData.event != null) {
                    sheetData.event.onReadRow(context);
                }
            });
        }

        Xlsx.create(xlxs);
    }


    public static class SheetData {

        public String title;
        public Object[] header;
        public Object[] headerType;
        public Event event;


        public interface Event {

            boolean onReadRow(Xlsx.Context context) throws Exception;
        }
    }


    public static ResponseMessage doImportData(Params params, String title, Event... events) throws VantarException {
        int[] sheetProcessOrder = new int[events.length];
        for (int i = 0; i < events.length; ++i) {
            sheetProcessOrder[i] = i;
        }
        return doImportData(params, title, sheetProcessOrder, events);
    }

    public static ResponseMessage doImportData(Params params, String title, int[] sheetProcessOrder
        , Event... events) throws VantarException {

        long t = System.currentTimeMillis();

        Xlsx.Config rXlsx = new Xlsx.Config();
        rXlsx.params = params;
        rXlsx.headerRowcount = 2;
        rXlsx.uploadDir = FILES_DIR + IMPORT_PATH;

        int i = 0;
        ImportData[] imports = new ImportData[events.length];
        for (Event event : events) {
            int order = sheetProcessOrder[i++];
            ImportData importX = new ImportData();
            imports[order] = importX;
            rXlsx.addReadEvent(order, new Xlsx.ReadEvents() {
                @Override
                public boolean onReadHeader(ReadableWorkbook workbook, Row row) {
                    importX.title = rXlsx.title;
                    importX.setHeader(row, Xlsx.getString(row, 0));
                    return true;
                }

                @Override
                public boolean onReadRow(ReadableWorkbook workbook, Row row) {
                    if (importX.successPrevious(row, Xlsx.getString(row, 0))) {
                        return true;
                    }
                    try {
                        boolean isRowEmpty = true;
                        if (row.getCellCount() > 0) {
                            for (Cell cell : row) {
                                if (cell != null && ObjectUtil.isNotEmpty(Xlsx.getString(cell))) {
                                    isRowEmpty = false;
                                    break;
                                }
                            }
                        }
                        if (isRowEmpty) {
                            return false;
                        }
                        return event.onReadRow(row, importX);
                    } catch (Exception e) {
                        importX.fail(row, e);
                    }
                    return true;
                }

                @Override
                public void onError(ReadableWorkbook workbook, Exception e, boolean rowError, int sheetIndex) {
                    if (e instanceof Xlsx.EndRowReadException) {
                        return;
                    }
                    log.error(" ! unexpected error sheet={} row-error={}", sheetIndex, rowError, e);
                }
            });
        }
        log.info("\n\n---> import start > {}", title);
        String filename = Xlsx.read(rXlsx);

        // > > > write result

        String filepath = rXlsx.uploadDir + filename;
        String resultFilepath = filepath + ".result";

        Xlsx.Config wXlsx = new Xlsx.Config(resultFilepath, title + " : result");
        for (ImportData importX : imports) {
            wXlsx.addWriteEvent(importX.title, (context) -> {
                context.sheet.fitToWidth((short) 1);
                context.sheet.freezePane(0, 1);
                context.sheet.width(0, 80);
                context.sheet.rowHeight(0, 13 * 3);

                XlsxStyledBase.setHeader(context);
                if (!importX.hasResultCol) {
                    Xlsx.setCell(context, importX.headerRow.getMessage());
                }
                for (Cell cell : (Row) importX.headerRow.data) {
                    Xlsx.setCell(context, Xlsx.getStringWrite(cell));
                }

                Map<Integer, Xlsx.CellType> cellTypes = new HashMap<>(200, 1);
                if (importX.headerRowType != null) {
                    XlsxStyledBase.setHeaderType(context);
                    if (!importX.hasResultCol) {
                        Xlsx.setCell(context, importX.headerRowType.getMessage());
                    }
                    for (Cell cell : (Row) importX.headerRowType.data) {
                        String v = Xlsx.getString(cell);
                        Xlsx.setCell(context, v);
                        v = v == null ? "-" : v.toLowerCase();
                        if (v.contains("int") || v.contains("number") || v.contains("long") || v.contains("double")
                            || v.contains("float")) {
                            cellTypes.put(cell.getColumnIndex(), Xlsx.CellType.NUMBER);
                        } else if (v.contains("yyyy-mm-dd") || v.contains("date") || v.contains("time")) {
                            cellTypes.put(cell.getColumnIndex(), Xlsx.CellType.DATETIME);
                        } else {
                            cellTypes.put(cell.getColumnIndex(), Xlsx.CellType.STRING);
                        }
                    }
                }

                for (ImportData.Row row : importX.rows) {
                    context.nextRow();

                    XlsxStyledBase.setImportResult(context, row.getColor());
                    Xlsx.setCell(context, row.getMessage());
                    context.setRowHeight(17 * Math.max(1, 1 + StringUtil.countMatches(row.getMessage(), Xlsx.LINE_SEPARATOR_WRITE)));

                    XlsxStyledBase.setNormalCenter(context);
                    for (Cell cell : (Row) row.data) {
                        if (importX.hasResultCol && cell.getColumnIndex() == 0) {
                            continue;
                        }
                        Xlsx.CellType type;
                        if (cell == null) {
                            type = Xlsx.CellType.STRING;
                        } else {
                            type = cellTypes.get(cell.getColumnIndex());
                            if (type == null) {
                                type = Xlsx.CellType.STRING;
                            }
                        }
                        String value = Xlsx.getStringWrite(cell, type);
                        if (StringUtil.isNotEmpty(value) && Xlsx.CellType.DATETIME.equals(type)) {
                            value = "\"" + value + "\"";
                        }
                        Xlsx.setCell(context, value);
                    }
                }
            });
        }
        Xlsx.create(wXlsx);

        // write result < < <

        String elapsed = DateTimeFormatter.secondsToDateTime((System.currentTimeMillis() - t) / 1000);
        ImportData result = ImportData.summarize(imports, IMPORT_PATH + filename, elapsed);
        FileUtil.removeFile(filepath);
        FileUtil.move(resultFilepath, filepath);

        log.info("import end ({}) <---\n\n", elapsed);
        ServiceLog.addAction("IMPORT", title);

        return ResponseMessage.success(VantarKey.SUCCESS_UPLOAD, result);
    }


    public interface Event {

        boolean onReadRow(Row row, ImportData importData) throws Exception;
    }
}
