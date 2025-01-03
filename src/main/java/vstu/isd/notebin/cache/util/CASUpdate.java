package vstu.isd.notebin.cache.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.support.atomic.RedisAtomicDouble;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The {@code CASUpdate} class implements a compare-and-set (CAS) mechanism using Redis transactions.
 * It provides a way to perform <u>atomic</u> updates to a Redis key by watching it, applying modifications,
 * and committing changes only if the key has not been modified by another process during the transaction.
 *
 * @param <K> the type of the Redis key.
 * @param <V> the type of the Redis value.
 *            <p>
 *            The CAS operation ensures thread-safe modifications to Redis keys and values.
 *            <p>
 *            This class uses the Spring Redis `SessionCallback` interface to manage the transaction.
 *
 *            <p><strong>Key Components:</strong></p>
 *             <ul>
 *               <li>{@code keyProvider} - Supplies the Redis key to set an {@literal WATCH} on a value.</li>
 *               <li>
 *                   {@code valueProvider} -
 *                   The function to form the context needed for updating.
 *                   <p>
 *                   The parameter of the function is {@link RedisOperations}{@code <K,V>}, which can be used to provide the updatable value.
 *                   <p>
 *                   <strong><u>IMPORTANT</u></strong>: it is strongly recommended not to use any write-operations, they will lead to errors and the actions will not be rollback
 *               </li>
 *               <li>
 *                   {@code modifier} - A function to modify provided value.
 *                   <p>
 *                   The first parameter {@code modifier} is provided by {@code valueProvider} value.
 *                   <p>
 *                   The second parameter {@code modifier} is {@link RedisOperations}{@code <K,V>}, which can be used to manipulate with value.
 *                   <p>
 *                   <strong><u>IMPORTANT</u></strong>: all read-operations through the {@code RedisOperations<K,V>} object will return null or exception.
 *                   <p>
 *                   All context about the state of the system should be provided in the {@code valueProvider}
 *               </li>
 *               <li>
 *                   {@code doIfChanged} - A supplier to execute an alternative action if the transaction fails.
 *               </li>
 *             </ul>
 *
 *             <p><strong>Usage:</strong></p>
 *             <ol>
 *               <li>
 *               Create an instance of {@code CASUpdate} by providing the key {@code keyProvider} to the entity observable,
 *               describe how to obtain the entity using {@code valueProvider},
 *               describe the modification actions {@code modifier},
 *               specify what to do if the transaction fails in {@code doIfChanged}.
 *               </li>
 *               <li>Execute it within a Redis template session to perform the CAS operation.</li>
 *             </ol>
 *            <p>
 *            Example:
 *             <pre>
 *             {@code
 *             CASUpdate<String, String> casUpdate = new CASUpdate<>(
 *                 "myKey",
 *                 redisOps -> redisOps.opsForValue().get("myKey"),
 *                 (currentValue, redisOps) -> {
 *                     String newValue = currentValue + "_updated";
 *                     redisOps.opsForValue().set("myKey", newValue);
 *                     return newValue;
 *                 },
 *                 () -> "Transaction failed"
 *             );
 *
 *             redisTemplate.execute(casUpdate);
 *             }
 *             </pre>
 *            </p>
 * @author of this shit Dmitry Smirnov and Katerina Aleeva
 * @see org.springframework.data.redis.support.atomic.CompareAndSet
 * @see SessionCallback
 * @see RedisAtomicDouble
 */
@Slf4j
@RequiredArgsConstructor
public class CASUpdate<K, V> implements SessionCallback<V> {

    private final Supplier<K> keyProvider;
    private final Function<RedisOperations<K, V>, V> valueProvider;
    private final BiFunction<V, RedisOperations<K, V>, V> modifier;
    private final Supplier<V> doIfChanged;

    public CASUpdate(K key, Function<RedisOperations<K, V>, V> valueProvider, BiFunction<V, RedisOperations<K, V>, V> modifier, Supplier<V> doIfChanged) {
        this(() -> key, valueProvider, modifier, doIfChanged);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <KK, VV> V execute(RedisOperations<KK, VV> operations) throws DataAccessException {

        RedisOperations<K, V> ops = (RedisOperations<K, V>) operations;

        K lockKey = keyProvider.get();
        ops.watch(lockKey);
//        log.info("CASUpdate watch on {} thread: {}", lockKey,Thread.currentThread());

        V persisted = valueProvider.apply(ops);

        ops.multi();

        V updated = modifier.apply(persisted, ops);

        if (successUpdate(ops.exec())) {
//            log.info("CASUpdate success for thread: {}", Thread.currentThread());
            return updated;
        }
//        log.info("CASUpdate failed for thread: {}", Thread.currentThread());
        ops.unwatch();
        return doIfChanged.get();
    }

    private static boolean successUpdate(Collection<?> execResult) {
        return CollectionUtils.isNotEmpty(execResult);
    }
}
