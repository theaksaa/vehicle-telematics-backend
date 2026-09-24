package com.vehicletelematics.backend.users.api.error;

import com.vehicletelematics.backend.users.api.AuthController;
import com.vehicletelematics.backend.users.exception.EmailAlreadyExistsException;
import com.vehicletelematics.backend.users.exception.InvalidCurrentPasswordException;
import com.vehicletelematics.backend.users.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class UserApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthenticationException() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    ProblemDetail handleInvalidCurrentPassword(InvalidCurrentPasswordException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
