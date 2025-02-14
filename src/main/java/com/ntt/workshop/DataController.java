package com.ntt.workshop;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
public class DataController {



    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BACKEND_SERVICE = "backendService";

    @GetMapping("/data")
    @CircuitBreaker(name = BACKEND_SERVICE, fallbackMethod = "fallbackResponse")
    public String getData(@RequestParam(defaultValue = "false") boolean fail) {
        if (fail) {
            throw new RuntimeException("Simulated failure");
        }
        return restTemplate.getForObject("https://jsonplaceholder.typicode.com/posts/1", String.class);
    }

    // Fallback method when circuit breaker is open
    public String fallbackResponse(boolean fail, Throwable t) {
        return "Fallback response: Service is temporarily unavailable.";
    }


}
