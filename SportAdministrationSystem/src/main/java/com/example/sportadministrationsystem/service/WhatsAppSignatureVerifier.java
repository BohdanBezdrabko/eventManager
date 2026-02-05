package com.example.sportadministrationsystem.service;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class WhatsAppSignatureVerifier {

    public boolean isValid(String rawBody, String headerSignature, String appSecret) {
        if (appSecret == null || appSecret.isBlank()) {
            // Локально можна тестити без секрета
            return true;
        }
        if (headerSignature == null || headerSignature.isBlank()) return false;

        String[] parts = headerSignature.split("=", 2);
        if (parts.length != 2) return false;
        if (!"sha256".equalsIgnoreCase(parts[0])) return false;

        String sigHex = parts[1];
        String computed = hmacSha256Hex(appSecret, rawBody);
        return constantTimeEquals(sigHex, computed);
    }

    private String hmacSha256Hex(String secret, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return toHex(out);
        } catch (Exception e) {
            return "";
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
