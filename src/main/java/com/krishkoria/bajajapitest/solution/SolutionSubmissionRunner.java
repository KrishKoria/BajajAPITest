package com.krishkoria.bajajapitest.solution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Executes after the webhook generation step to submit the final SQL query (step 4).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // run after other high precedence startup tasks
public class SolutionSubmissionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SolutionSubmissionRunner.class);
    private final SolutionSubmissionService service;
    private final SolutionSubmissionProperties properties;

    public SolutionSubmissionRunner(SolutionSubmissionService service,
                                    SolutionSubmissionProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return; // fast exit if disabled
        }
        long delay = properties.getDelayMs();
        if (delay > 0) {
            log.info("Delaying final SQL submission by {} ms (configured solution.submit.delay-ms).", delay);
            Thread t = new Thread(() -> {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                service.submitIfConfigured();
            }, "final-sql-submission-thread");
            t.setDaemon(true);
            t.start();
        } else {
            service.submitIfConfigured();
        }
    }
}

