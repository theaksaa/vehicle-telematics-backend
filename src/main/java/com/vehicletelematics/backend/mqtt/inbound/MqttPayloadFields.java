package com.vehicletelematics.backend.mqtt.inbound;

import tools.jackson.databind.JsonNode;

final class MqttPayloadFields {

    private MqttPayloadFields() {
    }

    static long requiredLong(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw invalid(field, "is required and must be an integer");
        }
        return value.longValue();
    }

    static Long optionalNonNegativeLong(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            throw invalid(field, "must be an integer");
        }
        return nonNegative(value.longValue(), field);
    }

    static Double optionalDouble(JsonNode json, String field, double minimum, double maximum) {
        JsonNode value = json.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw invalid(field, "must be a number");
        }
        double number = value.doubleValue();
        if (!Double.isFinite(number) || number < minimum || number > maximum) {
            throw invalid(field, "must be between " + minimum + " and " + maximum);
        }
        return number;
    }

    static Float optionalFloat(JsonNode json, String field, float minimum, float maximum) {
        Double value = optionalDouble(json, field, minimum, maximum);
        return value == null ? null : value.floatValue();
    }

    static Integer optionalInteger(JsonNode json, String field, int minimum, int maximum) {
        JsonNode value = json.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            throw invalid(field, "must be an integer");
        }
        long number = value.longValue();
        if (number < minimum || number > maximum) {
            throw invalid(field, "must be between " + minimum + " and " + maximum);
        }
        return (int) number;
    }

    static Short optionalShort(JsonNode json, String field, int minimum, int maximum) {
        Integer value = optionalInteger(json, field, minimum, maximum);
        return value == null ? null : value.shortValue();
    }

    static long nonNegative(long value, String field) {
        if (value < 0) {
            throw invalid(field, "must not be negative");
        }
        return value;
    }

    static InvalidMqttMessageException invalid(String field, String detail) {
        return new InvalidMqttMessageException("Field '" + field + "' " + detail);
    }
}
