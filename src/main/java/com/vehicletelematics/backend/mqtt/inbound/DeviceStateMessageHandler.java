package com.vehicletelematics.backend.mqtt.inbound;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.domain.DeviceStatus;
import com.vehicletelematics.backend.devices.service.DeviceService;
import com.vehicletelematics.backend.websocket.dto.DeviceStateWebSocketMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Locale;

import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.invalid;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.optionalNonNegativeLong;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class DeviceStateMessageHandler {

    private final DeviceService deviceService;
    private final SimpMessagingTemplate messagingTemplate;

    public DeviceStateMessageHandler(
            DeviceService deviceService,
            SimpMessagingTemplate messagingTemplate) {
        this.deviceService = deviceService;
        this.messagingTemplate = messagingTemplate;
    }

    public void handle(String deviceId, JsonNode json) {
        JsonNode statusNode = json.get("status");
        if (statusNode == null || !statusNode.isTextual()) {
            throw invalid("status", "is required and must be a string");
        }
        DeviceStatus status;
        try {
            status = DeviceStatus.valueOf(statusNode.asText().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("status", "must be online or offline");
        }

        Long bootId = optionalNonNegativeLong(json, "boot_id");
        Device device = deviceService.reportState(deviceId, status, bootId, Instant.now());
        messagingTemplate.convertAndSend(
                "/topic/devices/" + deviceId + "/state",
                new DeviceStateWebSocketMessage(
                        deviceId, device.getStatus(), device.getLastSeenAt(), device.getLastBootId()));
    }
}
