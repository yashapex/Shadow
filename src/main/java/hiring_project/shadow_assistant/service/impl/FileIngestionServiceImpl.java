package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.entity.DocumentEntity;
import hiring_project.shadow_assistant.repository.DocumentRepository;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.FileIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestionServiceImpl implements FileIngestionService {

    private final VectorStore vectorStore;
    private final AuthUtil authUtil;
    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;

    @Override
    public void ingestFile(MultipartFile file, String docType) throws IOException {
        Long userId = authUtil.getLoggedInUserId();
        String referenceId = (docType.equals("RESUME")) ? "CANDIDATE_" + userId : "JOB_" + userId;

        log.info("Starting ingestion for {} ID: {}", docType, referenceId);

        // 1. Read Text
        String fullContent = extractTextFromFile(file);

        // 2. 💾 SAVE TO SIMPLE DB (Postgres) - "The Library"
        // We check if it exists first to overwrite instead of duplicating
        DocumentEntity docEntity = documentRepository.findByUserIdAndDocType(userId, docType)
                .orElse(DocumentEntity.builder()
                        .userId(userId)
                        .docType(docType)
                        .build());

        docEntity.setFullContent(fullContent);
        documentRepository.save(docEntity);
        log.info("Saved Full Original Text to Postgres for User: {}", userId);

        // 3. AI Extraction & Vector Store
        processAndVectorize(fullContent, docType, referenceId);
    }

    // --- NEW: Helper to read PDF Text ---
    @Override
    public String extractTextFromFile(MultipartFile file) throws IOException {
        Resource pdfResource = new InputStreamResource(file.getInputStream());
        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(pdfResource);
        List<Document> rawDocuments = documentReader.read();

        return rawDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
    }

    // --- NEW: Specific Logic for Jobs (Uses Job ID, not User ID) ---
    @Override
    public void ingestJobContent(String content, Long jobId) {
        // We use the same AI logic, but with a specific Reference ID for this Job
        String referenceId = "JOB_" + jobId;
        processAndVectorize(content, "JD", referenceId);
    }

    // --- SHARED LOGIC: Extracts Info & Saves to Vector Store ---
    private void processAndVectorize(String content, String docType, String referenceId) {
        if (content == null || content.isBlank()) {
            log.warn("Skipping vectorization for empty content: {}", referenceId);
            return;
        }
        // ⚡ CRITICAL FIX
        deleteOldVectors(referenceId);

        log.info("Running AI Extraction for ID: {}", referenceId);

        String refinedContent = extractRelevantInfo(content, docType);

        Document smartDocument = new Document(refinedContent);
        smartDocument.getMetadata().put("docType", docType);
        smartDocument.getMetadata().put("referenceId", referenceId);

        vectorStore.add(List.of(smartDocument));
        log.info("Vectorization complete for: {}", referenceId);
    }

    // --- ⚡ CLEANUP HELPER ---
    private void deleteOldVectors(String referenceId) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            // ⚡ KEY FIX: We must provide a query string (like "resume" or "work")
            // so the vector store actually finds the documents to delete.
            // Searching with an empty query often returns nothing.
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("resume experience skills work job") // Dummy query to match likely content
                            .topK(200) // Increase limit to catch all chunks
                            .similarityThreshold(0.0) // Accept any match that passes the filter
                            .filterExpression(b.eq("referenceId", referenceId).build())
                            .build()
            );

            // Extract IDs
            List<String> ids = existingDocs.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
                log.info("Deleted {} old vectors for {}", ids.size(), referenceId);
            } else {
                log.info("No old vectors found to delete for {}", referenceId);
            }
        } catch (Exception e) {
            log.error("Failed to delete old vectors: {}", e.getMessage());
        }
    }

    private String extractRelevantInfo(String content, String docType) {
        String prompt = "";

        if (docType.equals("RESUME")) {
            prompt = """
                You are a Data Extractor. Read the following Resume and extract ONLY:
                1. Technical Skills (Languages, Frameworks, Tools).
                2. Project Titles and a brief 2-3 lines description of what they built and what they have used to build it.
                3. Work Experience (Role, Company, and key technical tasks).
                
                Ignore: Intro, Objective, Address, Hobbies, References.
                Output format: Plain text, concise.
                
                Resume Content:
                """ + content;
        } else {
            prompt = """
                You are a Data Extractor. Read the following Job Description and extract ONLY:
                1. Required Tech Stack (Must-haves).
                2. Key Responsibilities (What they will actually do).
                3. Years of Experience required.
                
                Ignore: Company Perks, "About Us", Equal Opportunity statements.
                Output format: Plain text, concise.
                
                JD Content:
                """ + content;
        }

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @Override
    public void ingestResumeContent(String content, Long candidateId) {
        // We link this vector to the Candidate ID so the Eliminator can find it later
        // Ref ID format: "CANDIDATE_{userId}" matches what EliminatorServiceImpl expects
        String referenceId = "CANDIDATE_" + candidateId;

        log.info("Ingesting Resume Summary for Candidate ID: {}", candidateId);
        processAndVectorize(content, "RESUME", referenceId);
    }
}
