package com.proactiveos.journal.controller;

import com.proactiveos.journal.service.JournalNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JournalExceptionHandler {

    @ExceptionHandler(JournalNotFoundException.class)
    ProblemDetail handleNotFound(JournalNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Journal entry not found");
        return problem;
    }
}