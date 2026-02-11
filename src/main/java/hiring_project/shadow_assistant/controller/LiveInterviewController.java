package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.entity.LiveInterview;
import hiring_project.shadow_assistant.service.LiveInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/live-interviews")
@RequiredArgsConstructor
public class LiveInterviewController {

    private final LiveInterviewService liveInterviewService;

    // Get Interview Details
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<LiveInterview> getInterviewByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(liveInterviewService.getInterviewByApplicationId(applicationId));
    }

    // Upload Transcript & Run Analysis
    @PostMapping("/{interviewId}/analyze")
    public ResponseEntity<LiveInterview> analyzeTranscript(
            @PathVariable Long interviewId,
            @RequestBody String transcriptText) { // Accepting raw text for simplicity

        return ResponseEntity.ok(liveInterviewService.analyzeInterview(interviewId, transcriptText));
    }
}