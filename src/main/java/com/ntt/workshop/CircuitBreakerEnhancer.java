package com.ntt.workshop;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
@Slf4j
public class CircuitBreakerEnhancer {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Synchronized Map to store method names and their circuit breaker states
    private final Map<String, CircuitBreaker.State> circuitBreakerStateMap = new HashMap<>();

    public CircuitBreakerEnhancer(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Pointcut("within(@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker *)")
    public void circuitBreakerClasses() {
        // Empty body – the method doesn't need to contain any logic, it's just a declaration
    }

    /**
     * Pointcut to match all methods in classes annotated with @CircuitBreaker.
     */
    @Around("within(@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker *)")
    public Object checkCircuitBreakerState(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return Optional.ofNullable(method.getAnnotation(io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class))
                .map(annotation -> handleCircuitBreaker(joinPoint, method, annotation))
                .orElseGet(() -> proceedWithExecution(joinPoint));
    }

    private Object handleCircuitBreaker(ProceedingJoinPoint joinPoint, Method method, io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker annotation) {
        String methodName = method.getName();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(annotation.name());

        // Update and check the state of the circuit breaker for the method
        updateCircuitBreakerState(methodName, circuitBreaker.getState());

        // Retrieve the current circuit breaker state from the map before execution
        CircuitBreaker.State currentState = getCircuitBreakerStateForMethod(methodName);

        // If any circuit breaker is OPEN, block method execution
        if (currentState == CircuitBreaker.State.OPEN) {
            log.info("[AOP] Circuit Breaker for method '{}' is OPEN. Execution blocked.", methodName);
            return "Circuit Breaker is OPEN. Request blocked.";
        }

        // Proceed with method execution if the state is not OPEN
        return proceedWithExecution(joinPoint);
    }

    private void updateCircuitBreakerState(String methodName, CircuitBreaker.State state) {
        synchronized (circuitBreakerStateMap) {
            circuitBreakerStateMap.put(methodName, state);
        }
    }

    private boolean isAnyCircuitBreakerOpen() {
        return circuitBreakerStateMap.values().stream()
                .anyMatch(state -> state == CircuitBreaker.State.OPEN);
    }

    private Object proceedWithExecution(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            log.error("[AOP] Exception while executing method", throwable);
            return "Error during execution";
        }
    }

    /**
     * Pre-processing logic before the actual method execution.
     */
    @Before("circuitBreakerClasses()")
    public void beforeCircuitBreakerExecution(JoinPoint joinPoint) {
        logMethodExecution("Before execution", joinPoint);
    }

    /**
     * Post-processing logic after successful execution.
     */
    @AfterReturning(pointcut = "circuitBreakerClasses()", returning = "result")
    public void afterSuccessfulExecution(JoinPoint joinPoint, Object result) {
        logMethodExecution("Successfully executed", joinPoint);
        log.info("[AOP] Response: {}", result);
    }

    /**
     * Handles exceptions thrown by methods in CircuitBreaker-annotated classes.
     */
    @AfterThrowing(pointcut = "circuitBreakerClasses()", throwing = "ex")
    public void afterExceptionThrown(JoinPoint joinPoint, Throwable ex) {
        logMethodExecution("Exception in method", joinPoint);
        log.error("[AOP] Exception Message: {}", ex.getMessage());
    }

    private void logMethodExecution(String action, JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("[AOP] {} method: {}", action, methodName);
    }

    /**
     * Returns the state of a circuit breaker for a particular method.
     */
    public synchronized CircuitBreaker.State getCircuitBreakerStateForMethod(String methodName) {
        return Optional.ofNullable(circuitBreakerStateMap.get(methodName))
                .orElse(CircuitBreaker.State.CLOSED); // Default to CLOSED if not present
    }
}



