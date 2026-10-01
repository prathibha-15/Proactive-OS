package com.proactiveos.events.controller;

import com.proactiveos.events.service.EventTypeMismatchException;
import com.proactiveos.events.service.LifeEventNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LifeEventExceptionHandler {

    @ExceptionHandler(LifeEventNotFoundException.class)
    ProblemDetail handleNotFound(LifeEventNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Life event not found");
        return problem;
    }

    @ExceptionHandler(EventTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(EventTypeMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Event type cannot be changed");
        return problem;
    }
}
