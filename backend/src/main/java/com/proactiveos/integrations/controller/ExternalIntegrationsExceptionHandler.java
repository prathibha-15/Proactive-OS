package com.proactiveos.integrations.controller;

import com.proactiveos.integrations.provider.UnsupportedExternalProviderException;
import com.proactiveos.integrations.service.ClientUploadRequiredException;
import com.proactiveos.integrations.service.InvalidExternalActivityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ExternalIntegrationsController.class)
public class ExternalIntegrationsExceptionHandler {

    @ExceptionHandler(UnsupportedExternalProviderException.class)
    public ProblemDetail handleUnsupportedProvider(UnsupportedExternalProviderException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidExternalActivityException.class)
    public ProblemDetail handleInvalidProviderData(InvalidExternalActivityException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(ClientUploadRequiredException.class)
    public ProblemDetail handleClientUploadRequired(ClientUploadRequiredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}