package vstu.isd.notebin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import vstu.isd.notebin.entity.NoteCacheable;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.connection_factory}")
    private String factoryType;

    @Bean
    @Lazy
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(host, port);
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    @Lazy
    public LettuceConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    @Primary
    public RedisConnectionFactory connectionFactory() {
        return switch (factoryType) {
            case "jedis" -> jedisConnectionFactory();
            case "lettuce" -> lettuceConnectionFactory();
            default -> throw new IllegalStateException("Unexpected value: " + factoryType);
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    RedisTemplate<String, NoteCacheable> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        return redisTemplateBuilder(
                connectionFactory,
                objectMapper,
                NoteCacheable.class
        );
    }

    private <T> RedisTemplate<String, T> redisTemplateBuilder(
            RedisConnectionFactory connectionFactory,
            ObjectMapper mapper,
            Class<T> type
    ) {
        RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        redisTemplate.setKeySerializer(keySerializer);

        Jackson2JsonRedisSerializer<T> valueSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, type);

        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }
}
