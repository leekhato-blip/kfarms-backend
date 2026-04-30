package com.kfarms.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportBuildersTest {

    private record SampleRow(LocalDate date, BigDecimal amount, BigDecimal total, int quantity) {
    }

    private record SuppliesPdfRow(
            LocalDate date,
            String item,
            String category,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String supplier,
            String note
    ) {
    }

    @Test
    void csvBuilderFormatsDatesAndMoneyForExport() throws Exception {
        List<ReportColumn<SampleRow>> columns = List.of(
                ReportColumn.of("Date", SampleRow::date, ReportValueType.DATE),
                ReportColumn.of("Amount", SampleRow::amount, ReportValueType.MONEY),
                ReportColumn.of("Quantity", SampleRow::quantity)
        );

        String csv = new String(
                CsvReportBuilder.buildReport(
                        List.of(new SampleRow(
                                LocalDate.of(2026, 4, 30),
                                new BigDecimal("5600"),
                                new BigDecimal("11200"),
                                7
                        )),
                        columns
                ).readAllBytes()
        );

        assertTrue(csv.contains("30 Apr 2026"));
        assertTrue(csv.contains("\"5,600.00\""));
    }

    @Test
    void excelBuilderKeepsDatesAndMoneyReadable() throws Exception {
        List<ReportColumn<SampleRow>> columns = List.of(
                ReportColumn.of("Date", SampleRow::date, ReportValueType.DATE),
                ReportColumn.of("Amount", SampleRow::amount, ReportValueType.MONEY)
        );

        byte[] workbookBytes = ExcelReportBuilder.buildWorkbook(
                List.of(new SampleRow(
                        LocalDate.of(2026, 4, 30),
                        new BigDecimal("5600"),
                        new BigDecimal("11200"),
                        0
                )),
                "Sample",
                columns
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            var sheet = workbook.getSheetAt(0);
            var dateCell = sheet.getRow(1).getCell(0);
            var moneyCell = sheet.getRow(1).getCell(1);

            assertTrue(DateUtil.isCellDateFormatted(dateCell));
            assertEquals("dd mmm yyyy", dateCell.getCellStyle().getDataFormatString().toLowerCase());
            assertEquals(5600.0d, moneyCell.getNumericCellValue(), 0.001d);
            assertEquals("#,##0.00", moneyCell.getCellStyle().getDataFormatString());
        }
    }

    @Test
    void pdfBuilderKeepsDatesAndMoneyReadable() throws Exception {
        List<ReportColumn<SampleRow>> columns = List.of(
                ReportColumn.of("Date", SampleRow::date, ReportValueType.DATE),
                ReportColumn.of("Unit Price", SampleRow::amount, ReportValueType.MONEY),
                ReportColumn.of("Total Price", SampleRow::total, ReportValueType.MONEY),
                ReportColumn.of("Quantity", SampleRow::quantity)
        );

        byte[] pdfBytes = PdfReportBuilder.buildReport(
                List.of(new SampleRow(
                        LocalDate.of(2026, 4, 30),
                        new BigDecimal("5600"),
                        new BigDecimal("11200"),
                        2
                )),
                "Sales Report",
                columns
        );

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("30 Apr 2026"));
            assertTrue(text.contains("5,600.00"));
            assertTrue(text.contains("11,200.00"));
            assertTrue(text.contains("Unit Price"));
            assertTrue(text.contains("Total Price"));
        }
    }

    @Test
    void pdfBuilderKeepsSuppliesColumnsSeparated() throws Exception {
        List<ReportColumn<SuppliesPdfRow>> columns = List.of(
                ReportColumn.of("Date", SuppliesPdfRow::date, ReportValueType.DATE),
                ReportColumn.of("Item", SuppliesPdfRow::item),
                ReportColumn.of("Category", SuppliesPdfRow::category),
                ReportColumn.of("Quantity", SuppliesPdfRow::quantity),
                ReportColumn.of("Unit Price", SuppliesPdfRow::unitPrice, ReportValueType.MONEY),
                ReportColumn.of("Total Price", SuppliesPdfRow::totalPrice, ReportValueType.MONEY),
                ReportColumn.of("Supplier", SuppliesPdfRow::supplier),
                ReportColumn.of("Note", SuppliesPdfRow::note)
        );

        byte[] pdfBytes = PdfReportBuilder.buildReport(
                List.of(new SuppliesPdfRow(
                        LocalDate.of(2026, 4, 4),
                        "Layer feed grower",
                        "FEED",
                        5,
                        new BigDecimal("250"),
                        new BigDecimal("1250"),
                        "grower",
                        "-"
                )),
                "Supplies Report",
                columns
        );

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("04 Apr 2026"), text);
            assertTrue(text.contains("250.00"), text);
            assertTrue(text.contains("1,250.00"), text);
            assertTrue(text.contains("Layer feed grower"), text);
        }
    }

    @Test
    void reportColumnSetsPreserveDateAndMoneyTypesAcrossExports() {
        assertColumnTypes(
                ReportColumnSets.SALES,
                expected("Date", ReportValueType.DATE),
                expected("Unit Price", ReportValueType.MONEY),
                expected("Total Price", ReportValueType.MONEY)
        );
        assertColumnTypes(
                ReportColumnSets.SUPPLIES,
                expected("Date", ReportValueType.DATE),
                expected("Unit Price", ReportValueType.MONEY),
                expected("Total Price", ReportValueType.MONEY)
        );
        assertColumnTypes(
                ReportColumnSets.FEEDS,
                expected("Date", ReportValueType.DATE),
                expected("Unit Cost", ReportValueType.MONEY)
        );
        assertColumnTypes(
                ReportColumnSets.INVENTORY,
                expected("Unit Cost", ReportValueType.MONEY),
                expected("Updated", ReportValueType.DATE)
        );
        assertColumnTypes(
                ReportColumnSets.LIVESTOCK,
                expected("Start Date", ReportValueType.DATE)
        );
        assertColumnTypes(
                ReportColumnSets.FISH_PONDS,
                expected("Date Stocked", ReportValueType.DATE),
                expected("Last Water Change", ReportValueType.DATE),
                expected("Next Water Change", ReportValueType.DATE)
        );
        assertColumnTypes(
                ReportColumnSets.FISH_HATCHES,
                expected("Hatch Date", ReportValueType.DATE)
        );
        assertColumnTypes(
                ReportColumnSets.EGGS,
                expected("Date", ReportValueType.DATE)
        );
    }

    private static ReportColumnExpectation expected(String header, ReportValueType valueType) {
        return new ReportColumnExpectation(header, valueType);
    }

    private static void assertColumnTypes(
            List<? extends ReportColumn<?>> columns,
            ReportColumnExpectation... expectations
    ) {
        for (ReportColumnExpectation expectation : expectations) {
            boolean matches = columns.stream().anyMatch(column ->
                    expectation.header().equals(column.header()) &&
                            expectation.valueType() == column.valueType()
            );
            assertTrue(matches, () -> "Missing column expectation: " + expectation);
        }
    }

    private record ReportColumnExpectation(String header, ReportValueType valueType) {
        @Override
        public String toString() {
            return Stream.of(header, valueType.name()).reduce((left, right) -> left + " [" + right + "]").orElse(header);
        }
    }
}
