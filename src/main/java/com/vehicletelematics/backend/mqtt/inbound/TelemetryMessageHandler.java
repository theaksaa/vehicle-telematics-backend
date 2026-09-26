package com.vehicletelematics.backend.mqtt.inbound;

import com.vehicletelematics.backend.telemetry.service.TelemetryMessage;
import com.vehicletelematics.backend.telemetry.service.TelemetryService;
import com.vehicletelematics.backend.websocket.dto.TelemetryWebSocketMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.Instant;

import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.invalid;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.nonNegative;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.optionalDouble;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.optionalFloat;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.optionalInteger;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.optionalShort;
import static com.vehicletelematics.backend.mqtt.inbound.MqttPayloadFields.requiredLong;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryMessageHandler {

    private final TelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;

    public TelemetryMessageHandler(
            TelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate) {
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
    }

    public void handle(String deviceId, JsonNode json) {
        long protocolVersion = requiredLong(json, "v");
        if (protocolVersion != 1) {
            throw invalid("v", "must be 1");
        }
        long bootId = nonNegative(requiredLong(json, "boot_id"), "boot_id");
        long sequenceNumber = nonNegative(requiredLong(json, "seq"), "seq");
        Instant recordedAt;
        try {
            recordedAt = Instant.ofEpochSecond(requiredLong(json, "ts"));
        } catch (DateTimeException exception) {
            throw invalid("ts", "is outside the supported epoch range");
        }
        Instant receivedAt = Instant.now();

        TelemetryMessage message = new TelemetryMessage(
                (short) protocolVersion,
                bootId,
                sequenceNumber,
                recordedAt,
                receivedAt,
                optionalDouble(json, "lat", -90, 90),
                optionalDouble(json, "lon", -180, 180),
                optionalFloat(json, "gnss_speed_kph", 0, 500),
                optionalFloat(json, "speed_kph", 0, 500),
                optionalInteger(json, "rpm", 0, 20_000),
                optionalFloat(json, "accelerator_pct", 0, 100),
                optionalShort(json, "rssi_dbm", -200, 50));

        telemetryService.store(deviceId, message).ifPresent(telemetry ->
                messagingTemplate.convertAndSend(
                        "/topic/devices/" + deviceId + "/telemetry",
                        TelemetryWebSocketMessage.from(telemetry)));
    }
}
