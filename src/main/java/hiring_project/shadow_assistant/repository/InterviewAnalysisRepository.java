package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.InterviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewAnalysisRepository extends JpaRepository<InterviewAnalysis, Long> {
    List<InterviewAnalysis> findByApplicationId(Long applicationId);
    /**
     * Count number of answers submitted for an application
     * Useful for tracking progress
     */
    long countByApplicationId(Long applicationId);

    /**
     * Delete all analysis records for an application
     * (Use with caution - primarily for cleanup/testing)
     */
    void deleteByApplicationId(Long applicationId);
}