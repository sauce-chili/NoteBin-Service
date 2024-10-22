package vstu.isd.notebin;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class NoteBinApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(NoteBinApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }
}
