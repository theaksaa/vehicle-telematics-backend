package com.vehicletelematics.backend.users.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long id) {
        super("User not found: " + id);
    }
}
