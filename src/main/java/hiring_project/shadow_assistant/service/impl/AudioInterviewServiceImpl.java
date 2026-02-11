package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.ai.AudioAnalysisResponse;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryRequest;
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryResponse;
import hiring_project.shadow_assistant.service.AudioInterviewService;
import hiring_project.shadow_assistant.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioInterviewServiceImpl implements AudioInterviewService {

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;

    @Override
    public String transcribeAudio(MultipartFile audioFile) {
        log.info("Starting transcription for file: {} (Size: {} bytes)",
                audioFile.getOriginalFilename(), audioFile.getSize());

        // 1. Convert MultipartFile to a Resource that OpenAI accepts
        Resource audioResource = convertToResource(audioFile);

        // 2. Configure Whisper Options
        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .temperature(0f) // 0 = Strict transcription, no creativity
                .build();

        // 3. Call OpenAI Whisper
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        String transcript = response.getResult().getOutput();
        log.info("Transcription completed. Length: {} chars", transcript.length());

        return transcript;
    }

    @Override
    public AudioAnalysisResponse evaluateTechnicalUnderstanding(String question, String transcript) {

        // 1. Generate the Trap Prompt
        String prompt = PromptUtil.getDepthDetectorPrompt(question, transcript);

        // 2. Ask GPT-4o to analyze
        return chatClient.prompt()
                .system(prompt)
                .call()
                .entity(AudioAnalysisResponse.class);
    }

    @Override
    public InterviewSummaryResponse finalizeInterview(InterviewSummaryRequest request) {

        // 1. Calculate Average Score Manually (More accurate than asking AI)
        int totalScore = 0;
        int genericCount = 0;
        StringBuilder logBuilder = new StringBuilder();

        for (InterviewSummaryRequest.QuestionResult result : request.results()) {
            totalScore += result.score();
            if (result.isGeneric()) genericCount++;

            logBuilder.append("Q: ").append(result.question()).append("\n")
                    .append("Score: ").append(result.score()).append("/100\n")
                    .append("Is Generic/AI: ").append(result.isGeneric()).append("\n")
                    .append("Transcript: ").append(result.transcript()).append("\n")
                    .append("-----------------------------------\n");
        }

        // Avoid divide by zero if list is empty
        int average = (request.results().isEmpty()) ? 0 : totalScore / request.results().size();

        // 2. Prepare Data for AI
        // We explicitly tell the AI the calculated average so it doesn't have to do math.
        String promptData = "Calculated Average Score: " + average + "\n" +
                "Count of Generic Answers: " + genericCount + "\n" +
                "Detailed Question Logs:\n" + logBuilder.toString();

        // 3. Generate Summary
        String prompt = PromptUtil.getRecruiterSummaryPrompt(promptData);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(InterviewSummaryResponse.class);
    }



    // --- Helper to handle the file conversion safely ---
    private Resource convertToResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    // OpenAI requires a filename with extension (e.g., "audio.wav")
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav";
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to read audio file", e);
        }
    }
}