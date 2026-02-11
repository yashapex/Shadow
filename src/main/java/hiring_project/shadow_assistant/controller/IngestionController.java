package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.FileIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final FileIngestionService fileIngestionService;
    private final AuthUtil authUtil;

    @PostMapping
    public ResponseEntity<String> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docType) throws IOException{
            Long userId = authUtil.getLoggedInUserId();
            log.info("Received request to ingest file: {}, Type: {}, ID: {}",
                    file.getOriginalFilename(), docType, userId);

            fileIngestionService.ingestFile(file, docType);

            return ResponseEntity.ok("Ingestion successful for ID: " + userId);

    }
}
