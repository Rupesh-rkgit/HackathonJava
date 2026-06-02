package com.atci.quizhub.bulk;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ExcelTemplate {

    // Column order for Template_MCQs.xlsx
    public static final String[] HEADERS = {
        "Stack_name", "Topic_name", "Difficulty", "Question_Stem",
        "Option_A", "Option_B", "Option_C", "Option_D", "Correct_answer"
    };

    // One example row so the file opens self-explanatory in Excel (users replace/append).
    private static final String[] EXAMPLE_ROW = {
        "Spring Cloud", "Introduction to Spring Cloud", "EASY",
        "What is the primary purpose of Spring Cloud?",
        "To replace Spring Boot",
        "To provide tools for building distributed systems and microservices",
        "To manage only database transactions",
        "To handle only UI development",
        "B"
    };

    private static final int[] COLUMN_WIDTHS = {
        18 * 256, 32 * 256, 12 * 256, 50 * 256, 26 * 256, 26 * 256, 26 * 256, 26 * 256, 16 * 256
    };

    private ExcelTemplate() {}

    public static byte[] emptyTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");

            // Header style: explicit WHITE bold text on a solid NAVY fill, with
            // borders. Everything is explicit so it never resolves to an invisible
            // theme colour in any Excel theme.
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x1A,(byte)0x2B,(byte)0x4A}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                if (i < COLUMN_WIDTHS.length) {
                    sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
                }
            }

            // Example row
            Row example = sheet.createRow(1);
            for (int i = 0; i < EXAMPLE_ROW.length; i++) {
                example.createCell(i).setCellValue(EXAMPLE_ROW[i]);
            }

            sheet.createFreezePane(0, 1); // keep header visible while scrolling

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build template", e);
        }
    }
}
