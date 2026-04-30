package com.kfarms.reports;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;

@Slf4j
public class PdfReportBuilder {

    private static final Color KFARMS_PURPLE = new Color(107, 70, 193); // 💜 Purple
    private static final List<String> FONT_CANDIDATES = List.of(
            "fonts/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    );

    public static <T> byte[] buildReport(List<T> dataList, String title, List<ReportColumn<T>> columns) throws Exception {
        return buildReport(dataList, title, columns, null);
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
            List<ReportColumn<T>> columns,
            ReportBranding branding
    ) throws Exception {

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Try to use system font if available; fallback to Helvetica safely
            BaseFont base = resolveBaseFont();

            Color titleColor = resolveColor(branding != null ? branding.primaryColor() : null, KFARMS_PURPLE);
            Color headerColor = resolveColor(branding != null ? branding.accentColor() : null, KFARMS_PURPLE);
            String organizationName = sanitizeText(branding != null ? branding.organizationName() : null);
            String footerText = sanitizeText(branding != null ? branding.footerText() : null);

            Font titleFont = new Font(base, 18, Font.BOLD, titleColor);
            Font headerFont = new Font(base, 11, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(base, 10, Font.NORMAL, Color.BLACK);
            Font footerFont = new Font(base, 9, Font.ITALIC, Color.GRAY);

            log.debug("Building PDF report '{}' with {} header(s) and {} record(s)",
                    title,
                    columns.size(),
                    dataList != null ? dataList.size() : 0);

            // ---------- TITLE ----------
            String resolvedTitle = organizationName.isBlank() ? title : organizationName + " • " + title;
            Paragraph titlePara = new Paragraph(resolvedTitle, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // ---------- TABLE ----------
            PdfPTable table = new PdfPTable(columns.size());
            table.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            table.setLockedWidth(true);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setWidths(resolveColumnWidths(columns));

            // ---------- HEADER ROW ----------
            for (ReportColumn<T> column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.header(), headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(headerColor);
                cell.setPadding(6f);
                cell.setUseAscender(true);
                cell.setUseDescender(true);
                table.addCell(cell);
            }

            // ---------- DATA ROWS ----------
            if (dataList != null && !dataList.isEmpty()) {
                for (T obj : dataList) {
                    for (ReportColumn<T> column : columns) {
                        PdfPCell cell;
                        try {
                            String text = column.formattedValueFor(obj);
                            if (text == null || text.isBlank()) {
                                text = "-";
                            }
                            cell = new PdfPCell(new Phrase(text, cellFont));
                        } catch (Exception ex) {
                            cell = new PdfPCell(new Phrase("ERR", cellFont));
                        }
                        int cellAlignment = resolveCellAlignment(column);
                        cell.setHorizontalAlignment(cellAlignment);
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        cell.setPadding(5f);
                        cell.setUseAscender(true);
                        cell.setUseDescender(true);
                        cell.setNoWrap(column.valueType() != ReportValueType.TEXT);
                        table.addCell(cell);
                    } 
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No records found", cellFont));
                emptyCell.setColspan(columns.size());
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

    private static <T> float[] resolveColumnWidths(List<ReportColumn<T>> columns) {
        float[] widths = new float[columns.size()];
        for (int index = 0; index < columns.size(); index++) {
            widths[index] = resolveColumnWidth(columns.get(index));
        }
        return widths;
    }

    private static int resolveCellAlignment(ReportColumn<?> column) {
        return switch (column.valueType()) {
            case DATE -> Element.ALIGN_CENTER;
            case MONEY -> Element.ALIGN_RIGHT;
            case TEXT -> isNumericHeader(column.header()) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
        };
    }

    private static float resolveColumnWidth(ReportColumn<?> column) {
        String header = sanitizeText(column.header()).toLowerCase(Locale.ROOT);

        if (header.contains("note")) return 1.5f;
        if (header.contains("item") || header.contains("batch") || header.contains("feed name")
                || header.contains("flock") || header.contains("pond") || header.contains("location")) {
            return 1.45f;
        }
        if (header.contains("supplier") || header.contains("buyer") || header.contains("status")
                || header.contains("category") || header.contains("source") || header.contains("type")) {
            return 1.15f;
        }
        if (column.valueType() == ReportValueType.DATE) return 1.35f;
        if (column.valueType() == ReportValueType.MONEY) return 1.25f;
        if (isNumericHeader(header)) return 0.92f;
        return 1f;
    }

    private static boolean isNumericHeader(String header) {
        String normalized = sanitizeText(header).toLowerCase(Locale.ROOT);
        return normalized.contains("quantity")
                || normalized.contains("price")
                || normalized.contains("cost")
                || normalized.contains("stock")
                || normalized.contains("mortality")
                || normalized.contains("crates")
                || normalized.contains("count")
                || normalized.contains("rate")
                || normalized.contains("age")
                || normalized.contains("alive")
                || normalized.contains("good eggs")
                || normalized.contains("cracked eggs");
    }

    private static BaseFont resolveBaseFont() throws Exception {
        try (InputStream stream = PdfReportBuilder.class.getClassLoader().getResourceAsStream("fonts/DejaVuSans.ttf")) {
            if (stream != null) {
                return BaseFont.createFont(
                        "DejaVuSans.ttf",
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED,
                        BaseFont.CACHED,
                        stream.readAllBytes(),
                        null
                );
            }
        }

        for (String candidate : FONT_CANDIDATES) {
            if (Files.exists(Path.of(candidate))) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }
}
