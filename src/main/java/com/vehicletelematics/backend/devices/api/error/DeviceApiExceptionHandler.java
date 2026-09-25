package com.vehicletelematics.backend.devices.api.error;

import com.vehicletelematics.backend.devices.api.DeviceController;
import com.vehicletelematics.backend.devices.exception.DeviceAlreadyExistsException;
import com.vehicletelematics.backend.devices.exception.DeviceAssignmentConflictException;
import com.vehicletelematics.backend.devices.exception.DeviceNotFoundException;
import com.vehicletelematics.backend.devices.exception.InvalidDeviceDataException;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = DeviceController.class)
public class DeviceApiExceptionHandler {

    @ExceptionHandler({DeviceNotFoundException.class, VehicleNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
            DeviceAlreadyExistsException.class,
            DeviceAssignmentConflictException.class,
            DataIntegrityViolationException.class
    })
    ProblemDetail handleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidDeviceDataException.class)
    ProblemDetail handleInvalidDevice(InvalidDeviceDataException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
