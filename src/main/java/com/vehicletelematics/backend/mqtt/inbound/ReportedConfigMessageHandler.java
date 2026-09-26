package com.vehicletelematics.backend.mqtt.inbound;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.service.DeviceService;
import com.vehicletelematics.backend.websocket.dto.ReportedConfigWebSocketMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.invalid;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.nonNegative;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.requiredLong;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class ReportedConfigMessageHandler {

    private final DeviceService deviceService;
    private final SimpMessagingTemplate messagingTemplate;

    public ReportedConfigMessageHandler(
            DeviceService deviceService,
            SimpMessagingTemplate messagingTemplate) {
        this.deviceService = deviceService;
        this.messagingTemplate = messagingTemplate;
    }

    public void handle(String deviceId, JsonNode json) {
        long schemaVersion = requiredLong(json, "schema_version");
        if (schemaVersion != 1) {
            throw invalid("schema_version", "must be 1");
        }
        long version = nonNegative(requiredLong(json, "revision"), "revision");
        JsonNode config = json.get("configuration");
        Device device = deviceService.reportConfig(deviceId, version, config);
        messagingTemplate.convertAndSend(
                "/topic/devices/" + deviceId + "/config/reported",
                new ReportedConfigWebSocketMessage(
                        deviceId,
                        device.getReportedConfigVersion(),
                        device.getReportedConfig(),
                        device.getReportedConfigAt(),
                        device.getConfigState()));
    }
}
