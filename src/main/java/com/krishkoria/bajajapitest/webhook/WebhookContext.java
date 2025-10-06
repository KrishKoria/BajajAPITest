package com.krishkoria.bajajapitest.webhook;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the webhook generation response so later steps (SQL resolution + submission) can use it.
 */
@Component
public class WebhookContext {

    public record StoredWebhook(URI webhookUrl, String jwtToken, Map<String, Object> raw) {}

    private final AtomicReference<StoredWebhook> ref = new AtomicReference<>();

    public void set(StoredWebhook value) {
        this.ref.set(value);
    }

    public Optional<StoredWebhook> get() {
        return Optional.ofNullable(ref.get());
    }
}

