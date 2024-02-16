package com.vantar.util.xlsx;

import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.dhatim.fastexcel.*;
import org.dhatim.fastexcel.reader.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;


public class Xlsx {

    public static String FILE_UPLOAD = "file";

    private static final Logger log = LoggerFactory.getLogger(Xlsx.class);
    private static final int PERSIAN_DAY_OFFSET = 9;

    public static final char LINE_SEPARATOR_READ = '\n';
    public static final char LINE_SEPARATOR_WRITE = '\r';


    public static void create(Config config) throws VantarException {
        if (config.response != null) {
            Response.setDownloadHeaders(config.response, config.filename);
        }

        try (
            OutputStream outputStream = config.response == null ?
                new FileOutputStream(config.filename) : config.response.getOutputStream();
            Workbook workbook = new Workbook(outputStream, config.title, "1.0")
        ) {
            config.iterating = true;
            for (Map.Entry<String, WriteEvents> entry : config.writeSheets.entrySet()) {
                String title = entry.getKey();
                WriteEvents e = entry.getValue();
                config.context.reset();
                config.context.workbook = workbook;
                config.context.sheet = workbook.newWorksheet(title == null ? "" : title);
                e.onCreateSheet(config.context);
            }
            for (Map.Entry<String, WriteEvents> entry : config.writeSheetsSecondary.entrySet()) {
                String title = entry.getKey();
                WriteEvents e = entry.getValue();
                config.context.reset();
                config.context.workbook = workbook;
                config.context.sheet = workbook.newWorksheet(title);
                e.onCreateSheet(config.context);
            }
        } catch (Exception e) {
            log.error(" ! create xlsx {}", config.filename, e);
            throw new ServerException(VantarKey.IO_ERROR);
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static String read(Config config) throws VantarException {
        String filename;
        String filepath;
        if (config.params == null) {
            filename = config.filename;
            filepath = config.filename;
        } else {
            try (Params.Uploaded uploaded = config.params.upload(FILE_UPLOAD)) {
                if (!uploaded.isUploadedOk()) {
                    throw new InputException(uploaded.getError(), FILE_UPLOAD);
                }
                if (!uploaded.isType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                    throw new InputException(VantarKey.FILE_TYPE, FILE_UPLOAD, "xlsx");
                }

                filename = config.filename == null ?
                    StringUtil.replace(
                        uploaded.getOriginalFilename(),
                        ".xlsx", "-" + new DateTime().formatter().getDateTimePersianAsFilename() + ".xlsx"
                    ) :
                    config.filename;

                config.filename = filename;

                if (!uploaded.moveTo(config.uploadDir, filename)) {
                    throw new ServerException(VantarKey.UPLOAD_FAIL);
                }
                filepath = config.uploadDir + filename;
            }
        }

        try (InputStream inputStream = new FileInputStream(filepath);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {

            for (Map.Entry<Integer, ReadEvents> entry : config.readSheets.entrySet()) {
                Integer i = entry.getKey();
                ReadEvents e = entry.getValue();

                Sheet sheet;
                try {
                    sheet = workbook.getSheet(i).get();
                    config.title = sheet.getName();
                } catch (Exception sheetException) {
                    e.onError(workbook, sheetException, false, i);
                    return filename;
                }

                try (Stream<Row> rows = sheet.openStream()) {
                    rows.forEach(row -> {
                        if (!(row.getRowNum() > config.headerRowcount ?
                            e.onReadRow(workbook, row) :
                            e.onReadHeader(workbook, row))) {

                            // if returns false, interrupt
                            log.error(" ! EndRowReadException (handler interrupt) row={}", row.getRowNum());
                            throw new EndRowReadException();
                        }
                    });
                } catch (RuntimeException x) {
                    e.onError(workbook, x, true, i);
                }
            }
        } catch (Exception e) {
            throw new ServerException(e);
        }
        return filename;
    }


    public static class Config {

        public HttpServletResponse response;
        public Params params;
        public String title;
        public String uploadDir;
        public String filename;
        public int headerRowcount;
        public Map<String, WriteEvents> writeSheets = new LinkedHashMap<>(7, 1);
        public Map<String, WriteEvents> writeSheetsSecondary = new LinkedHashMap<>(7, 1);
        public Map<Integer, ReadEvents> readSheets = new LinkedHashMap<>(7, 1);
        public Context context = new Context();
        protected boolean iterating = false;


        public Config() {

        }

        public Config(HttpServletResponse response, String filename, String title) {
            this.response = response;
            this.filename = filename;
            this.title = title;
        }

        public Config(String filename, String title) {
            this.title = title;
            this.filename = filename;
        }

        public void addWriteEvent(String title, WriteEvents e) {
            if (iterating) {
                writeSheetsSecondary.put(title, e);
            } else {
                writeSheets.put(title, e);
            }
        }

        public void addReadEvent(int index, ReadEvents e) {
            readSheets.put(index, e);
        }
    }


    public interface WriteEvents {

        void onCreateSheet(Context context) throws Exception;
    }


    public interface ReadEvents {

        boolean onReadHeader(ReadableWorkbook workbook, Row row);

        boolean onReadRow(ReadableWorkbook workbook, Row row);

        void onError(ReadableWorkbook workbook, Exception e, boolean rowError, int sheetIndex);
    }


    public static class EndRowReadException extends RuntimeException {

    }


    public static class Context {

        public Workbook workbook;
        public Worksheet sheet;
        public int rowIndex;
        public int colIndex;
        public Style style;

        public void reset() {
            rowIndex = 0;
            colIndex = 0;
            style = new Style();
        }

        public void nextRow() {
            ++rowIndex;
            colIndex = 0;
        }

        public void nextRow(int jumpToColIndex) {
            ++rowIndex;
            colIndex = jumpToColIndex;
        }

        //   0 1 2 3 4 5 6 7 8
        //   1 - - - - - - - -
        //   2 - - X - - - - -
        //   3 - - X - - - - -
        //   4 - - X - - - - -
        //   5 - - - - - - - -
        //   (3, 2, 3, 4)
        public void mergeCells(int x1, int y1, int x2, int y2) {
            try {
                sheet.range(x1, y1, x2, y2).merge();
            } catch (IllegalArgumentException e) {
                log.error("! merge x1={}, y1={}, x2={}, y2={}", x1, y1, x2, y2);
            }
        }

        public void setRowHeight(int h) {
            try {
                sheet.rowHeight(rowIndex, h);
            } catch (Exception e) {
                log.warn(" ! row={}", rowIndex, e);
            }
        }

        public void setRowWidth(int w) {
            try {
                sheet.width(rowIndex, w);
            } catch (Exception e) {
                log.warn(" ! row={}", rowIndex, e);
            }
        }
    }


    public static class Style {

        public String horizontalAlignment;
        public String verticalAlignment;
        public String fontName;
        public int fontSize;
        public boolean bold;
        public String color;
        public String bgColor;
        public String borderColor;
    }


    /**
     *  > > > SET cell / row
     */

    public static void setCell(Context context, Object... values) {
        for (Object value : values) {
            context.sheet.value(context.rowIndex, context.colIndex, value == null ? "" : ObjectUtil.toString(value));
            StyleSetter styleSetter = context.sheet.style(context.rowIndex, context.colIndex);
            styleSetter
                .wrapText(true)
                .horizontalAlignment(context.style.horizontalAlignment)
                .verticalAlignment(context.style.verticalAlignment)
                .borderStyle(BorderStyle.THIN)
                .fontName(context.style.fontName)
                .fontSize(context.style.fontSize)
                .fontColor(context.style.color)
                .fillColor(context.style.bgColor)
                .borderColor(context.style.borderColor)
                .set();
            if (context.style.bold) {
                styleSetter.bold();
            }
            ++context.colIndex;
        }
    }

    public static void setCellColorWidth(Context context, String bgColor, double width, Object... values) {
        for (Object value : values) {
            context.sheet.value(context.rowIndex, context.colIndex, value == null ? "" : ObjectUtil.toString(value));
            StyleSetter styleSetter = context.sheet.style(context.rowIndex, context.colIndex);
            styleSetter
                .wrapText(true)
                .horizontalAlignment(context.style.horizontalAlignment)
                .verticalAlignment(context.style.verticalAlignment)
                .borderStyle(BorderStyle.THIN)
                .fontName(context.style.fontName)
                .fontSize(context.style.fontSize)
                .fontColor(context.style.color)
                .fillColor(bgColor)
                .borderColor(context.style.borderColor)
                .set();
            if (context.style.bold) {
                styleSetter.bold();
            }
            context.sheet.width(context.colIndex, width);
            ++context.colIndex;
        }
    }

    public static void setCellWidth(Context context, double width, Object... values) {
        for (Object value : values) {
            context.sheet.value(context.rowIndex, context.colIndex, value == null ? "" : ObjectUtil.toString(value));
            StyleSetter styleSetter = context.sheet.style(context.rowIndex, context.colIndex);
            styleSetter
                .wrapText(true)
                .horizontalAlignment(context.style.horizontalAlignment)
                .verticalAlignment(context.style.verticalAlignment)
                .borderStyle(BorderStyle.THIN)
                .fontName(context.style.fontName)
                .fontSize(context.style.fontSize)
                .fontColor(context.style.color)
                .fillColor(context.style.bgColor)
                .borderColor(context.style.borderColor)
                .set();
            if (context.style.bold) {
                styleSetter.bold();
            }
            context.sheet.width(context.colIndex, width);
            ++context.colIndex;
        }
    }

    public static void setCellBg(Context context, String bgColor, Object... values) {
        for (Object value : values) {
            context.sheet.value(context.rowIndex, context.colIndex, value == null ? "" : ObjectUtil.toString(value));
            StyleSetter styleSetter = context.sheet.style(context.rowIndex, context.colIndex);
            styleSetter
                .wrapText(true)
                .horizontalAlignment(context.style.horizontalAlignment)
                .verticalAlignment(context.style.verticalAlignment)
                .borderStyle(BorderStyle.THIN)
                .fontName(context.style.fontName)
                .fontSize(context.style.fontSize)
                .fontColor(context.style.color)
                .fillColor(bgColor)
                .borderColor(context.style.borderColor)
                .set();
            if (context.style.bold) {
                styleSetter.bold();
            }
            ++context.colIndex;
        }
    }

    public static void setCellBgFg(Context context, String bgColor, String fgColor, Object... values) {
        for (Object value : values) {
            context.sheet.value(context.rowIndex, context.colIndex, value == null ? "" : ObjectUtil.toString(value));
            StyleSetter styleSetter = context.sheet.style(context.rowIndex, context.colIndex);
            styleSetter
                .wrapText(true)
                .horizontalAlignment(context.style.horizontalAlignment)
                .verticalAlignment(context.style.verticalAlignment)
                .borderStyle(BorderStyle.THIN)
                .fontName(context.style.fontName)
                .fontSize(context.style.fontSize)
                .fontColor(fgColor)
                .fillColor(bgColor)
                .borderColor(context.style.borderColor)
                .set();
            if (context.style.bold) {
                styleSetter.bold();
            }
            ++context.colIndex;
        }
    }

    /**
     *  > > > GET cell / row
     */
    public static String getStringWrite(Row row, int i) {
        return StringUtil.replace(getString(row, i, CellType.STRING), LINE_SEPARATOR_READ, LINE_SEPARATOR_WRITE);
    }

    public static String getStringWrite(Row row, int i, CellType type) {
        return StringUtil.replace(getString(row, i, type), LINE_SEPARATOR_READ, LINE_SEPARATOR_WRITE);
    }

    public static String getStringWrite(Cell cell) {
        return StringUtil.replace(getString(cell, CellType.STRING), LINE_SEPARATOR_READ, LINE_SEPARATOR_WRITE);
    }

    public static String getStringWrite(Cell cell, CellType type) {
        return StringUtil.replace(getString(cell, type), LINE_SEPARATOR_READ, LINE_SEPARATOR_WRITE);
    }

    public static String getString(Row row, int i) {
        return getString(row, i, CellType.STRING);
    }

    public static String getString(Row row, int i, CellType type) {
        try {
            return getString(row.getCell(i), type);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getString(Cell cell) {
        return getString(cell, CellType.STRING);
    }

    public static String getString(Cell cell, CellType type) {
        if (cell == null) {
            return null;
        }
        Object o = cell.getValue();
        if (o == null) {
            return null;
        }

        if (CellType.DATETIME.equals(type)) {
            DateTime dt = tryGetDate(cell);
            if (dt != null) {
                return (dt.formatter().hour == 0 && dt.formatter().minute == 0)
                    ? dt.formatter().getDate() : dt.formatter().getDateTime();
            }
        }

        String s = ObjectUtil.toString(o).trim();
        s = s.equalsIgnoreCase("null") ? null : s;
        s = StringUtil.replace(
            s,
            new String[] {"_x000D_", "_x000d_", "x000D", "x000d", "-->", "___", "\n", "\r"},
            "" + LINE_SEPARATOR_READ
        );
        s = StringUtil.replace(s, "" + LINE_SEPARATOR_READ + LINE_SEPARATOR_READ, "" + LINE_SEPARATOR_READ);
        s = StringUtil.trim(s, '\'', '"', '\"', LINE_SEPARATOR_READ, '\t', ' ');
        return StringUtil.isEmpty(s) ? null : s;
    }

    public static DateTime getDate(Cell cell) {
        if (cell == null || cell.getValue() == null) {
            return null;
        }
        DateTime dt = tryGetDate(cell);
        if (dt != null) {
            return dt;
        }
        String stringVal = getString(cell, CellType.STRING);
        if (stringVal == null) {
            return null;
        }
        try {
            return new DateTime(stringVal).truncateTime();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static DateTime tryGetDate(Cell cell) {
        DateTime dateTime;
        try {
            LocalDateTime localDateTime = cell.asDate();
            int y = localDateTime.getYear();
            if (y > 1900 && y < 2050) {
                dateTime = new DateTime(localDateTime.toString());
            } else if (y > 1390 && y < 1420) {
                dateTime = new DateTime(localDateTime.toString()).decreaseDays(PERSIAN_DAY_OFFSET);
            } else {
                return null;
            }
            dateTime.truncateTime();
            return dateTime;
        } catch (Exception ignore) {
            return null;
        }
    }


    public enum CellType {
        NUMBER,
        STRING,
        DATETIME,
    }
}

