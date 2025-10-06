package com.krishkoria.bajajapitest;

import com.krishkoria.bajajapitest.webhook.WebhookGenerateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({WebhookGenerateProperties.class})
public class BajajApiTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BajajApiTestApplication.class, args);
    }

}
