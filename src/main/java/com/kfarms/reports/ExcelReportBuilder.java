package com.kfarms.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ExcelReportBuilder {

    /**
     * Builds an Excel workbook from a list of rows and explicit column definitions.
     */
    public static <T> byte[] buildWorkbook(List<T> dataList, String sheetName, List<ReportColumn<T>> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setDefaultColumnWidth(20);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            DataFormat dataFormat = workbook.createDataFormat();

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("dd mmm yyyy"));

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));


            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).header());
                cell.setCellStyle(headerStyle);
            }

            // Populate data rows
            if (dataList != null && !dataList.isEmpty()) {
                int rowIdx = 1;
                for (T obj : dataList) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int col = 0; col < columns.size(); col++) {
                        Cell cell = row.createCell(col);
                        ReportColumn<T> column = columns.get(col);
                        writeCellValue(
                                cell,
                                column.valueFor(obj),
                                column.valueType(),
                                dateStyle,
                                moneyStyle
                        );
                    }
                }
            }

            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error building Excel report for sheet: " + sheetName, e);
        }
    }

    private static void writeCellValue(
            Cell cell,
            Object value,
            ReportValueType valueType,
            CellStyle dateStyle,
            CellStyle moneyStyle
    ) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (valueType == ReportValueType.DATE) {
            if (value instanceof LocalDate localDate) {
                cell.setCellValue(java.sql.Date.valueOf(localDate));
                cell.setCellStyle(dateStyle);
                return;
            }

            if (value instanceof LocalDateTime localDateTime) {
                cell.setCellValue(Timestamp.valueOf(localDateTime));
                cell.setCellStyle(dateStyle);
                return;
            }

            cell.setCellValue(ReportValueFormatter.format(value, valueType));
            return;
        }

        if (valueType == ReportValueType.MONEY && value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(moneyStyle);
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }
}
