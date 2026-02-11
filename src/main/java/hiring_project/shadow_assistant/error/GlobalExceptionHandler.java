package hiring_project.shadow_assistant.error;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle File Upload Too Large (Common in your app)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("File upload exceeded limit: {}", exc.getMessage());

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "File Too Large",
                "The uploaded file is too large. Please upload a file smaller than 10MB."
        );
        return new ResponseEntity<>(error, HttpStatus.EXPECTATION_FAILED);
    }

    // 2. Handle IO Errors (File reading issues)
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIOException(IOException exc) {
        log.error("IO Exception occurred: ", exc);

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "File Processing Error",
                "Could not read the uploaded file. Please try again."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 3. Handle Generic / Unexpected Errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception exc) {
        log.error("Unexpected error: ", exc);

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                exc.getMessage() // In prod, you might hide this message for security
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 4. Handle Missing Resources (JD or Resume not found)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exc) {
        log.warn("Resource not found: {}", exc.getMessage());

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(), // 404 Status
                "Not Found",
                exc.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    //5. Handle OpenAI/Spring AI Specific Errors
    @ExceptionHandler(org.springframework.ai.retry.NonTransientAiException.class)
    public ResponseEntity<ApiError> handleAiException(Exception exc) {
        log.error("AI Service Error: ", exc);

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(), // 502: It's not our fault, it's the upstream AI
                "AI Processing Failed",
                "The AI service rejected the request. Please try again later."
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }
}
