package com.vehicletelematics.backend.mqtt.outbound;

import com.vehicletelematics.backend.devices.service.DesiredConfigChangedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttDesiredConfigPublisher {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public MqttDesiredConfigPublisher(
            @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel,
            ObjectMapper objectMapper) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DesiredConfigChangedEvent event) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schema_version", 1);
        payload.put("revision", event.version());
        payload.set("configuration", event.config());
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload.toString())
                .setHeader(MqttHeaders.TOPIC, "v1/devices/" + event.deviceId() + "/config/desired")
                .setHeader(MqttHeaders.QOS, 1)
                .setHeader(MqttHeaders.RETAINED, true)
                .build());
    }
}
