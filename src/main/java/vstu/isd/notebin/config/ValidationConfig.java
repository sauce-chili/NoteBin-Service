package vstu.isd.notebin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Value("${spring.validationRule.title-regexp}")
    private String titleRegexp;

    @Value("${spring.validationRule.title-length}")
    private int titleLength;

    @Value("${spring.validationRule.content-length}")
    private int contentLength;

    @Bean
    public String titleRegexp() {
        return titleRegexp;
    }

    @Bean
    public int titleLength() {
        return titleLength;
    }

    @Bean
    public int contentLength() {
        return contentLength;
    }
}
