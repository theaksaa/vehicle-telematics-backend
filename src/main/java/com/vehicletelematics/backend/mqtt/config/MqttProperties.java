package com.vehicletelematics.backend.mqtt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("app.mqtt")
public class MqttProperties {

    private boolean enabled = true;
    private String serverUri = "ssl://localhost:8883";
    private String clientId = "vehicle-telematics-backend";
    private String caCertificate;
    private String clientCertificate;
    private String clientPrivateKey;
    private int connectionTimeoutSeconds = 10;
    private int keepAliveSeconds = 30;

}
