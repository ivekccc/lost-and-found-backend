package com.example.demo.shared.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
@Component
public class FlywayConfig {

    @Value("${app.flyway.seed-enabled}")
    private boolean devSeedEnabled;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.flyway.username:}")
    private String migrationUsername;

    @Value("${app.flyway.password:}")
    private String migrationPassword;

    @PostConstruct
    public void migrate() {
        List<String> locations = new ArrayList<>(List.of("classpath:db/migration", "classpath:db/seed"));
        if (devSeedEnabled) {
            locations.add("classpath:db/dev-seed");
        }
        boolean separateOwner = StringUtils.hasText(migrationUsername);
        Flyway.configure()
                .dataSource(url, separateOwner ? migrationUsername : username, separateOwner ? migrationPassword : password)
                .locations(locations.toArray(String[]::new))
                .ignoreMigrationPatterns("*:future", "repeatable:missing")
                .baselineOnMigrate(true)
                .cleanDisabled(true)
                .load()
                .migrate();
    }
}
