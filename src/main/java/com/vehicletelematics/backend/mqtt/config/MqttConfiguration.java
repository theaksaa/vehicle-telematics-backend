package com.vehicletelematics.backend.mqtt.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttConfiguration {

    static final String[] SUBSCRIBED_TOPICS = {
            "v1/devices/+/telemetry",
            "v1/devices/+/state",
            "v1/devices/+/config/reported"
    };

    @Bean
    DefaultMqttPahoClientFactory mqttClientFactory(MqttProperties properties) {
        if (properties.getServerUri() == null || !properties.getServerUri().startsWith("ssl://")) {
            throw new IllegalStateException("MQTT_SERVER_URI must use ssl:// for mutual TLS");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getServerUri()});
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setHttpsHostnameVerificationEnabled(true);
        options.setConnectionTimeout(properties.getConnectionTimeoutSeconds());
        options.setKeepAliveInterval(properties.getKeepAliveSeconds());
        options.setSocketFactory(MqttTlsSocketFactory.create(properties));

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttProperties properties,
            DefaultMqttPahoClientFactory clientFactory,
            @Qualifier("mqttInboundChannel") MessageChannel mqttInboundChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-in", clientFactory, SUBSCRIBED_TOPICS);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel);
        return adapter;
    }

    @Bean
    MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    MqttPahoMessageHandler mqttOutboundHandler(
            MqttProperties properties,
            DefaultMqttPahoClientFactory clientFactory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                properties.getClientId() + "-out", clientFactory);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(true);
        return handler;
    }

    @Bean
    org.springframework.integration.endpoint.EventDrivenConsumer mqttOutboundEndpoint(
            @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel,
            @Qualifier("mqttOutboundHandler") MessageHandler mqttOutboundHandler) {
        return new org.springframework.integration.endpoint.EventDrivenConsumer(
                (org.springframework.messaging.SubscribableChannel) mqttOutboundChannel,
                mqttOutboundHandler);
    }
}
