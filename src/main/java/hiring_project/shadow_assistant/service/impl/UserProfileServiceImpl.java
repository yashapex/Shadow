package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.ai.SkillAnalysisResponse;
import hiring_project.shadow_assistant.dto.profile.CandidateProfileDTO;
import hiring_project.shadow_assistant.dto.profile.RecruiterProfileDTO;
import hiring_project.shadow_assistant.entity.CandidateProfile;
import hiring_project.shadow_assistant.entity.RecruiterProfile;
import hiring_project.shadow_assistant.entity.User;
import hiring_project.shadow_assistant.repository.CandidateProfileRepository;
import hiring_project.shadow_assistant.repository.RecruiterProfileRepository;
import hiring_project.shadow_assistant.repository.UserRepository;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.FileIngestionService;
import hiring_project.shadow_assistant.service.UserProfileService;
import hiring_project.shadow_assistant.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final AuthUtil authUtil;
    private final FileIngestionService fileIngestionService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    // --- CANDIDATE METHODS ---

    @Override
    public CandidateProfileDTO getCandidateProfile() {
        User user = getLoggedInUser();
        CandidateProfile profile = candidateProfileRepository.findById(user.getId())
                .orElse(CandidateProfile.builder().user(user).build());

        return mapToCandidateDTO(user, profile);
    }

    @Override
    @Transactional
    public CandidateProfileDTO updateCandidateProfile(CandidateProfileDTO dto) {
        User user = getLoggedInUser();

        // 1. Update Core User Data (Name)
        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
            userRepository.save(user);
        }

        CandidateProfile profile = candidateProfileRepository.findById(user.getId())
                .orElse(CandidateProfile.builder().user(user).build());

        // Update Fields
        profile.setJobTitle(dto.jobTitle());
        profile.setPhone(dto.phone());
        profile.setLocation(dto.location());
        profile.setBio(dto.bio());
        profile.setSkills(dto.skills());
        profile.setGithubUrl(dto.githubUrl());
        profile.setLinkedinUrl(dto.linkedinUrl());
        profile.setPortfolioUrl(dto.portfolioUrl());
        profile.setAvatarUrl(dto.avatarUrl());

        CandidateProfile saved = candidateProfileRepository.save(profile);
        return mapToCandidateDTO(user, saved);
    }

    // --- RECRUITER METHODS ---

    @Override
    public RecruiterProfileDTO getRecruiterProfile() {
        User user = getLoggedInUser();
        RecruiterProfile profile = recruiterProfileRepository.findById(user.getId())
                .orElse(RecruiterProfile.builder().user(user).build());

        return mapToRecruiterDTO(user, profile);
    }

    @Override
    @Transactional
    public RecruiterProfileDTO updateRecruiterProfile(RecruiterProfileDTO dto) {
        User user = getLoggedInUser();
        RecruiterProfile profile = recruiterProfileRepository.findById(user.getId())
                .orElse(RecruiterProfile.builder().user(user).build());

        // Update Fields
        profile.setPosition(dto.position());
        profile.setDepartment(dto.department());
        profile.setCompanyName(dto.companyName());
        profile.setLocation(dto.location());
         profile.setAvatarUrl(dto.avatarUrl()); // Handle file upload separately if needed

        RecruiterProfile saved = recruiterProfileRepository.save(profile);
        return mapToRecruiterDTO(user, saved);
    }

    // --- ⚡ NEW: Resume Upload Logic ---
    @Override
    @Transactional
    public void uploadResume(MultipartFile file) throws IOException {
        User user = getLoggedInUser();

        CandidateProfile profile = candidateProfileRepository.findById(user.getId())
                .orElse(CandidateProfile.builder().user(user).build());

        // 1. Save File to Database (for Download)
        profile.setResumeFile(file.getBytes());
        profile.setResumeFileName(file.getOriginalFilename());
        profile.setResumeContentType(file.getContentType());
        profile.setResumeLastUpdated(LocalDateTime.now());

        candidateProfileRepository.save(profile);

        // 2. Trigger AI Ingestion (Extracts skills, experience, etc.)
        // This ensures the resume is searchable by recruiters
        fileIngestionService.ingestFile(file, "RESUME");
    }

