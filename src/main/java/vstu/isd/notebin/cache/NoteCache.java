package vstu.isd.notebin.cache;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.cache.util.CASUpdate;
import vstu.isd.notebin.entity.NoteCacheable;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * {@code NoteCache} a component responsible for managing a cache of notes in a Redis cache.
 * <p>
 * This class provides methods to perform cache operations such as read,
 * transaction update with Optimistic Lock, and deletion of cached notes. It supports features like
 * setting default expiration times.
 * </p>
 */
@Component
public class NoteCache {

    private final RedisTemplate<String, NoteCacheable> redisTemplate;
    private final NoteCacheHeater cacheHeater;
    private final int CAPACITY;
    private final Duration DEFAULT_TTL;

    NoteCache(
            RedisTemplate<String, NoteCacheable> redisTemplate,
            @Qualifier("cacheNoteCapacity") int capacity,
            Duration defaultTTL,
            NoteCacheHeater cacheHeater
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheHeater = cacheHeater;
        CAPACITY = capacity;
        DEFAULT_TTL = defaultTTL;
    }

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().multiSet(
                cacheHeater.getMostUsedNotes(CAPACITY).stream()
                        .collect(Collectors.toMap(NoteCacheable::getUrl, note -> note))
        );
    }

    /**
     * Retrieves a note from the cache by its URL.
     *
     * @param url the key (URL) of the note to retrieve
     * @return an {@link Optional} containing the note if found, or empty if not
     */
    public Optional<NoteCacheable> get(String url) {
        NoteCacheable note = redisTemplate.opsForValue().get(url);
        return note != null ? Optional.of(note) : Optional.empty();
    }

    /**
     * Retrieves a note from the cache and resets its TTL to the specified duration.
     *
     * @param url the key (URL) of the note to retrieve
     * @param ttl the new time-to-live duration
     * @return an {@link Optional} containing the note if found, or empty if not
     */
    public Optional<NoteCacheable> getAndExpire(String url, Duration ttl) {
        NoteCacheable note = redisTemplate.opsForValue().getAndExpire(url, ttl);
        if (note == null) {
            return Optional.empty();
        }

        return Optional.of(note);
    }

    /**
     * Retrieves a note from the cache and resets its TTL to the default duration.
     *
     * @param url the key (URL) of the note to retrieve
     * @return an {@link Optional} containing the note if found, or empty if not
     */
    public Optional<NoteCacheable> getAndExpire(String url) {
        return getAndExpire(url, DEFAULT_TTL);
    }

    /**
     * Saves a note to the cache if it is not already present.
     *
     * @param note the {@link NoteCacheable} object to save
     * @return {@code true} if the note was successfully saved, {@code false} if it already exists
     */
    public boolean save(NoteCacheable note) {
        return Boolean.TRUE.equals(
                DEFAULT_TTL != null ?
                        redisTemplate.opsForValue().setIfAbsent(
                                note.getUrl(),
                                note,
                                DEFAULT_TTL
                        )
                        :
                        redisTemplate.opsForValue().setIfAbsent(
                                note.getUrl(),
                                note
                        )
        );
    }

    /**
     * Updates a note in the cache if it is present.
     *
     * @param url      the key (URL) of the updatable note
     * @param modifier a function that modifies the note stored by the key {@code url}
     * @return the updated note
     * @throws NoSuchElementException  if the note with the specified key does not exist
     * @throws OptimisticLockException if the note was updated by another process/transaction
     */
    public NoteCacheable update(String url, UnaryOperator<NoteCacheable> modifier) {
        return updateWithOLE(
                url,
                ops -> {
                    NoteCacheable note = ops.opsForValue().get(url);
                    if (note == null) {
                        throw new NoSuchElementException("Note with key `" + url + "` not found");
                    }
                    return note;
                },
                (persisted, ops) -> {

                    NoteCacheable updated = modifier.apply(persisted);
                    // If the key doesn't exist, there will be no update.
                    Boolean updateSuccess = ops.opsForValue().setIfPresent(url, updated);

                    if (Boolean.FALSE.equals(updateSuccess)) {
                        throw new NoSuchElementException("Note with key `" + url + "` not found");
                    }
                    return updated;
                }
        );
    }

    private NoteCacheable updateWithOLE(
            String lockKey,
            Function<RedisOperations<String, NoteCacheable>, NoteCacheable> provider,
            BiFunction<NoteCacheable, RedisOperations<String, NoteCacheable>, NoteCacheable> modifier
    ) {

        Supplier<NoteCacheable> doIfUpdateFailed = () -> {
            throw new OptimisticLockException();
        };

        return redisTemplate.execute(new CASUpdate<>(
                lockKey,
                provider,
                modifier,
                doIfUpdateFailed
        ));
    }

    /**
     * Deletes a note from the cache.
     *
     * @param url the key (URL) of the deletable note
     * @return the deleted note or {@code null} if isn't present
     */
    public NoteCacheable deleteNote(String url) {
        return redisTemplate.opsForValue().getAndDelete(url);
    }
}