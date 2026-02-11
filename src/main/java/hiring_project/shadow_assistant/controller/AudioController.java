package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.ai.AudioAnalysisResponse;
import hiring_project.shadow_assistant.service.AudioInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/interview/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioInterviewService audioService;

    @PostMapping("/analyze")
    public ResponseEntity<AudioAnalysisResponse> analyzeAudioAnswer(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question) {

        // 1. Transcribe
        String transcript = audioService.transcribeAudio(file);

        // 2. Analyze
        AudioAnalysisResponse response = audioService.evaluateTechnicalUnderstanding(question, transcript);

        return ResponseEntity.ok(response);
    }
}