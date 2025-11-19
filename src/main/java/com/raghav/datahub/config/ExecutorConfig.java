package com.raghav.datahub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService indexingExecutorService(
            @Value("${datahub.indexing.core-pool-size:4}") int corePoolSize,
            @Value("${datahub.indexing.max-pool-size:16}") int maxPoolSize,
            @Value("${datahub.indexing.queue-capacity:100}") int queueCapacity
    ) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy() // prevent silent loss
        );
    }
}
