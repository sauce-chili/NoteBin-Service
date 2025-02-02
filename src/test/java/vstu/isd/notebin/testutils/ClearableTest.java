package vstu.isd.notebin.testutils;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.entity.NoteCacheable;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
public class ClearableTest {
    @Autowired
    private RedisTemplate<String, NoteCacheable> redisTemplate;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp() {
        clearRedis();
        clearTables();
    }

    private void clearRedis() {
        try (var connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            connection.serverCommands().flushAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void clearTables() {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("""
                        DO $$ DECLARE
                            r RECORD;
                        BEGIN
                            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                                EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                            END LOOP;
                        END $$;
                    """.trim());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
