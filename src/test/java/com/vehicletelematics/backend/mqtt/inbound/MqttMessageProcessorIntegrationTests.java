package com.vehicletelematics.backend.mqtt.inbound;

import com.vehicletelematics.backend.devices.domain.DeviceConfigState;
import com.vehicletelematics.backend.devices.domain.DeviceStatus;
import com.vehicletelematics.backend.devices.service.DeviceService;
import com.vehicletelematics.backend.telemetry.repository.TelemetryRepository;
import com.vehicletelematics.backend.telemetry.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MqttMessageProcessorIntegrationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private TelemetryRepository telemetryRepository;

    private List<Message<?>> outboundMessages;
    private MqttMessageProcessor processor;

    @BeforeEach
    void setUp() {
        outboundMessages = new ArrayList<>();
        ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
        brokerChannel.subscribe(outboundMessages::add);
        SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(brokerChannel);
        processor = new MqttMessageProcessor(
                objectMapper,
                new TelemetryMessageHandler(telemetryService, messagingTemplate),
                new DeviceStateMessageHandler(deviceService, messagingTemplate),
                new ReportedConfigMessageHandler(deviceService, messagingTemplate));
    }

    @Test
    void storesValidTelemetryOnlyOnceAndForwardsItToWebSocket() {
        deviceService.create("mqtt-device-01", null);
        String payload = """
                {
                  "v": 1,
                  "boot_id": 1588733874,
                  "ts": 1789929100,
                  "seq": 0,
                  "lat": 43.997007,
                  "lon": 20.855162,
                  "gnss_speed_kph": 0.5,
                  "speed_kph": 0.0,
                  "rpm": 780,
                  "accelerator_pct": 14.5,
                  "rssi_dbm": -51
                }
                """;

        processor.handle(payload, "v1/devices/mqtt-device-01/telemetry");
        processor.handle(payload, "v1/devices/mqtt-device-01/telemetry");

        assertThat(telemetryRepository.count()).isEqualTo(1);
        var saved = telemetryRepository.findAll().getFirst();
        assertThat(saved.getRecordedAt().getEpochSecond()).isEqualTo(1789929100);
        assertThat(saved.getReceivedAt()).isNotNull();
        assertThat(saved.getLatitude()).isEqualTo(43.997007);
        assertThat(outboundMessages).hasSize(1);
        assertThat(outboundMessages.getFirst().getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER))
                .isEqualTo("/topic/devices/mqtt-device-01/telemetry");
    }

    @Test
    void allowsMissingGpsAndCanFieldsButRejectsOutOfRangeTelemetry() {
        deviceService.create("mqtt-device-02", null);

        processor.handle("""
                {"v":1,"boot_id":7,"ts":1789929100,"seq":1,"rssi_dbm":-70}
                """, "v1/devices/mqtt-device-02/telemetry");
        processor.handle("""
                {"v":1,"boot_id":7,"ts":1789929101,"seq":2,"lat":0,"lon":181}
                """, "v1/devices/mqtt-device-02/telemetry");

        assertThat(telemetryRepository.count()).isEqualTo(1);
        var saved = telemetryRepository.findAll().getFirst();
        assertThat(saved.getLatitude()).isNull();
        assertThat(saved.getVehicleSpeedKph()).isNull();
    }

    @Test
    void appliesOnlineAndOfflineStateIndependentlyFromTelemetry() {
        deviceService.create("mqtt-device-03", null);

        processor.handle("""
                {"status":"online","boot_id":1588733874}
                """, "v1/devices/mqtt-device-03/state");

        var online = deviceService.findByDeviceId("mqtt-device-03");
        assertThat(online.getStatus()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(online.getLastBootId()).isEqualTo(1588733874L);
        assertThat(online.getLastSeenAt()).isNotNull();
        var lastSeen = online.getLastSeenAt();

        processor.handle("""
                {"status":"offline"}
                """, "v1/devices/mqtt-device-03/state");

        var offline = deviceService.findByDeviceId("mqtt-device-03");
        assertThat(offline.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        assertThat(offline.getLastSeenAt()).isEqualTo(lastSeen);
    }

    @Test
    void appliesReportedConfigurationEnvelope() {
        deviceService.create("mqtt-device-04", null);
        deviceService.updateDesiredConfig(
                "mqtt-device-04",
                JsonNodeFactory.instance.objectNode().put("samplingIntervalSeconds", 10));

        processor.handle("""
                {"schema_version":1,"revision":1,"configuration":{"samplingIntervalSeconds":10}}
                """, "v1/devices/mqtt-device-04/config/reported");

        var device = deviceService.findByDeviceId("mqtt-device-04");
        assertThat(device.getReportedConfigVersion()).isEqualTo(1);
        assertThat(device.getConfigState()).isEqualTo(DeviceConfigState.APPLIED);
    }
}