//    @Override
//    public SkillAnalysisResponse analyzeCandidateSkills(Long userId) {
//        // 1. Fetch Profile
//        CandidateProfile profile = candidateProfileRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("Candidate profile not found"));
//
//        if (profile.getResumeFile() == null || profile.getResumeFile().length == 0) {
//            throw new RuntimeException("No resume found. Please upload a resume first.");
//        }
//
//        // 2. Extract Text from PDF (Assuming PDF for now, can be extended)
//        String resumeText = extractTextFromBytes(profile.getResumeFile());
//
//        // 3. Call AI
//        String prompt = PromptUtil.getSkillAnalysisPrompt(resumeText);
//
//        return chatClient.prompt()
//                .user(prompt)
//                .call()
//                .entity(SkillAnalysisResponse.class); // Spring AI auto-maps JSON to DTO
//    }

    private String extractTextFromBytes(byte[] fileData) {
        try {
            // Use Spring AI's built-in PDF reader
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new ByteArrayResource(fileData));
            List<Document> documents = pdfReader.read();
            return documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to parse PDF resume", e);
            // Fallback: If it's a text file or parsing fails, return empty or raw string
            return "";
        }
    }

    @Override
    @Transactional // Ensure transaction for saving
    public SkillAnalysisResponse analyzeCandidateSkills(Long userId) {
        // 1. Fetch Profile
        CandidateProfile profile = candidateProfileRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("Candidate profile not found"));

        // 2. CHECK: If analysis exists, return it (Optimization)
        // If you want to force regeneration, you can add a 'force' parameter later
        if (profile.getSkillAnalysisJson() != null && !profile.getSkillAnalysisJson().isEmpty()) {
            try {
                return objectMapper.readValue(profile.getSkillAnalysisJson(), SkillAnalysisResponse.class);
            } catch (Exception e) {
                log.error("Failed to parse existing analysis, regenerating...", e);
            }
        }

        if (profile.getResumeFile() == null || profile.getResumeFile().length == 0) {
            throw new RuntimeException("No resume found. Please upload a resume first.");
        }

        // 3. Extract & Analyze (Existing Logic)
        String resumeText = extractTextFromBytes(profile.getResumeFile());
        String prompt = PromptUtil.getSkillAnalysisPrompt(resumeText);

        SkillAnalysisResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(SkillAnalysisResponse.class);

        // 4. SAVE: Serialize and store in DB
        try {
            String json = objectMapper.writeValueAsString(response);
            profile.setSkillAnalysisJson(json);
            candidateProfileRepository.save(profile);
        } catch (Exception e) {
            log.error("Failed to save skill analysis to DB", e);
        }

        return response;
    }



    // --- HELPERS ---

    @Override
    @Transactional(readOnly = true) // Important for Postgres LOB
    public CandidateProfile getProfileEntity() {
        User user = getLoggedInUser();
        return candidateProfileRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    private User getLoggedInUser() {
        Long id = authUtil.getLoggedInUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private CandidateProfileDTO mapToCandidateDTO(User user, CandidateProfile p) {
        return new CandidateProfileDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                p.getJobTitle(),
                p.getPhone(),
                p.getLocation(),
                p.getBio(),
                p.getSkills(),
                p.getGithubUrl(),
                p.getLinkedinUrl(),
                p.getPortfolioUrl(),
                p.getAvatarUrl(),
                p.getResumeLastUpdated()
        );
    }

    private RecruiterProfileDTO mapToRecruiterDTO(User user, RecruiterProfile p) {
        return new RecruiterProfileDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                p.getPosition(),
                p.getDepartment(),
                p.getCompanyName(),
                p.getLocation(),
                p.getAvatarUrl()
        );
    }
}