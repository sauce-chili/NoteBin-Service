package vstu.isd.notebin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Value("${spring.validationRule.title-regexp}")
    private String titleRegexp;

    @Value("${spring.validationRule.content-regexp}")
    private String contentRegexp;

    @Value("${spring.validationRule.title-length}")
    private int titleLength;

    @Bean
    public String titleRegexp() {
        return titleRegexp;
    }

    @Bean
    public String contentRegexp() {
        return contentRegexp;
    }

    @Bean
    public int titleLength() {
        return titleLength;
    }
}
