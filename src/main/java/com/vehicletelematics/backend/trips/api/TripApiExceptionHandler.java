package com.vehicletelematics.backend.trips.api;

import com.vehicletelematics.backend.trips.exception.TripNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class TripApiExceptionHandler {

    @ExceptionHandler(TripNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(TripNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }
}
