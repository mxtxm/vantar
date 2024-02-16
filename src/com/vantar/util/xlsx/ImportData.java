package com.vantar.util.xlsx;

import com.vantar.service.log.ServiceLog;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import java.util.*;


public class ImportData {

    public static final String IMPORT_RESULT_HEADER_VALUE = "import result";
    public static final String RESULT_MESSAGE_SUCCESS_INSERT = "inserted!";
    public static final String RESULT_MESSAGE_SUCCESS_UPDATE = "updated!";
    public static final String RESULT_MESSAGE_SUCCESS_DELETED = "deleted!";
    public static final String RESULT_MESSAGE_NO_ACTION = "no action (already up to date)!";

    public Boolean hasResultCol = false;
    public String title;

    public int failedCount = 0;
    public int insertedCount = 0;
    public int updatedCount = 0;
    public int deleteCount = 0;
    public int previousCount = 0;
    public int totalCount = 0;
    public String elapsed;
    public String resultUrl;
    public Row headerRow;
    public Row headerRowType;
    public List<Row> rows = new ArrayList<>();


    public int getColIndex(int i) {
        return hasResultCol ? i + 1 : i;
    }

    public void setHeader(Object data, String firstCol) {
        if (headerRow == null) {
            headerRow = new Row();
            headerRow.data = data;
            headerRow.setMessage(IMPORT_RESULT_HEADER_VALUE);
            hasResultCol = IMPORT_RESULT_HEADER_VALUE.equalsIgnoreCase(firstCol);
        } else {
            headerRowType = new Row();
            headerRowType.data = data;
            headerRowType.setMessage("message");
        }
    }

    public boolean successPrevious(Object data, String previousMessage) {
        if (!hasResultCol) {
            return false;
        }
        if (!RESULT_MESSAGE_SUCCESS_INSERT.equalsIgnoreCase(previousMessage)
            && !RESULT_MESSAGE_SUCCESS_UPDATE.equalsIgnoreCase(previousMessage)
            && !RESULT_MESSAGE_SUCCESS_DELETED.equalsIgnoreCase(previousMessage)
            && !RESULT_MESSAGE_NO_ACTION.equalsIgnoreCase(previousMessage)
        ) {
            return false;
        }
        ++previousCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.setMessage(previousMessage);
        r.previousSuccess = true;
        rows.add(r);
        return true;
    }

    public void successInsert(Object data) {
        ++insertedCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.setMessage(RESULT_MESSAGE_SUCCESS_INSERT);
        rows.add(r);
    }

    public void successUpdate(Object data) {
        ++updatedCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.setMessage(RESULT_MESSAGE_SUCCESS_UPDATE);
        rows.add(r);
    }

    public void successDelete(Object data) {
        ++deleteCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.setMessage(RESULT_MESSAGE_SUCCESS_DELETED);
        rows.add(r);
    }

    public void neutral(Object data) {
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.setMessage(RESULT_MESSAGE_NO_ACTION);
        rows.add(r);
    }

    // > > > one fail per row

    public void fail(Object data, Exception e) {
        ServiceLog.log.error(" ! ", e);
        fail(data, "(system error) " + e.getMessage());
    }

    public void fail(Object data, String message) {
        ++failedCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.success = false;
        r.setMessage(message);
        rows.add(r);
    }

    // > > > many fails per row

    public void appendFail(Object data, Exception e) {
        ServiceLog.log.error(" ! ", e);
        appendFail(data, "(system error) " + e.getMessage());
    }

    public void appendFail(Object data, String message) {
        ++failedCount;
        ++totalCount;
        Row r = new Row();
        r.data = data;
        r.success = false;
        r.appendMessage(message);
        rows.add(r);
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }

    public static ImportData summarize(ImportData[] imports, String resultUrl, String elapsed) {
        ImportData result = imports[0];
        if (imports.length > 1) {
            for (int i = 1, l = imports.length; i < l; ++i) {
                ImportData importX = imports[i];
                result.failedCount += importX.failedCount;
                result.insertedCount += importX.insertedCount;
                result.updatedCount += importX.updatedCount;
                result.deleteCount += importX.deleteCount;
                result.previousCount += importX.previousCount;
                result.totalCount += importX.totalCount;
            }
        }
        result.resultUrl = resultUrl;
        result.hasResultCol = null;
        result.title = null;
        result.headerRow = null;
        result.headerRowType = null;
        result.rows = null;
        result.elapsed = elapsed;
        return result;
    }


    public static class Row {

        // row is updated/inserted/deleted successfully in a previous import
        public boolean previousSuccess = false;
        public boolean success = true;
        public Object data;
        private StringBuilder messageBuffer;
        private String message = getMessage();


        public void setMessage(String message) {
            this.message = StringUtil.replace(message, Xlsx.LINE_SEPARATOR_READ, Xlsx.LINE_SEPARATOR_WRITE);
        }

        public void appendMessage(String message) {
            if (messageBuffer == null) {
                messageBuffer = new StringBuilder(1000);
            }
            messageBuffer
                .append(
                    StringUtil.replace(
                        message,
                        Xlsx.LINE_SEPARATOR_READ,
                        Xlsx.LINE_SEPARATOR_WRITE
                    )
                ).append(Xlsx.LINE_SEPARATOR_WRITE);
        }

        public String getMessage() {
            if (message == null) {
                if (messageBuffer == null || messageBuffer.length() < 4) {
                    return  "";
                }
                messageBuffer.setLength(messageBuffer.length() - 2);
                message = messageBuffer.toString();
            }
            return message;
        }

        public String getColor() {
            getMessage();
            if (previousSuccess) {
                return "EEEEEE";
            }
            if (!success) {
                return "FFCCCC";
            }
            if (RESULT_MESSAGE_SUCCESS_INSERT.equals(message)) {
                return "CDFFF9";
            }
            if (RESULT_MESSAGE_SUCCESS_UPDATE.equals(message)) {
                return "CDFFD5";
            }
            return "FFFDCD";
        }

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }
}