package com.vantar.util.xlsx;


public class XlsxStyledBase {

    public static final String HEADER = "FF7700";
    public static final String TYPE_HEADER = "80C4FF";
    public static final String WHITE = "FFFFFF";


    public static void setDefault(Xlsx.Context context) {
        context.style.horizontalAlignment = "center";
        context.style.verticalAlignment = "center";
        context.style.fontName = "Arial";
        context.style.fontSize = 10;
        context.style.bold = false;
        context.style.color = "333333";
        context.style.bgColor = "ffffff";
        context.style.borderColor = "777777";
    }

    public static void setHeader(Xlsx.Context context) {
        context.colIndex = 0;
        context.rowIndex = 0;
        setDefault(context);
        context.style.bgColor = HEADER;
        context.style.bold = true;
    }

    public static void setHeaderType(Xlsx.Context context) {
        context.colIndex = 0;
        context.rowIndex = 1;
        setDefault(context);
        context.style.bgColor = TYPE_HEADER;
        context.style.bold = true;
    }

    public static void setNormalCenter(Xlsx.Context context) {
        setDefault(context);
        context.style.bgColor = WHITE;
    }

    public static void setNormalCenterBold(Xlsx.Context context) {
        setDefault(context);
        context.style.bgColor = WHITE;
        context.style.bold = true;
    }

    public static void setNormalLeft(Xlsx.Context context) {
        setDefault(context);
        context.style.bgColor = WHITE;
        context.style.horizontalAlignment = "left";
    }

    public static void setNormalRight(Xlsx.Context context) {
        setDefault(context);
        context.style.bgColor = WHITE;
        context.style.horizontalAlignment = "right"
        ;
    }

    public static void setHeaderImportResult(Xlsx.Context context) {
        context.colIndex = 0;
        context.rowIndex = 0;
        setDefault(context);
        context.style.bgColor = HEADER;
        context.style.bold = true;
        context.style.fontSize = 8;
        context.sheet.fitToWidth((short) 1);
        context.sheet.freezePane(2, 1);
        context.sheet.width(0, 100);
        context.sheet.rowHeight(0, 17 * 3);
    }

    public static void setImportResult(Xlsx.Context context, String color) {
        setDefault(context);
        context.style.bgColor = color;
        context.style.horizontalAlignment = "left";
        context.style.fontSize = 9;
    }
}
