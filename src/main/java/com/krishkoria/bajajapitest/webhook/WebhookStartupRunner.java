package com.krishkoria.bajajapitest.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Triggers the webhook generation once the Spring context is ready.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebhookStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookStartupRunner.class);
    private final WebhookGenerationService service;

    public WebhookStartupRunner(WebhookGenerationService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting webhook generation step (step 1).");
        service.generateAndStore();
    }
}

