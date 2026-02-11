package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import this
import org.springframework.data.repository.query.Param; // Import this
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);
    List<JobApplication> findByCandidateId(Long candidateId);
    List<JobApplication> findByJobId(Long jobId);
    long countByJobId(Long jobId);

//    // ⚡ FIX: Explicit Query to join tables
//    // "Select applications where the Job's Recruiter ID matches the parameter"
//    @Query("SELECT ja FROM JobApplication ja WHERE ja.job.recruiter.id = :recruiterId")
//    List<JobApplication> findByJobRecruiterId(@Param("recruiterId") Long recruiterId);

//    // Explicitly fetch the analyses to avoid LazyInitializationException in the controller
//    @Query("SELECT ja FROM JobApplication ja LEFT JOIN FETCH ja.interviewAnalyses WHERE ja.job.recruiter.id = :recruiterId")
//    List<JobApplication> findByJobRecruiterId(@Param("recruiterId") Long recruiterId); //
// hiring_project/shadow_assistant/repository/JobApplicationRepository.java


    @Query("SELECT DISTINCT ja FROM JobApplication ja " +
            "LEFT JOIN FETCH ja.candidate " +   // <--- Fetches Name/Email
            "LEFT JOIN FETCH ja.interviewAnalyses " + // <--- Fetches Interview Answers
            "JOIN ja.job j WHERE j.recruiter.id = :recruiterId")
    List<JobApplication> findByJobRecruiterId(@Param("recruiterId") Long recruiterId);
}