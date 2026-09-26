package com.vehicletelematics.backend.mqtt.inbound;

import com.vehicletelematics.backend.devices.exception.DeviceNotFoundException;
import com.vehicletelematics.backend.devices.exception.InvalidDeviceDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageProcessor.class);
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
            "^v1/devices/([a-zA-Z0-9_-]{1,64})/(telemetry|state|config/reported)$");

    private final ObjectMapper objectMapper;
    private final TelemetryMessageHandler telemetryMessageHandler;
    private final DeviceStateMessageHandler deviceStateMessageHandler;
    private final ReportedConfigMessageHandler reportedConfigMessageHandler;

    public MqttMessageProcessor(
            ObjectMapper objectMapper,
            TelemetryMessageHandler telemetryMessageHandler,
            DeviceStateMessageHandler deviceStateMessageHandler,
            ReportedConfigMessageHandler reportedConfigMessageHandler) {
        this.objectMapper = objectMapper;
        this.telemetryMessageHandler = telemetryMessageHandler;
        this.deviceStateMessageHandler = deviceStateMessageHandler;
        this.reportedConfigMessageHandler = reportedConfigMessageHandler;
    }

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(String payload, @Header(MqttHeaders.RECEIVED_TOPIC) String topic) {
        try {
            Matcher matcher = TOPIC_PATTERN.matcher(topic);
            if (!matcher.matches()) {
                throw new InvalidMqttMessageException("Unsupported MQTT topic");
            }
            String deviceId = matcher.group(1);
            String messageType = matcher.group(2);
            JsonNode json = objectMapper.readTree(payload);
            if (json == null || !json.isObject()) {
                throw new InvalidMqttMessageException("Payload must be a JSON object");
            }

            switch (messageType) {
                case "telemetry" -> telemetryMessageHandler.handle(deviceId, json);
                case "state" -> deviceStateMessageHandler.handle(deviceId, json);
                case "config/reported" -> reportedConfigMessageHandler.handle(deviceId, json);
                default -> throw new InvalidMqttMessageException("Unsupported MQTT message type");
            }
        } catch (JacksonException | InvalidMqttMessageException |
                 DeviceNotFoundException | InvalidDeviceDataException exception) {
            log.warn("Rejected MQTT message on topic {}: {}", topic, exception.getMessage());
            log.debug("Rejected MQTT message details", exception);
        }
    }
}
