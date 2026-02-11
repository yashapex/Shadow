package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.entity.LiveInterview;
import hiring_project.shadow_assistant.repository.LiveInterviewRepository;
import hiring_project.shadow_assistant.service.LiveInterviewService;
import hiring_project.shadow_assistant.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveInterviewServiceImpl implements LiveInterviewService {

    private final LiveInterviewRepository liveInterviewRepository;
    private final ChatClient chatClient;

    @Transactional
    @Override
    public LiveInterview getInterviewByApplicationId(Long applicationId) {
        return liveInterviewRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("No interview scheduled for this application"));
    }

    @Transactional
    @Override
    public LiveInterview analyzeInterview(Long interviewId, String transcript) {
        LiveInterview interview = liveInterviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        interview.setTranscript(transcript);
        interview.setStatus("COMPLETED");

        // 1. Generate Insights instead of Bias Audit
        String prompt = PromptUtil.getInterviewInsightsPrompt(transcript);

        String insightsJson = chatClient.prompt().user(prompt).call().content();

        // 2. Save to new field
        interview.setAiInsights(insightsJson);

        return liveInterviewRepository.save(interview);
    }
}