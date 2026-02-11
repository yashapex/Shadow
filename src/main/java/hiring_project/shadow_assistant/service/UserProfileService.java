package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.ai.SkillAnalysisResponse;
import hiring_project.shadow_assistant.dto.profile.CandidateProfileDTO;
import hiring_project.shadow_assistant.dto.profile.RecruiterProfileDTO;
import hiring_project.shadow_assistant.entity.CandidateProfile;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface UserProfileService {
    // Getters
    CandidateProfileDTO getCandidateProfile();
    RecruiterProfileDTO getRecruiterProfile();

    // Updaters
    CandidateProfileDTO updateCandidateProfile(CandidateProfileDTO dto);
    RecruiterProfileDTO updateRecruiterProfile(RecruiterProfileDTO dto);

    // ⚡ NEW METHODS
    void uploadResume(MultipartFile file) throws IOException;
    CandidateProfile getProfileEntity(); // Helper for download controller
    SkillAnalysisResponse analyzeCandidateSkills(Long userId);
}