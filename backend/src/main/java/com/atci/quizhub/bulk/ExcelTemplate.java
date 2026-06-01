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

    private ExcelTemplate() {}

    public static byte[] emptyTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build template", e);
        }
    }
}
