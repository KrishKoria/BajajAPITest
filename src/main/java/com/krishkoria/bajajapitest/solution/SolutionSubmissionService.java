package com.krishkoria.bajajapitest.solution;

import com.krishkoria.bajajapitest.webhook.WebhookContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Handles step 4: submitting the final SQL query using the JWT token acquired in step 1.
 */
@Service
public class SolutionSubmissionService {
    private static final Logger log = LoggerFactory.getLogger(SolutionSubmissionService.class);

    private final WebClient webClient;
    private final WebhookContext webhookContext;
    private final SolutionSubmissionProperties properties;

    public SolutionSubmissionService(WebClient.Builder builder,
                                     WebhookContext webhookContext,
                                     SolutionSubmissionProperties properties) {
        this.webClient = builder.build();
        this.webhookContext = webhookContext;
        this.properties = properties;
    }

    public void submitIfConfigured() {
        if (!properties.isEnabled()) {
            log.info("Solution submission disabled (solution.submit.enabled=false).");
            return;
        }
        String query = Optional.ofNullable(properties.getFinalQuery()).map(String::trim).orElse("");
        if (query.isBlank()) {
            log.warn("Final query is blank; aborting submission.");
            return;
        }
        waitForTokenIfNeeded();
        var stored = webhookContext.get().orElse(null);
        if (stored == null || stored.jwtToken() == null || stored.jwtToken().isBlank()) {
            log.error("Cannot submit final query: JWT token not available.");
            return;
        }
        try {
            log.info("Submitting final query to {} (length={} chars)", properties.getUrl(), query.length());
            Map<String,Object> body = Map.of("finalQuery", query);
            String response = webClient.post()
                    .uri(properties.getUrl())
                    .header(HttpHeaders.AUTHORIZATION, stored.jwtToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .doOnError(err -> log.error("Final query submission failed", err))
                    .onErrorResume(err -> Mono.empty())
                    .blockOptional().orElse("");
            if (response.isBlank()) {
                log.warn("Submission completed but empty response body returned.");
            } else {
                log.info("Submission response (truncated to 500 chars): {}", response.substring(0, Math.min(response.length(), 500)));
            }
        } catch (Exception e) {
            log.error("Unexpected error during final query submission", e);
        }
    }

    private void waitForTokenIfNeeded() {
        long timeoutMs = properties.getWaitForTokenTimeoutMs();
        long pollMs = Math.max(50L, properties.getWaitForTokenPollMs());
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            var stored = webhookContext.get().orElse(null);
            if (stored != null && stored.jwtToken() != null && !stored.jwtToken().isBlank()) {
                return; // token ready
            }
            try { Thread.sleep(pollMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
        }
        log.warn("Token not available after waiting {} ms; proceeding (may fail).", timeoutMs);
    }
}

