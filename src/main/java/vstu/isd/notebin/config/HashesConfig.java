package vstu.isd.notebin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HashesConfig {
    @Value("${spring.hashes.hash-pool-size}")
    private int hashPoolSize;

    @Value("${spring.hashes.generation-thread-pool-size}")
    private int generationThreadPoolSize;

    @Value("${spring.hashes.pool-exhaustion-percentage:30}")
    private int exhaustionPercentageExhaustion;

    @Value("${spring.hashes.hash-cache-size}")
    private int hashCacheSize;

    @Value("${spring.hashes.cache-exhaustion-percentage:20}")
    private int cacheExhaustionPercentage;

    @Bean
    public int HASH_POOL_SIZE() {
        return hashPoolSize;
    }

    @Bean
    public int EXHAUSTION_POOL_PERCENTAGE() {
        return exhaustionPercentageExhaustion;
    }

    @Bean
    public ExecutorService hashGeneratorThreadPool() {
        return Executors.newFixedThreadPool(generationThreadPoolSize);
    }

    @Bean
    public int hashCacheSize() {
        return hashCacheSize;
    }

    @Bean
    public int cacheExhaustionPercentage() {
        return cacheExhaustionPercentage;
    }
}
