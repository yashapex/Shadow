package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.LiveInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LiveInterviewRepository extends JpaRepository<LiveInterview, Long> {
    Optional<LiveInterview> findByApplicationId(Long applicationId);
}