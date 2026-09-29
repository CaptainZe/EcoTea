package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStock;
import com.appsinnova.admin.common.utils.DictUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 存货盘点表 Excel 导出（样式参考常见盘点表：黑底标题 + 单行说明 + 浅灰表头）。
 */
public final class ChaiStockExcelExporter {

    private static final String SHEET_TITLE = "存货盘点表";

    private static final String[] HEADERS = {
            "SKU编码", "品牌", "品名", "规格", "年", "批次",
            "本仓件数", "无提袋", "破损", "破损无袋"
    };

    /** 列宽（字符约宽），索引对应 HEADERS */
    private static final int[] COLUMN_WIDTHS = {
            24, 14, 36, 24, 8, 12,
            12, 10, 10, 12
    };

    private ChaiStockExcelExporter() {
    }

    public static void export(HttpServletResponse response,
                              String brandName,
                              String warehouseName,
                              Date exportTime,
                              List<ChaiStock> rows) throws IOException {
        String brand = StringUtils.hasText(brandName) ? brandName : "-";
        String warehouse = StringUtils.hasText(warehouseName) ? warehouseName : "-";
        Date when = exportTime != null ? exportTime : new Date();
        SimpleDateFormat dateShowFmt = new SimpleDateFormat("yyyy.MM.dd");
        SimpleDateFormat fileFmt = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateText = dateShowFmt.format(when);

        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet(SHEET_TITLE);
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

            // 标题：黑底白字
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            XSSFFont titleFont = workbook.createFont();
            titleFont.setFontName("Microsoft YaHei UI");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);

            // 说明行：白底黑字
            XSSFCellStyle metaLeftStyle = workbook.createCellStyle();
            metaLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            metaLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            metaLeftStyle.setIndention((short) 1);
            XSSFFont metaFont = workbook.createFont();
            metaFont.setFontName("Microsoft YaHei UI");
            metaFont.setFontHeightInPoints((short) 11);
            metaFont.setColor(IndexedColors.BLACK.getIndex());
            metaLeftStyle.setFont(metaFont);

            XSSFCellStyle metaRightStyle = workbook.createCellStyle();
            metaRightStyle.cloneStyleFrom(metaLeftStyle);
            metaRightStyle.setAlignment(HorizontalAlignment.RIGHT);
            metaRightStyle.setIndention((short) 1);

            // 表头：浅灰底黑字 + 边框
            XSSFCellStyle thStyle = workbook.createCellStyle();
            thStyle.setBorderTop(BorderStyle.THIN);
            thStyle.setBorderLeft(BorderStyle.THIN);
            thStyle.setBorderRight(BorderStyle.THIN);
            thStyle.setBorderBottom(BorderStyle.THIN);
            thStyle.setAlignment(HorizontalAlignment.CENTER);
            thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            thStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            XSSFFont thFont = workbook.createFont();
            thFont.setFontName("Microsoft YaHei UI");
            thFont.setBold(true);
            thFont.setFontHeightInPoints((short) 11);
            thFont.setColor(IndexedColors.BLACK.getIndex());
            thStyle.setFont(thFont);

            XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setWrapText(true);
            XSSFFont cellFont = workbook.createFont();
            cellFont.setFontName("Microsoft YaHei UI");
            cellFont.setFontHeightInPoints((short) 10);
            cellStyle.setFont(cellFont);

            XSSFCellStyle numStyle = workbook.createCellStyle();
            numStyle.cloneStyleFrom(cellStyle);
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            numStyle.setWrapText(false);

            // 第 1 行：标题
            XSSFRow titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            for (int i = 0; i < HEADERS.length; i++) {
                XSSFCell cell = titleRow.createCell(i);
                if (i == 0) {
                    cell.setCellValue(SHEET_TITLE);
                }
                cell.setCellStyle(titleStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // 第 2 行：品牌/仓库（左）+ 截止日期（右）
            XSSFRow metaRow = sheet.createRow(1);
            metaRow.setHeightInPoints(20);
            int mid = HEADERS.length / 2;
            for (int i = 0; i < HEADERS.length; i++) {
                XSSFCell cell = metaRow.createCell(i);
                if (i == 0) {
                    cell.setCellValue("品牌：" + brand + "　　仓库：" + warehouse);
                    cell.setCellStyle(metaLeftStyle);
                } else if (i < mid) {
                    cell.setCellStyle(metaLeftStyle);
                } else if (i == mid) {
                    cell.setCellValue("截止日期：" + dateText);
                    cell.setCellStyle(metaRightStyle);
                } else {
                    cell.setCellStyle(metaRightStyle);
                }
            }
            if (mid > 0) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, mid - 1));
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, mid, HEADERS.length - 1));

            // 第 3 行：表头
            XSSFRow thRow = sheet.createRow(2);
            thRow.setHeightInPoints(22);
            for (int i = 0; i < HEADERS.length; i++) {
                XSSFCell th = thRow.createCell(i);
                th.setCellValue(HEADERS[i]);
                th.setCellStyle(thStyle);
            }

            int rowIdx = 3;
            if (rows != null) {
                for (ChaiStock stock : rows) {
                    XSSFRow row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(18);
                    String skuCode = stock.getSkuCode() != null ? stock.getSkuCode() : "";
                    String name = stock.getName() != null ? stock.getName() : "";
                    String spec = stock.getSpecShow() != null ? stock.getSpecShow() : "";
                    String brandCell = stock.getBrandName() != null ? stock.getBrandName() : brand;
                    Integer year = stock.getYear();
                    String batch = "";
                    if (stock.getProdBatch() != null) {
                        String dictVal = DictUtils.keyValue("CHAI_PROD_BATCH", String.valueOf(stock.getProdBatch()));
                        batch = StringUtils.hasText(dictVal) ? dictVal : String.valueOf(stock.getProdBatch());
                    }
                    writeText(row, 0, skuCode, cellStyle);
                    writeText(row, 1, brandCell, cellStyle);
                    writeText(row, 2, name, cellStyle);
                    writeText(row, 3, spec, cellStyle);
                    writeText(row, 4, year != null ? String.valueOf(year) : "", cellStyle);
                    writeText(row, 5, batch, cellStyle);
                    writeNumber(row, 6, stock.getListQty(), numStyle);
                    writeNumber(row, 7, stock.getListQtyNoBag(), numStyle);
                    writeNumber(row, 8, stock.getListQtyDamaged(), numStyle);
                    writeNumber(row, 9, stock.getListQtyDamagedNoBag(), numStyle);
                }
            }

            String fileName = URLEncoder.encode(
                    SHEET_TITLE + "_" + brand + "_" + warehouse + "_" + fileFmt.format(new Date()) + ".xlsx",
                    StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
        } finally {
            workbook.close();
        }
    }

    private static void writeText(XSSFRow row, int col, String value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static void writeNumber(XSSFRow row, int col, Integer value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : 0);
        cell.setCellStyle(style);
    }
}
