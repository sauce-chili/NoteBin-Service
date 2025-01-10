package vstu.isd.notebin;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableRetry
public class NoteBinApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(NoteBinApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }
}
