package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
}