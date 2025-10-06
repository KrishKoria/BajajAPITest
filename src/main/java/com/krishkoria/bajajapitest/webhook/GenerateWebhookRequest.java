package com.krishkoria.bajajapitest.webhook;

/**
 * Request payload for generating the webhook as per task instructions.
 */
public record GenerateWebhookRequest(String name, String regNo, String email) {
}
