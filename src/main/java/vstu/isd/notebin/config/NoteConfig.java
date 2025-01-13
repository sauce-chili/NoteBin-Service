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
    @Value("${spring.note.heater-page-size}")
    private int heaterPageSize;
    @Value("${spring.note.note-page-size}")
    private int notePageSize;

    @Bean
    public int cacheNoteCapacity() {
        return cacheCapacity;
    }

    @Bean
    public Duration defaultTTL() {
        return defaultTTL;
    }

    @Bean
    public int heaterPageSize() {
        return heaterPageSize;
    }

    @Bean
    public String userIdHeaderAttribute() {
        return "x-user-id";
    }

    @Bean
    public int notePageSize() {
        return notePageSize;
    }
}
