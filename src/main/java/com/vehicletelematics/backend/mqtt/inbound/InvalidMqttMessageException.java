package com.vehicletelematics.backend.mqtt.inbound;

final class InvalidMqttMessageException extends RuntimeException {

    InvalidMqttMessageException(String message) {
        super(message);
    }
}
