package com.atci.quizhub.bulk;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BulkServiceTest {

    @Autowired BulkService bulkService;

    private MockMultipartFile xlsx(String[][] rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");
            Row header = sheet.createRow(0);
            for (int i = 0; i < ExcelTemplate.HEADERS.length; i++)
                header.createCell(i).setCellValue(ExcelTemplate.HEADERS[i]);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            wb.write(out);
            return new MockMultipartFile("file", "Template_MCQs.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void validRowImportsAsDraft() throws Exception {
        var file = xlsx(new String[][]{
            {"Spring Cloud","Introduction to Spring Cloud","EASY","What is Spring Cloud?","a","b","c","d","B"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertEquals(1, results.size());
        assertTrue(results.get(0).success(), results.get(0).message());
    }

    @Test
    void invalidCorrectAnswerFailsRow() throws Exception {
        var file = xlsx(new String[][]{
            {"Spring Cloud","Introduction to Spring Cloud","EASY","Q?","a","b","c","d","Z"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertFalse(results.get(0).success());
    }

    @Test
    void unknownStackFailsRow() throws Exception {
        var file = xlsx(new String[][]{
            {"Nonexistent Stack","Some Topic","EASY","Q?","a","b","c","d","A"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertFalse(results.get(0).success());
    }
}
