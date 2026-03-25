package com.kfarms.reports;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PdfReportBuilder {

    private static final Color KFARMS_PURPLE = new Color(107, 70, 193); // 💜 Purple

    public static <T> byte[] buildReport(List<T> dataList, String title, List<String> headers) throws Exception {
        return buildReport(dataList, title, headers, null);
    }

    public record ReportBranding(
            String organizationName,
            String primaryColor,
            String accentColor,
            String footerText
    ) {}

    public static <T> byte[] buildReport(
            List<T> dataList,
            String title,
            List<String> headers,
            ReportBranding branding
    ) throws Exception {

        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("Headers cannot be null or empty");
        }

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Try to use system font if available; fallback to Helvetica safely
            BaseFont base;
            try {
                base = BaseFont.createFont("fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            }

            Color titleColor = resolveColor(branding != null ? branding.primaryColor() : null, KFARMS_PURPLE);
            Color headerColor = resolveColor(branding != null ? branding.accentColor() : null, KFARMS_PURPLE);
            String organizationName = sanitizeText(branding != null ? branding.organizationName() : null);
            String footerText = sanitizeText(branding != null ? branding.footerText() : null);

            Font titleFont = new Font(base, 18, Font.BOLD, titleColor);
            Font headerFont = new Font(base, 12, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(base, 11, Font.NORMAL, Color.BLACK);
            Font footerFont = new Font(base, 9, Font.ITALIC, Color.GRAY);

            log.debug("Building PDF report '{}' with {} header(s) and {} record(s)",
                    title,
                    headers.size(),
                    dataList != null ? dataList.size() : 0);

            // ---------- TITLE ----------
            String resolvedTitle = organizationName.isBlank() ? title : organizationName + " • " + title;
            Paragraph titlePara = new Paragraph(resolvedTitle, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // ---------- TABLE ----------
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // ---------- HEADER ROW ----------
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(headerColor);
                cell.setPadding(6f);
                table.addCell(cell);
            }

            // ---------- DATA ROWS ----------
            if (dataList != null && !dataList.isEmpty()) {
                for (Object obj : dataList) {
                    Field[] fields = getAllFields(obj.getClass());
                    for (int i = 0; i < headers.size(); i++) {
                        PdfPCell cell;
                        try {
                            if (i < fields.length) {
                                fields[i].setAccessible(true);
                                Object value = fields[i].get(obj);
                                String text = (value != null) ? value.toString() : "-";
                                cell = new PdfPCell(new Phrase(text, cellFont));
                            } else {
                                cell = new PdfPCell(new Phrase("-", cellFont));
                            }
                        } catch (Exception ex) {
                            cell = new PdfPCell(new Phrase("ERR", cellFont));
                        }
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(5f);
                        table.addCell(cell);
                    } 
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No records found", cellFont));
                emptyCell.setColspan(headers.size());
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10f);
                table.addCell(emptyCell);
            }

            document.add(table);

            // ---------- FOOTER ----------
            String resolvedFooter = footerText.isBlank()
                    ? "Generated by KFarms for " + (organizationName.isBlank() ? "your farm" : organizationName)
                    : footerText;
            Paragraph footer = new Paragraph(
                    resolvedFooter + " • " + LocalDate.now(),
                    footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

        } catch (Exception e) {
            log.error("PDF generation failed for '{}'", title, e);
            throw e;
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    private static String sanitizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static Color resolveColor(String value, Color fallback) {
        String normalized = sanitizeText(value).replace("#", "");
        if (!normalized.matches("[0-9A-Fa-f]{6}")) {
            return fallback;
        }
        return Color.decode("#" + normalized);
    }


    private static Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(List.of(c.getDeclaredFields()));
        }
        return fields.toArray(new Field[0]);
    }
}
