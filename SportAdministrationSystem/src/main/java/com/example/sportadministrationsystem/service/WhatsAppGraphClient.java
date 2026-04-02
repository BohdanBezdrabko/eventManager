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


    /**
     * Відправка templated message (для reminder/scheduled outside 24h window).
     * Шаблон має бути зареєстрований на Meta Business Account.
     *
     * @param to       phone number (E.164)
     * @param templateName назва шаблону (напр. "event_reminder")
     * @param parameters параметри для підстановки (напр. {{1}}, {{2}})
     */
    public void sendTemplate(String to, String templateName, List<String> parameters) {
        String url = baseUrl + "/" + version + "/" + phoneNumberId + "/messages";

        var bodyParams = new java.util.ArrayList<>();
        List<String> paramsList = parameters != null ? parameters : List.of();
        for (String param : paramsList) {
            bodyParams.add(Map.of("type", "text", "text", param));
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("type", "body");
        body.put("parameters", bodyParams);

        var template = new LinkedHashMap<String, Object>();
        template.put("name", templateName);
        template.put("language", Map.of("code", "uk"));
        template.put("components", List.of(body));

        var payload = new LinkedHashMap<String, Object>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", template);

        post(url, payload);
    }

    public void sendReplyButtons(String to, String bodyText, List<Button> buttons) {
        String url = baseUrl + "/" + version + "/" + phoneNumberId + "/messages";

        var buttonsList = new java.util.ArrayList<>();
        for (Button b : buttons) {
            if (buttonsList.size() >= 3) break;
            var buttonMap = new LinkedHashMap<String, Object>();
            buttonMap.put("type", "reply");
            var replyMap = new LinkedHashMap<String, String>();
            replyMap.put("id", b.id());
            replyMap.put("title", b.title());
            buttonMap.put("reply", replyMap);
            buttonsList.add(buttonMap);
        }

        var action = new LinkedHashMap<String, Object>();
        action.put("buttons", buttonsList);

        var interactive = new LinkedHashMap<String, Object>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", bodyText));
        interactive.put("action", action);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "interactive");
        payload.put("interactive", interactive);

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
