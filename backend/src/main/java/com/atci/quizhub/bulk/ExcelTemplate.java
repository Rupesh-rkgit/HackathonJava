package com.atci.quizhub.bulk;

import org.apache.poi.ss.usermodel.*;
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
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");

            // Bold header style
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

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
