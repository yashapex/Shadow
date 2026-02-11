package hiring_project.shadow_assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recruiter_id", nullable = false)
    @JsonIgnoreProperties({"jobs", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
    private User recruiter;

    // --- Structured Fields from the Form ---
    @Column(nullable = false)
    private String title;           // e.g. "Senior Frontend Developer"

    @Column(nullable = false)
    private String location;        // e.g. "Remote"

    private String salaryRange;     // e.g. "$120k - $160k"

    private String experienceLevel; // e.g. "5+ years"

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;  // e.g. "React, TypeScript, Node.js"

    private String interviewType;   // e.g. "AI + Panel"

    // --- The "Simple File" Storage ---
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;     // Short description from the Text Area

    @Column(columnDefinition = "TEXT")
    private String fullTextFromPdf; // <--- RAW CONTENT of the uploaded PDF

    @Lob
    @JsonIgnore // Important: Don't send this huge data in the JSON list
//    @Column(columnDefinition = "LONGBLOB")
    private byte[] jdFile;

    private String jdFileName;
    private String jdContentType; // e.g., "application/pdf"

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Transient
    private long applicants;


    // Helper to check if file exists (for frontend)
    public boolean getHasPdf() {
        return jdFile != null && jdFile.length > 0;
    }
//    // Getter and Setter
//    public long getApplicants() {
//        return applicants;
//    }
//
//    public void setApplicants(long applicants) {
//        this.applicants = applicants;
//    }

}