package hiring_project.shadow_assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    private Long userId; // Shared Key with User

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String jobTitle;       // e.g. "Senior Software Engineer"
    private String phone;
    private String location;       // e.g. "San Francisco, CA"

    @Column(columnDefinition = "TEXT")
    private String bio;            // e.g. "Passionate frontend engineer..."

    @Column(columnDefinition = "TEXT")
    private String skills;         // Comma-separated: "React, TypeScript, AWS"

    // Links
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;

    // ⚡ NEW: Store the Resume File
    @Lob
    @JsonIgnore // Don't send the heavy file in JSON profile fetch
    private byte[] resumeFile;

    private String resumeFileName;
    private String resumeContentType;

    private String avatarUrl;      // For "Change Photo"

    private LocalDateTime resumeLastUpdated; // To show "Last updated: Jan 15"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String skillAnalysisJson; // ⚡ NEW: Stores the AI analysis
}