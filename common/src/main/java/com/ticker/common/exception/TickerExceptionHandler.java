package com.ticker.common.exception;

import com.ticker.common.model.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class TickerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TickerException.class)
    public ResponseEntity<ResponseStatus> customServiceException(Exception e, WebRequest request) {

        return new ResponseEntity<>(new ResponseStatus(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}