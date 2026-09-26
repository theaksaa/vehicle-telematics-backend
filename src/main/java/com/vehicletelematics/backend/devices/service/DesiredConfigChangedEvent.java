package com.vehicletelematics.backend.devices.service;

import tools.jackson.databind.JsonNode;

public record DesiredConfigChangedEvent(String deviceId, long version, JsonNode config) {
}
