package com.krishkoria.bajajapitest.solution;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for step 4: submitting the final SQL query back to the hiring API.
 */
@ConfigurationProperties(prefix = "solution.submit")
public class SolutionSubmissionProperties {
    private boolean enabled = true;
    /** Endpoint for submission (step 4). */
    private String url = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
    private String finalQuery = "";
    private long delayMs = 0L;
    private long waitForTokenTimeoutMs = 10000L;
    private long waitForTokenPollMs = 300L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFinalQuery() { return finalQuery; }
    public void setFinalQuery(String finalQuery) { this.finalQuery = finalQuery; }
    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
    public long getWaitForTokenTimeoutMs() { return waitForTokenTimeoutMs; }
    public void setWaitForTokenTimeoutMs(long waitForTokenTimeoutMs) { this.waitForTokenTimeoutMs = waitForTokenTimeoutMs; }
    public long getWaitForTokenPollMs() { return waitForTokenPollMs; }
    public void setWaitForTokenPollMs(long waitForTokenPollMs) { this.waitForTokenPollMs = waitForTokenPollMs; }
}
