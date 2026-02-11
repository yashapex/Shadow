package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;
import hiring_project.shadow_assistant.dto.job.ScheduleInterviewRequest;
import hiring_project.shadow_assistant.entity.JobApplication;
import hiring_project.shadow_assistant.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;

    // ⚡ ONE-CLICK APPLY (No file upload needed here)
    @PostMapping("/{jobId}/apply")
    public ResponseEntity<String> applyForJob(@PathVariable Long jobId) {
        try {
            applicationService.applyForJob(jobId);
            return ResponseEntity.ok("Application submitted successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get List (For Recruiter Dashboard)
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplication>> getJobApplications(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId));
    }

    // ⚡ PRE-INTERVIEW: Generate Questions
    @PostMapping("/interview/generate-questions/{applicationId}")
    public ResponseEntity<InterviewQuestionsResponse> generateInterviewQuestions(
            @PathVariable Long applicationId) {

        // The service returns a SINGLE object containing the list
        return ResponseEntity.ok(applicationService.generatePreInterviewQuestions(applicationId));
    }

    // ⚡ PRE-INTERVIEW: Submit Interview
    @PostMapping("/interview/submit/{applicationId}")
    public ResponseEntity<?> submitInterview(
            @PathVariable Long applicationId,
            @RequestBody Map<String, Object> results) {

        applicationService.submitInterview(applicationId, results);
        return ResponseEntity.ok().body(Map.of("message", "Interview submitted successfully"));
    }

    // Get My Applications (For Candidate Dashboard)
    @Transactional(readOnly = true)
    @GetMapping("/my-applications")
    public ResponseEntity<List<JobApplication>> getMyApplications() {
        return ResponseEntity.ok(applicationService.getMyApplications());
    }

    // Download Resume Snapshot
    @GetMapping("/{applicationId}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long applicationId) {
        JobApplication app = applicationService.getApplicationById(applicationId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(app.getResumeContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + app.getResumeFileName() + "\"")
                .body(new ByteArrayResource(app.getResumeFile()));
    }

    // ⚡ NEW: Get All Applications for Recruiter Dashboard
    @GetMapping("/recruiter/all")
    public ResponseEntity<List<JobApplication>> getRecruiterApplications() {
        // This now returns the app + the nested interviewAnalyses list
        return ResponseEntity.ok(applicationService.getApplicationsForRecruiter()); //
    }

    // 1. Submit Single Answer
    @PostMapping("/interview/{applicationId}/answer")
    public ResponseEntity<String> submitAnswer(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> payload) { // Expects { "question": "...", "answer": "..." }

        applicationService.submitAnswer(
                applicationId,
                payload.get("question"),
                payload.get("answer")
        );
        return ResponseEntity.ok("Answer analyzed and saved.");
    }

    // 2. Finalize Interview
    @PostMapping("/interview/{applicationId}/finalize")
    public ResponseEntity<String> finalizeInterview(@PathVariable Long applicationId) {
        applicationService.finalizeInterview(applicationId); //
        return ResponseEntity.ok("Interview finalized and results saved.");
    }

    // ⚡ NEW: Endpoint to update application status (Shortlist/Reject)
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<String> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status) {
        try {
            applicationService.updateApplicationStatus(applicationId, status);
            return ResponseEntity.ok("Application status updated to " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{applicationId}/schedule")
    public ResponseEntity<String> scheduleInterview(
            @PathVariable Long applicationId,
            @RequestBody ScheduleInterviewRequest request) {

        applicationService.scheduleInterview(applicationId, request);
        return ResponseEntity.ok("Interview scheduled successfully.");
    }
}