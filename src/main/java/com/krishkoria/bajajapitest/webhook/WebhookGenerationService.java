package com.krishkoria.bajajapitest.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for performing the startup POST to generate a webhook.
 */
@Service
public class WebhookGenerationService {
    private static final Logger log = LoggerFactory.getLogger(WebhookGenerationService.class);

    private final WebClient webClient;
    private final WebhookGenerateProperties properties;
    private final WebhookContext webhookContext;

    public WebhookGenerationService(WebClient.Builder builder,
                                    WebhookGenerateProperties properties,
                                    WebhookContext webhookContext) {
        this.webClient = builder.build();
        this.properties = properties;
        this.webhookContext = webhookContext;
    }

    public void generateAndStore() {
        if (!properties.isEnabled()) {
            log.info("Webhook generation disabled by configuration.");
            return;
        }
        GenerateWebhookRequest request = properties.toRequest();
        log.info("Generating webhook via POST {} for regNo={} email={}", properties.getUrl(), request.regNo(), request.email());
        try {
            Map response = webClient.post()
                    .uri(properties.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .doOnError(err -> log.error("Failed calling webhook generation endpoint", err))
                    .onErrorResume(err -> Mono.empty())
                    .blockOptional()
                    .orElse(Map.of());

            if (response.isEmpty()) {
                log.warn("Webhook generation response empty or failed; downstream steps will not proceed.");
                return;
            }
            log.debug("Webhook generation raw response keys: {}", response.keySet());
            URI webhookUrl = (URI) extractWebhookUrl(response).orElse(null);
            String token = extractToken(response).orElse(null).toString();
            if (webhookUrl == null) {
                log.warn("Could not locate webhook URL in response; keys: {}", response.keySet());
            } else {
                log.info("Obtained webhook URL: {}", webhookUrl);
            }
            if (token == null) {
                log.warn("No JWT token found in response (expected maybe token/jwt/jwtToken).");
            } else {
                log.info("Captured JWT token (hidden).");
            }
            webhookContext.set(new WebhookContext.StoredWebhook(webhookUrl, token, response));
        } catch (Exception e) {
            log.error("Unexpected error generating webhook", e);
        }
    }

    private Optional<URI> extractWebhookUrl(Map<String, Object> response) {
        for (String key : response.keySet()) {
            if (key.toLowerCase().contains("webhook") || key.toLowerCase().contains("callback")) {
                Object val = response.get(key);
                if (val instanceof String s && s.startsWith("http")) {
                    try {
                        return Optional.of(URI.create(s));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        // generic scan for any http URL string value
        return response.values().stream()
                .filter(v -> v instanceof String s && s.startsWith("http"))
                .map(v -> {
                    try {
                        return URI.create(v.toString());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Optional<String> extractToken(Map<String, Object> response) {
        for (String key : response.keySet()) {
            String lk = key.toLowerCase();
            if (lk.contains("token") || lk.contains("jwt")) {
                Object val = response.get(key);
                if (val instanceof String s && !s.isBlank()) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }
}
