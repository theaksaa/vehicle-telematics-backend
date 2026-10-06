package com.vehicletelematics.backend.geocoding.api;

import com.vehicletelematics.backend.geocoding.exception.GeocodingUnavailableException;
import com.vehicletelematics.backend.geocoding.exception.LocationNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = GeocodingController.class)
public class GeocodingApiExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleInvalidCoordinates(ConstraintViolationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(LocationNotFoundException.class)
    ProblemDetail handleLocationNotFound(LocationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(GeocodingUnavailableException.class)
    ProblemDetail handleGeocodingUnavailable(GeocodingUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
