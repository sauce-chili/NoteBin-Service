package vstu.isd.notebin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Value("${spring.validation-rule.title.regex}")
    private String titleRegex;

    @Value("${spring.validationRule.title.max-length}")
    private int titleLength;

    @Value("${spring.validationRule.content.length}")
    private int contentLength;

    @Bean
    public String titleRegexp() {
        return titleRegex;
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
