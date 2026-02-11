package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.job.JobPostRequest;
import hiring_project.shadow_assistant.entity.Job;
import hiring_project.shadow_assistant.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;

    // ⚡ NEW: Public/Candidate Endpoint to list all open roles
    @GetMapping("/all")
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    // --- ⚡ NEW: View/Download Job Description PDF ---
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getJobFile(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        if (job.getJdFile() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(job.getJdContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + job.getJdFileName() + "\"")
                .body(new ByteArrayResource(job.getJdFile()));
    }

    // Create Job (Multipart)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Job> createJob(
            @RequestPart("data") JobPostRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(jobService.createJob(request, file));
    }

    // List My Jobs
    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs() {
        return ResponseEntity.ok(jobService.getMyJobs());
    }

    // --- NEW: Get Single Job ---
    @GetMapping("/{userId}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // --- NEW: Update Job ---
    @PutMapping("/{userId}")
    public ResponseEntity<Job> updateJob(
            @PathVariable Long id,
            @RequestBody JobPostRequest request
    ) {
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    // --- NEW: Delete Job ---
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }
}