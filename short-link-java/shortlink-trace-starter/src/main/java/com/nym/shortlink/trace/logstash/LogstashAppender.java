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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    @Value("${trace.logstash.host:shortlink-log-go}")
    private String logstashHost;

    @Value("${trace.logstash.port:8081}")
    private String logstashPort;

    @Value("${trace.logstash.enabled:true}")
    private boolean enabled;

    private String applicationName = "shortlink-unknown";

    private final BlockingQueue<Map<String, Object>> logQueue = new LinkedBlockingQueue<>(10000);
    private final ObjectMapper objectMapper;

    public LogstashAppender() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        // Resolve service name from env var first, then Spring property, then fallback
        String envName = System.getenv("SPRING_APPLICATION_NAME");
        String propName = System.getProperty("spring.application.name");
        if (envName != null && !envName.isBlank()) {
            applicationName = envName;
        } else if (propName != null && !propName.isBlank()) {
            applicationName = propName;
        }
        if (enabled) {
            logger.info("Logstash appender initialized, service={}, target: {}:{}", applicationName, logstashHost, logstashPort);
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {
                @Override
                protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
                    if (!enabled || event.getLoggerName().equals(LogstashAppender.class.getName())) return;
                    String level = event.getLevel().toString();
                    String message = event.getFormattedMessage();
                    ch.qos.logback.classic.spi.IThrowableProxy tp = event.getThrowableProxy();
                    if (tp != null) {
                        message += "\n" + ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(tp);
                    }
                    Map<String, String> mdc = event.getMDCPropertyMap();
                    LogstashAppender.this.append(level, message, mdc);
                }
            };
            appender.setContext(rootLogger.getLoggerContext());
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    public void append(String level, String message, Map<String, String> mdc) {
        if (!enabled) {
            return;
        }

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", level);
        logEntry.put("timestamp", FORMATTER.format(Instant.now()));
        logEntry.put("service", applicationName);
        String msg = message != null && !message.trim().isEmpty() ? message : "-";
        logEntry.put("message", msg);

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

            String json = objectMapper.writeValueAsString(logs);
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


}