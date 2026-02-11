package hiring_project.shadow_assistant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruiter_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterProfile {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String position;       // e.g. "HR Director" (from "Role" in UI)
    private String department;     // e.g. "Human Resources"
    private String companyName;    // e.g. "Shadow AI Inc."
    private String location;       // e.g. "San Francisco, CA"

    private String avatarUrl;      // For "Change Photo"
}