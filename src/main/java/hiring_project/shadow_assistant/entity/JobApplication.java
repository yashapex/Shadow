package hiring_project.shadow_assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hiring_project.shadow_assistant.enums.ApplicationStatus;
import hiring_project.shadow_assistant.enums.Verdict;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "job_applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"candidate_id", "job_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Lob
    @Column(nullable = false)
    @JsonIgnore
    private byte[] resumeFile;
    private String resumeFileName;
    private String resumeContentType;

    // --- Resume Parsing Fields ---
    private Integer matchScore;
    @Column(columnDefinition = "TEXT")
    private String aiFeedback;
    @Enumerated(EnumType.STRING)
    private Verdict aiVerdict;

    // --- Pre-Interview Fields ---
    private Integer interviewScore;

    @Column(columnDefinition = "TEXT")
    private String interviewSummary;

    // ⚡ FIX: Add these missing columns so the data has a place to live
    @Column(columnDefinition = "TEXT")
    private String keyObservation;

    @Column(columnDefinition = "TEXT")
    private String redFlags;

    @Enumerated(EnumType.STRING)
    private Verdict interviewVerdict;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InterviewAnalysis> interviewAnalyses;

    // We use "mappedBy" so this entity doesn't store the foreign key
    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private LiveInterview liveInterview;

    @CreationTimestamp
    private LocalDateTime appliedAt;
}