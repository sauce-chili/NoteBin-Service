package vstu.isd.notebin.generator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.cache.HashCache;
import vstu.isd.notebin.entity.Hash;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UrlGeneratorImpl implements UrlGenerator {

    private final HashCache hashCache;

    @Override
    public String generateUrl() {
        return hashCache.getHash();
    }
}
