package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.ai.SkillAnalysisResponse;
import hiring_project.shadow_assistant.dto.profile.CandidateProfileDTO;
import hiring_project.shadow_assistant.dto.profile.RecruiterProfileDTO;
import hiring_project.shadow_assistant.entity.CandidateProfile;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import needed for upload
import java.nio.file.*;                              // Import needed for file ops
import java.util.UUID;                               // Import needed for unique names
import java.io.IOException;                          // Import needed for exceptions

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthUtil authUtil;

    // --- Candidate Endpoints ---

    @GetMapping("/candidate")
    public ResponseEntity<CandidateProfileDTO> getCandidateProfile() {
        return ResponseEntity.ok(userProfileService.getCandidateProfile());
    }

    @PutMapping("/candidate")
    public ResponseEntity<CandidateProfileDTO> updateCandidateProfile(@RequestBody CandidateProfileDTO dto) {
        return ResponseEntity.ok(userProfileService.updateCandidateProfile(dto));
    }

    // --- ⚡ NEW: Upload Resume Endpoint ---
    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            userProfileService.uploadResume(file);
            return ResponseEntity.ok("Resume uploaded and processed successfully");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload resume: " + e.getMessage());
        }
    }

    // --- ⚡ NEW: Download/View Resume Endpoint ---
    @GetMapping("/resume")
    public ResponseEntity<Resource> getResume() {
        CandidateProfile profile = userProfileService.getProfileEntity();

        if (profile.getResumeFile() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(profile.getResumeContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + profile.getResumeFileName() + "\"")
                .body(new ByteArrayResource(profile.getResumeFile()));
    }

    // --- Recruiter Endpoints ---

    @GetMapping("/recruiter")
    public ResponseEntity<RecruiterProfileDTO> getRecruiterProfile() {
        return ResponseEntity.ok(userProfileService.getRecruiterProfile());
    }

    @PutMapping("/recruiter")
    public ResponseEntity<RecruiterProfileDTO> updateRecruiterProfile(@RequestBody RecruiterProfileDTO dto) {
        return ResponseEntity.ok(userProfileService.updateRecruiterProfile(dto));
    }

    // --- ⚡ RESTORED: File Upload Endpoint ---
    @PostMapping("/upload-avatar")
    public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        try {
            // 1. Create "uploads" directory in project root
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 3. Save the file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Return the public URL
            // NOTE: Ensure port 8090 matches your application.properties server.port
            String fileUrl = "http://localhost:8090/uploads/" + newFilename;
            return ResponseEntity.ok(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Could not upload file: " + e.getMessage());
        }
    }

    // âš¡ NEW: Analyze Resume for Skills
    @GetMapping("/candidate/analysis/skills")
    public ResponseEntity<SkillAnalysisResponse> analyzeSkills() {
        Long userId = authUtil.getLoggedInUserId();
        return ResponseEntity.ok(userProfileService.analyzeCandidateSkills(userId));
    }
}