package hiring_project.shadow_assistant.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileIngestionService {

    void ingestFile(MultipartFile file, String docType) throws IOException;

    // --- NEW METHODS FOR JOB SERVICE ---

    // 1. Helper to just read the PDF text (so JobService can save it to SQL)
    String extractTextFromFile(MultipartFile file) throws IOException;

    // 2. Helper to run AI Extraction & Vectorization for a specific Job ID
    void ingestJobContent(String content, Long jobId);

    void ingestResumeContent(String content, Long candidateId);
}
