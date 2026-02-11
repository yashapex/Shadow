package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryRequest;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryResponse;
import hiring_project.shadow_assistant.service.AudioInterviewService;
import hiring_project.shadow_assistant.service.EliminatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eliminator")
@RequiredArgsConstructor
public class EliminatorController {

    private final EliminatorService eliminatorService;
    private final AudioInterviewService audioInterviewService;

    // ⚡ FIX: Update to accept applicationId and return a simple success message
    // POST /api/eliminator/evaluate/15
    @PostMapping("/evaluate/{applicationId}")
    public ResponseEntity<String> evaluateCandidate(@PathVariable Long applicationId) {
        // This triggers the AI, calculates the score, and saves it to the DB
        eliminatorService.evaluateCandidate(applicationId);

        return ResponseEntity.ok("Evaluation completed and saved successfully.");
    }

    // This method remains correct (it still uses candidateId/jobId in the service)
    @PostMapping("/generate-questions/{candidateId}")
    public ResponseEntity<InterviewQuestionsResponse> generateQuestions(
            @PathVariable Long candidateId,
            @RequestParam Long jobId) {
        return ResponseEntity.ok(eliminatorService.generateQuestions(candidateId, jobId));
    }

    @PostMapping("/finalize")
    public ResponseEntity<InterviewSummaryResponse> finalizeInterview(@RequestBody InterviewSummaryRequest request) {
        return ResponseEntity.ok(audioInterviewService.finalizeInterview(request));
    }
}