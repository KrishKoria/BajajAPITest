package com.krishkoria.bajajapitest.solution;

import com.krishkoria.bajajapitest.webhook.WebhookContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
        if (stored == null) {
            log.error("Cannot submit final query: webhook context empty.");
            return;
        }
        String targetUrl = stored.webhookUrl() != null ? stored.webhookUrl().toString() : properties.getUrl();
        if (stored.webhookUrl() != null) {
            log.info("Using dynamically generated webhook URL (stored): {}", targetUrl);
        } else {
            log.warn("Stored webhook URL is null; falling back to configured submission URL: {}", targetUrl);
        }
        String rawToken = stored.jwtToken();
        if (rawToken == null || rawToken.isBlank()) {
            log.error("Cannot submit final query: JWT token not available.");
            return;
        }

        Map<String, Object> body = Map.of("finalQuery", query);
        log.info("Preparing final query submission (length={} chars) to {}", query.length(), targetUrl);

        // 1st attempt: raw token (no Bearer)
        boolean success = attemptSubmission(targetUrl, body, rawToken, false);
        if (!success) {
            // If first attempt fails with 401 and token did not already start with Bearer, retry with Bearer schema
            if (!rawToken.startsWith("Bearer ")) {
                log.info("Retrying submission with 'Bearer ' prefix added to token.");
                attemptSubmission(targetUrl, body, "Bearer " + rawToken, true);
            }
        }
    }

    private boolean attemptSubmission(String url, Map<String,Object> body, String authHeader, boolean isSecondAttempt) {
        try {
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .doOnError(err -> log.error("Final query submission failed", err))
                    .onErrorResume(err -> Mono.empty())
                    .blockOptional().orElse("");
            if (response.isBlank()) {
                log.warn("Submission completed but empty response body returned (attempt {} - auth header starts with: {}).", isSecondAttempt ? 2 : 1, prefix(authHeader));
            } else {
                log.info("Submission response (truncated to 500 chars) [attempt {}]: {}", isSecondAttempt ? 2 : 1, response.substring(0, Math.min(response.length(), 500)));
            }
            return true; // treat empty as success to avoid infinite concerns
        } catch (WebClientResponseException wcre) {
            if (wcre.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Received 401 Unauthorized on attempt {} (auth header starts with: {}).", isSecondAttempt ? 2 : 1, prefix(authHeader));
                return false;
            }
            log.error("HTTP error during submission attempt {}: {}", isSecondAttempt ? 2 : 1, wcre.getStatusCode(), wcre);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during submission attempt {}", isSecondAttempt ? 2 : 1, e);
            return false;
        }
    }

    private String prefix(String authHeader) {
        if (authHeader == null) return "null";
        return authHeader.length() <= 15 ? authHeader : authHeader.substring(0, 15) + "...";
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
