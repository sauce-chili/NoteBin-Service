package vstu.isd.notebin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NoteConfig {
    @Value("${spring.note.cache-capacity}")
    private int cacheCapacity;
    @Value("${spring.note.default-ttl}")
    private Duration defaultTTL;

    @Bean
    public int cacheNoteCapacity() {
        return cacheCapacity;
    }

    @Bean
    public Duration defaultTTL() {
        return defaultTTL;
    }
}
