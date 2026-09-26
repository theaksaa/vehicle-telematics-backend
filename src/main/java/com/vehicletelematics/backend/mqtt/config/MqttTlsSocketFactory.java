package com.vehicletelematics.backend.mqtt.config;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

final class MqttTlsSocketFactory {

    private static final char[] KEY_PASSWORD = new char[0];

    private MqttTlsSocketFactory() {
    }

    static SSLSocketFactory create(MqttProperties properties) {
        Path clientCertificate = requiredPath(
                properties.getClientCertificate(), "MQTT_CLIENT_CERTIFICATE");
        Path clientPrivateKey = requiredPath(
                properties.getClientPrivateKey(), "MQTT_CLIENT_PRIVATE_KEY");

        try {
            X509Certificate[] clientChain = readCertificates(clientCertificate);
            PrivateKey privateKey = readPrivateKey(clientPrivateKey);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("mqtt-backend", privateKey, KEY_PASSWORD, clientChain);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_PASSWORD);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            if (properties.getCaCertificate() == null || properties.getCaCertificate().isBlank()) {
                trustManagerFactory.init((KeyStore) null);
            } else {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                X509Certificate[] trustedCertificates = readCertificates(
                        Path.of(properties.getCaCertificate()));
                for (int index = 0; index < trustedCertificates.length; index++) {
                    trustStore.setCertificateEntry("mqtt-ca-" + index, trustedCertificates[index]);
                }
                trustManagerFactory.init(trustStore);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Cannot initialize MQTT mutual TLS: " + exception.getMessage(), exception);
        }
    }

    private static Path requiredPath(String configuredPath, String environmentVariable) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must point to a PEM file");
        }
        Path path = Path.of(configuredPath);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(environmentVariable + " is not a readable file: " + path);
        }
        return path;
    }

    private static X509Certificate[] readCertificates(Path path)
            throws IOException, GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates;
        try (InputStream input = Files.newInputStream(path)) {
            certificates = certificateFactory.generateCertificates(input);
        }
        if (certificates.isEmpty()) {
            throw new GeneralSecurityException("No X.509 certificate found in " + path);
        }
        List<X509Certificate> result = new ArrayList<>(certificates.size());
        for (Certificate certificate : certificates) {
            result.add((X509Certificate) certificate);
        }
        return result.toArray(X509Certificate[]::new);
    }

    private static PrivateKey readPrivateKey(Path path) throws IOException, GeneralSecurityException {
        String pem = Files.readString(path, StandardCharsets.US_ASCII);
        String beginMarker = "-----BEGIN PRIVATE KEY-----";
        String endMarker = "-----END PRIVATE KEY-----";
        int begin = pem.indexOf(beginMarker);
        int end = pem.indexOf(endMarker);
        if (begin < 0 || end <= begin) {
            throw new GeneralSecurityException(
                    "Private key must be an unencrypted PKCS#8 PEM file generated by openssl genpkey: " + path);
        }
        String base64 = pem.substring(begin + beginMarker.length(), end).replaceAll("\\s", "");
        byte[] encoded;
        try {
            encoded = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException exception) {
            throw new GeneralSecurityException("Invalid PEM private key: " + path, exception);
        }

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        for (String algorithm : List.of("RSA", "EC")) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            } catch (GeneralSecurityException ignored) {
                // Try the next supported key algorithm.
            }
        }
        throw new GeneralSecurityException("Unsupported PKCS#8 private key algorithm in " + path);
    }
}
