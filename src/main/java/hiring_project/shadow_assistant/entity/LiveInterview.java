package hiring_project.shadow_assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_interviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the main application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion
    private JobApplication application;

    // Scheduling Details
    private LocalDateTime scheduledAt;
    private String meetingLink; // Google Meet URL

    private String status; // "SCHEDULED", "COMPLETED", "CANCELLED"

    // Post-Interview Data
    @Lob
    @Column(columnDefinition = "TEXT")
    private String transcript; // Full text of the conversation

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiInsights;

    @CreationTimestamp
    private LocalDateTime createdAt;
}