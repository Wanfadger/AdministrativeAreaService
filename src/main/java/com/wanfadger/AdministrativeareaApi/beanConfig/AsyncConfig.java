package com.wanfadger.AdministrativeareaApi.beanConfig;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for optimized thread pool management
 * 
 * This configuration provides better control over async processing
 * compared to the default Spring async executor
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Custom async executor with optimized thread pool settings
     * 
     * Configuration based on:
     * - Core pool size: Base number of threads
     * - Max pool size: Maximum threads under load
     * - Queue capacity: Buffer for pending tasks
     * 
     * Adjust these values based on:
     * - Expected async workload
     * - Available CPU cores
     * - Memory constraints
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(10);
        
        // Max pool size: maximum number of threads
        executor.setMaxPoolSize(50);
        
        // Queue capacity: buffer for tasks when all threads are busy
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("async-adminArea-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time for shutdown
        executor.setAwaitTerminationSeconds(60);
        
        // Reject policy: throw exception when queue is full
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // Log async exceptions
            System.err.println("Async method execution failed: " + method.getName());
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        };
    }
}
