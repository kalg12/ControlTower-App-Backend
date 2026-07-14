package com.controltower.app.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {

    private String apiKey;
    private String fromEmail = "noreply@controltower.io";
    private String fromName = "Control Tower";
}
