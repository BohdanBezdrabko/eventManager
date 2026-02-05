package com.example.sportadministrationsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WhatsAppGraphClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.graph.base-url}") private String baseUrl;
    @Value("${whatsapp.graph.version}")  private String version;
    @Value("${whatsapp.phone-number-id}") private String phoneNumberId;
    @Value("${whatsapp.access-token}") private String accessToken;

    public void sendText(String to, String text) {
        String url = baseUrl + "/" + version + "/" + phoneNumberId + "/messages";

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        post(url, payload);
    }

    public void sendReplyButtons(String to, String bodyText, List<Button> buttons) {
        String url = baseUrl + "/" + version + "/" + phoneNumberId + "/messages";

        var action = new LinkedHashMap<String, Object>();
        action.put("buttons", buttons.stream().limit(3).map(b -> Map.of(
                "type", "reply",
                "reply", Map.of(
                        "id", b.id(),
                        "title", b.title()
                )
        )).toList());

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", bodyText),
                        "action", action
                )
        );

        post(url, payload);
    }

    private void post(String url, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (HttpStatusCodeException e) {
            log.error("WhatsApp API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("WhatsApp API call failed: {}", e.getMessage(), e);
        }
    }

    public record Button(String id, String title) {}
}
