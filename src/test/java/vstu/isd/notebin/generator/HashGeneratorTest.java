package vstu.isd.notebin.generator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vstu.isd.notebin.config.TestContainersConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
public class HashGeneratorTest {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private final static ExecutorService executors = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);

    @AfterAll
    public static void afterAll() {
        executors.shutdownNow();
    }

    @Autowired
    HashGenerator hashGenerator;

    @Test
    public void testConcurrentGenerateUniqueHashes() {

        int tasksCount = MAXIMUM_POOL_SIZE; // one task per thread
        int hashCountPerTask = 1000; // count generated hashes per task

        // run concurrent hash generation
        List<CompletableFuture<Stream<String>>> futures = Stream.generate(
                        () -> CompletableFuture.supplyAsync(
                                () -> hashGenerator.generateHashes(hashCountPerTask), executors
                        )
                )
                .limit(tasksCount) // constraint required task count
                .toList();

        // await all running tasksCount
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> resultHashes = futures.stream()
                .flatMap(CompletableFuture::join) // merging in common Stream
                .toList();


        int expectedTotalUniqueHashes = tasksCount * hashCountPerTask;
        assertEquals(
                expectedTotalUniqueHashes,
                resultHashes.size(),
                "The count of generated hashes doesn't match the expected one"
        );

        long uniqueHashesCount = resultHashes.stream().distinct().count();
        assertEquals(
                expectedTotalUniqueHashes,
                uniqueHashesCount,
                "Generated hashes aren't unique"
        );
    }
}
