package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.config.TestContainersConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
class AnalyticsServiceTest {

    @Test
    void getViewAnalytics() {

    }
}