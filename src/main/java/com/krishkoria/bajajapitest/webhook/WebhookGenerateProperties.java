package com.krishkoria.bajajapitest.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties controlling the webhook generation POST made at startup.
 */
@ConfigurationProperties(prefix = "webhook.generate")
public class WebhookGenerateProperties {
    /** Enable/disable the startup POST call (disabled in tests). */
    private boolean enabled = true;
    /** Full URL for the POST request. */
    private String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private String name = "John Doe";
    private String regNo = "REG12347";
    private String email = "john@example.com";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public GenerateWebhookRequest toRequest() {
        return new GenerateWebhookRequest(name, regNo, email);
    }
}

