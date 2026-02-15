package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Slf4j
@Configuration
@Profile("dev")
public class SwaggerAutoOpen {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    ApplicationRunner openSwaggerOnStartup() {
        return args -> {
            String url = "http://localhost:" + serverPort + "/swagger-ui/index.html";
            openBrowser(url);
        };
    }

    private void openBrowser(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
            log.info("Opening Swagger UI: {}", url);
        } catch (IOException e) {
            log.warn("Could not open browser automatically: {}", e.getMessage());
        }
    }
}
