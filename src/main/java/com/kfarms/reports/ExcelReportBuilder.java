package com.kfarms.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ExcelReportBuilder {

    /**
     * Builds an Excel workbook from a list of entities and headers.
     * Generic and reusable for any report type.
     */
    public static byte[] buildWorkbook(List<?> dataList, String sheetName, List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("Headers cannot be null or empty");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setDefaultColumnWidth(20);

            // Header style (bold + purple accent 💜)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.VIOLET.getIndex()); // Purple touch
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);


            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Populate data rows
            if (dataList != null && !dataList.isEmpty()) {
                int rowIdx = 1;
                for (Object obj : dataList) {
                    Row row = sheet.createRow(rowIdx++);
                    Field[] fields = getAllFields(obj.getClass());

                    for (int col = 0; col < fields.length && col < headers.size(); col++) {
                        fields[col].setAccessible(true);
                        Object value = fields[col].get(obj);
                        row.createCell(col).setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error building Excel report for sheet: " + sheetName, e);
        }
    }

    private static Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(List.of(c.getDeclaredFields()));
        }
        return fields.toArray(new Field[0]);
    }
}
