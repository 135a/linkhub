package com.nym.shortlink.trace.logstash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@EnableAsync
@Component
public class LogstashAppender {

    private static final Logger logger = LoggerFactory.getLogger(LogstashAppender.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    @Value("${trace.logstash.host:shortlink-log-go}")
    private String logstashHost;

    @Value("${trace.logstash.port:8081}")
    private String logstashPort;

    @Value("${trace.logstash.enabled:true}")
    private boolean enabled;

    private final BlockingQueue<Map<String, Object>> logQueue = new LinkedBlockingQueue<>(10000);
    private final ObjectMapper objectMapper;

    public LogstashAppender() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            logger.info("Logstash appender initialized, target: {}:{}", logstashHost, logstashPort);
        }
    }

    public void append(String level, String message, Map<String, String> mdc) {
        if (!enabled) {
            return;
        }

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", level);
        logEntry.put("timestamp", FORMATTER.format(Instant.now()));
        logEntry.put("service", getServiceName());
        logEntry.put("message", message);
        logEntry.put("@timestamp", Instant.now().toString());

        if (mdc != null) {
            logEntry.putAll(mdc);
        }

        if (!logQueue.offer(logEntry)) {
            logger.warn("Log queue full, dropping log entry");
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void flush() {
        if (!enabled || logQueue.isEmpty()) {
            return;
        }

        List<Map<String, Object>> batch = new ArrayList<>();
        logQueue.drainTo(batch, 100);

        if (!batch.isEmpty()) {
            sendToLogstash(batch);
        }
    }

    private void sendToLogstash(List<Map<String, Object>> logs) {
        try {
            URL url = new URL(String.format("http://%s:%s/api/v1/logs/ingest", logstashHost, logstashPort));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = objectMapper.writeValueAsString(Map.of("logs", logs));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Failed to send logs to logstash, response code: {}", responseCode);
            }
        } catch (Exception e) {
            logger.error("Error sending logs to logstash: {}", e.getMessage());
        }
    }

    private String getServiceName() {
        String name = System.getProperty("spring.application.name");
        return name != null ? name : "shortlink-unknown";
    }
}