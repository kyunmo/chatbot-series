package io.moyam.chatbot.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final Environment env;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", env.getProperty("spring.application.name"));
        health.put("profiles", env.getActiveProfiles());

        // 데이터베이스 연결 체크
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", "Connected");
        } catch (Exception e) {
            health.put("database", "Disconnected");
            health.put("status", "DOWN");
            log.error("Database connection failed", e);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        Map<String, String> version = new HashMap<>();
        version.put("version", "1.0.0-SNAPSHOT");
        version.put("build", "2024.01.15");
        version.put("description", "Moyam ChatBot API");

        return ResponseEntity.ok(version);
    }
}
