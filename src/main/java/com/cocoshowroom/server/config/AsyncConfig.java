package com.cocoshowroom.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's {@code @Async} support.
 *
 * <p>With Java 21 virtual threads active ({@code spring.threads.virtual.enabled=true}),
 * the default {@link org.springframework.core.task.SimpleAsyncTaskExecutor} runs each
 * async invocation on a new virtual thread — no thread-pool tuning required for v1.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
