package com.kurobytes.accounts.service;

import com.kurobytes.accounts.dto.AccountCreatedEvent;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.messaging.MessageDeliveryException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {
    private final StreamBridge streamBridge;
    private final MeterRegistry meterRegistry;

    @Async("eventTaskExecutor")
    @Retry(name = "event-publish", maxAttempts = 3)
    public CompletableFuture<Boolean> publishAccountCreatedEvent(AccountCreatedEvent event) {
        try {
            String eventId = UUID.randomUUID().toString();
            event.setEventId(eventId);
            event.setTimestamp(Instant.now());

            boolean result = streamBridge.send("account-created-events", event);

            if (result) {
                meterRegistry.counter("events.published", "event.type", "account.created").increment();
                log.info("Successfully published account created event: {}", eventId);
            } else {
                meterRegistry.counter("events.failed", "event.type", "account.created").increment();
                log.error("Failed to publish account created event: {}", eventId);
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error publishing account created event", e);
            return CompletableFuture.completedFuture(false);
        }
    }
} 