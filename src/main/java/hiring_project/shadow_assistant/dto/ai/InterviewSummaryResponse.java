package hiring_project.shadow_assistant.dto.ai;

public record InterviewSummaryResponse(
        int averageScore,
        String aiRecommendation,   // "Highly Recommended", "Worth Interviewing", "Not Recommended"
        String keyObservation,     // e.g. "Strong Java skills but weak database knowledge."
        String redFlags,           // e.g. "Generic answers detected in Q2."
        String interviewSummary    // A 2-sentence summary for the Recruiter
) {}