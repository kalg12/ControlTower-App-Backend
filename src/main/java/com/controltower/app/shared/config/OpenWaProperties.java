package com.controltower.app.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openwa")
public class OpenWaProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:2785";
    private String apiKey;
    private String session;
    private String devGroupId;
}
