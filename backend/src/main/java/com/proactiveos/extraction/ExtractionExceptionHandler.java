package com.proactiveos.extraction;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExtractionExceptionHandler {

    @ExceptionHandler(AiExtractionException.class)
    ProblemDetail handleExtractionFailure(AiExtractionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Journal extraction failed");
        return problem;
    }
}