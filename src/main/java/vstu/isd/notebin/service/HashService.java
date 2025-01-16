package vstu.isd.notebin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.entity.Hash;
import vstu.isd.notebin.generator.HashGenerator;
import vstu.isd.notebin.repository.HashRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class HashService {

    private final HashGenerator hashGenerator;
    private final HashRepository hashRepository;
    private final int HASH_POOL_SIZE;
    private final int EXHAUSTION_POOL_PERCENTAGE;

    @Async("hashGeneratorThreadPool")
    @Transactional
    public CompletableFuture<List<String>> getHashesAsync(int amount) {
        return CompletableFuture.completedFuture(getHashes(amount));
    }

    @Transactional
    public List<String> getHashes(int amount) {

        if (amount > HASH_POOL_SIZE) {
            // TODO mb invoke directly hashGenerator
            throw new IllegalArgumentException("amount can't be greater than the hash-pool size: " + HASH_POOL_SIZE);
        }

        List<Hash> hashes = hashRepository.popAll(amount);

        if (hashes.size() < amount) {
            fillHashPoolIfNecessary();

            int lacking = amount - hashes.size();
            hashes.addAll(hashRepository.popAll(lacking));
        } else {
            fillHashPoolIfNecessaryAsync();
        }

        return hashes.stream()
                .map(Hash::getHashId)
                .toList();
    }

    // TODO 1: set lock on repository method instead of isolation
    // TODO 2: add some lock from concurrent invoke to this method. Mb `ReentrantLock`
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRES_NEW
    )
    public void fillHashPoolIfNecessary() {
        /*
         * IMPORTANT:
         * Concurrency locking is not used due to transaction isolation level `Isolation.SERIALIZABLE`,
         * which embeds all transactions into an "order". When changing the isolation level, add race condition protection
         *
         * NOTE:
         * There may be excessive waste of resources on connecting to the DB,
         * in which case it is necessary to set a lock against concurrent invoke to this method.
         * */
        int actualHashesCount = (int) hashRepository.count();

        if (isHashPoolExhausted(actualHashesCount)) {
            int lacking = HASH_POOL_SIZE - actualHashesCount;
            generateAndSaveHashes(lacking);
        }
    }

    private boolean isHashPoolExhausted(int actualHashesCount) {
        return actualHashesCount <= HASH_POOL_SIZE * EXHAUSTION_POOL_PERCENTAGE / 100;
    }

    private void generateAndSaveHashes(int amount) {
        List<Hash> urlHashes = hashGenerator.generateHashes(amount)
                .map(Hash::new)
                .toList();

        hashRepository.saveAll(urlHashes);
    }

    @Async("hashGeneratorThreadPool")
    @Transactional(propagation = Propagation.SUPPORTS)
    public void fillHashPoolIfNecessaryAsync() {
        fillHashPoolIfNecessary();
    }
}
