package com.ntt.workshop;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataConsumer {

    private static final String CIRCUIT_BREAKER_NAME = "rabbitMqConsumer";

    @RabbitListener(queues = "my-queue")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "handleFailure")
    public void receiveMessage(String message) {
        log.info("Received message: {}", message);

        // Simulate an error
        if ("error".equalsIgnoreCase(message)) {
            log.error("Message NOT Processed: {}", message);

            throw new RuntimeException("Simulated failure while processing message");
        }
        log.info("Message processed successfully: {}", message);
    }

    // Fallback method when the circuit breaker is open
    public void handleFailure(String message, Throwable t) {
        log.info("Fallback triggered: Unable to process message: " + message);
    }
}
