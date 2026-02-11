package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.ai.AudioAnalysisResponse;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryRequest;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AudioInterviewService {

    String transcribeAudio(MultipartFile audioFile);
    AudioAnalysisResponse evaluateTechnicalUnderstanding(String question, String transcript);

    InterviewSummaryResponse finalizeInterview(InterviewSummaryRequest request);
}