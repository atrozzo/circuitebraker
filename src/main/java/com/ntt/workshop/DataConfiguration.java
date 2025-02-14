package com.ntt.workshop;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConfiguration {

    @Bean
    public Queue myQueue() {
        return new Queue("my-queue", true);
    }
}
