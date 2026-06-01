package com.atci.quizhub.bulk;

import com.atci.quizhub.auth.CurrentUser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bulk")
public class BulkController {

    private final BulkService bulkService;
    private final CurrentUser currentUser;

    public BulkController(BulkService bulkService, CurrentUser currentUser) {
        this.bulkService = bulkService; this.currentUser = currentUser;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template() {
        byte[] bytes = ExcelTemplate.emptyTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_MCQs.xlsx")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/upload")
    public List<BulkRowResult> upload(@RequestParam("file") MultipartFile file) {
        return bulkService.importFile(file, currentUser.get().getEnterpriseId());
    }
}
