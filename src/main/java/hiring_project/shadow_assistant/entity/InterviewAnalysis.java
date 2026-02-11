package hiring_project.shadow_assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore
    private JobApplication application;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String candidateAnswer; // The Transcript

    // AI Analysis Data
    private int technicalScore;
    private int communicationScore;
    private boolean isGeneric; // Did they use ChatGPT?

    @Column(columnDefinition = "TEXT")
    private String aiFeedback; // Short critique for this specific answer

    @CreationTimestamp
    private LocalDateTime createdAt;
}