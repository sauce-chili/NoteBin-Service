package vstu.isd.notebin.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class TestContainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Container
    public PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    private final static int REDIS_PORT = 6379;
    @Container
    public RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(REDIS_PORT);

    public void initialize(ConfigurableApplicationContext ctx) {
        postgresContainer.start();
        redisContainer.start();

        System.setProperty("SPRING_DATASOURCE_URL", postgresContainer.getJdbcUrl());
        System.setProperty("SPRING_DATASOURCE_USERNAME", postgresContainer.getUsername());
        System.setProperty("SPRING_DATASOURCE_PASSWORD", postgresContainer.getPassword());
        System.setProperty("SPRING_REDIS_HOST", redisContainer.getHost());
        System.setProperty("SPRING_REDIS_PORT", String.valueOf(redisContainer.getMappedPort(REDIS_PORT)));
    }
}
