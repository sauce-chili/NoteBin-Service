package vstu.isd.notebin.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.service.HashService;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class LocalHashCache implements HashCache {

    private final HashService hashService;
    private final int CAPACITY;
    private final int EXHAUSTION_PERCENTAGE;

    private final Queue<String> cache;

    private final AtomicBoolean isFilling;

    public LocalHashCache(
            HashService hashService,
            @Qualifier("hashCacheSize") int capacity,
            @Qualifier("cacheExhaustionPercentage") int exhaustionPercentage
    ) {
        this.hashService = hashService;
        CAPACITY = capacity;
        EXHAUSTION_PERCENTAGE = exhaustionPercentage;
        cache = new ArrayBlockingQueue<>(CAPACITY);
        isFilling = new AtomicBoolean(false);
    }

    @PostConstruct
    public void init() {
        cache.addAll(
                hashService.getHashes(CAPACITY)
        );
    }

    @Retryable(
            retryFor = {NoSuchElementException.class},
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    @Override
    public String getHash() {

        if (isCacheExhausted()) {
            if (isFilling.compareAndExchange(false, true)) {
                fillCacheAsync();
            }
        }

        String hash = cache.poll();

        if (hash == null) {
            log.error("Cache is empty");
            throw new NoSuchElementException("Cache is empty");
        }

        return hash;
    }

    private boolean isCacheExhausted() {
        return cache.size() <= CAPACITY * EXHAUSTION_PERCENTAGE / 100;
    }

    private void fillCacheAsync() {
        hashService.getHashesAsync(CAPACITY)
                .thenAccept(cache::addAll)
                .thenRun(() -> isFilling.set(false));
    }
}
