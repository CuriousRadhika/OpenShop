package com.openshop.orderservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig paymentServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // Circuit opens when 50% of calls fail
                .failureRateThreshold(50)
                // Calls taking longer than 3s are considered slow
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                // Circuit opens when 100% of calls are slow
                .slowCallRateThreshold(100)
                // Number of calls to record in the sliding window
                .slidingWindowSize(10)
                // Minimum number of calls before calculating error rate
                .minimumNumberOfCalls(5)
                // Wait 30s in open state before transitioning to half-open
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Allow 3 test calls in half-open state
                .permittedNumberOfCallsInHalfOpenState(3)
                // Automatically transition from open to half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Record these exceptions as failures
                .recordExceptions(
                        feign.FeignException.class,
                        java.io.IOException.class,
                        java.util.concurrent.TimeoutException.class
                )
                .build();
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("Circuit breaker '{}' added", circuitBreaker.getName());
                
                circuitBreaker.getEventPublisher()
                        .onSuccess(event -> log.debug("Circuit breaker '{}' recorded a successful call", 
                                circuitBreaker.getName()))
                        .onError(event -> log.warn("Circuit breaker '{}' recorded an error: {}", 
                                circuitBreaker.getName(), event.getThrowable().getMessage()))
                        .onStateTransition(event -> log.warn("Circuit breaker '{}' state changed from {} to {}", 
                                circuitBreaker.getName(), event.getStateTransition().getFromState(), 
                                event.getStateTransition().getToState()))
                        .onSlowCallRateExceeded(event -> log.warn("Circuit breaker '{}' slow call rate exceeded: {}%", 
                                circuitBreaker.getName(), event.getSlowCallRate()))
                        .onFailureRateExceeded(event -> log.warn("Circuit breaker '{}' failure rate exceeded: {}%", 
                                circuitBreaker.getName(), event.getFailureRate()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.info("Circuit breaker '{}' removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit breaker '{}' replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}
