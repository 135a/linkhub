package com.nym.shortlink.trace.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class JsonLogEncoder extends EncoderBase<ILoggingEvent> {

    private static final ObjectMapper OBJECT_MAPPER;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", event.getLevel().toString());
        logEntry.put("timestamp", FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
        logEntry.put("service", getServiceName());
        logEntry.put("thread", event.getThreadName());
        logEntry.put("logger", event.getLoggerName());
        logEntry.put("message", event.getFormattedMessage());

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && mdc.containsKey("trace_id")) {
            logEntry.put("trace_id", mdc.get("trace_id"));
        }

        try {
            String json = OBJECT_MAPPER.writeValueAsString(logEntry);
            return (json + "\n").getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ("{\"level\":\"ERROR\",\"message\":\"encode error: " + e.getMessage() + "\"}\n")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private String getServiceName() {
        String name = System.getProperty("spring.application.name");
        return name != null ? name : "shortlink-unknown";
    }

    @Override
    public void init(OutputStream outputStream) {
        super.init(outputStream);
    }

    @Override
    public void close() {
        super.close();
    }
}