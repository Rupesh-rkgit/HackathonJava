package com.atci.quizhub.bulk;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkService {

    private final McqRepository mcqs;
    private final TechStackRepository stacks;
    private final TopicRepository topics;
    private final UserRepository users;
    private final DataFormatter formatter = new DataFormatter();

    public BulkService(McqRepository mcqs, TechStackRepository stacks,
                       TopicRepository topics, UserRepository users) {
        this.mcqs = mcqs; this.stacks = stacks; this.topics = topics; this.users = users;
    }

    @Transactional
    public List<BulkRowResult> importFile(MultipartFile file, String creatorEnterpriseId) {
        User creator = users.findByEnterpriseId(creatorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<BulkRowResult> results = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row)) continue;
                results.add(processRow(row, r + 1, creator));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read Excel file: " + e.getMessage());
        }
        return results;
    }

    private BulkRowResult processRow(Row row, int rowNumber, User creator) {
        try {
            String stackName = cell(row, 0);
            String topicName = cell(row, 1);
            String difficultyRaw = cell(row, 2);
            String stem = cell(row, 3);
            String a = cell(row, 4), b = cell(row, 5), c = cell(row, 6), d = cell(row, 7);
            String correct = cell(row, 8);

            if (stem.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                return new BulkRowResult(rowNumber, false, "Missing required text field");
            }
            TechStack stack = stacks.findByName(stackName).orElse(null);
            if (stack == null) return new BulkRowResult(rowNumber, false, "Unknown stack: " + stackName);

            Topic topic = topics.findByStackId(stack.getId()).stream()
                    .filter(t -> t.getName().equalsIgnoreCase(topicName)).findFirst().orElse(null);
            if (topic == null) return new BulkRowResult(rowNumber, false, "Unknown topic: " + topicName);

            AnswerOption answer;
            try { answer = AnswerOption.valueOf(correct.trim().toUpperCase()); }
            catch (Exception e) { return new BulkRowResult(rowNumber, false, "Invalid correct answer: " + correct); }

            Difficulty difficulty;
            try { difficulty = Difficulty.valueOf(difficultyRaw.trim().toUpperCase()); }
            catch (Exception e) { return new BulkRowResult(rowNumber, false, "Invalid difficulty: " + difficultyRaw); }

            Mcq m = new Mcq();
            m.setQuestionStem(stem);
            m.setOptionA(a); m.setOptionB(b); m.setOptionC(c); m.setOptionD(d);
            m.setCorrectAnswer(answer); m.setDifficulty(difficulty);
            m.setStack(stack); m.setTopic(topic); m.setCreator(creator);
            m.setStatus(McqStatus.DRAFT);
            mcqs.save(m);
            return new BulkRowResult(rowNumber, true, "Imported as Draft");
        } catch (Exception e) {
            return new BulkRowResult(rowNumber, false, "Error: " + e.getMessage());
        }
    }

    private boolean isBlank(Row row) {
        for (int i = 0; i < ExcelTemplate.HEADERS.length; i++) {
            if (!cell(row, i).isBlank()) return false;
        }
        return true;
    }

    private String cell(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        String v = formatter.formatCellValue(cell);
        return v == null ? "" : v.trim();
    }
}
