package com.vehicletelematics.backend.vehicles.api.error;

import com.vehicletelematics.backend.vehicles.api.VehicleController;
import com.vehicletelematics.backend.vehicles.exception.InvalidVehicleDataException;
import com.vehicletelematics.backend.vehicles.exception.InvalidVehicleYearException;
import com.vehicletelematics.backend.vehicles.exception.RegistrationAlreadyExistsException;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.exception.VinAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = VehicleController.class)
public class VehicleApiExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    ProblemDetail handleVehicleNotFound(VehicleNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({RegistrationAlreadyExistsException.class, VinAlreadyExistsException.class})
    ProblemDetail handleVehicleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({InvalidVehicleDataException.class, InvalidVehicleYearException.class})
    ProblemDetail handleInvalidVehicle(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
