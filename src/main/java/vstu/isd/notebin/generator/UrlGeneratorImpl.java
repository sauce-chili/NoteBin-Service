package vstu.isd.notebin.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.cache.HashCache;

@Component
@RequiredArgsConstructor
public class UrlGeneratorImpl implements UrlGenerator {

    private final HashCache hashCache;

    @Override
    public String generateUrl() {

        return hashCache.getHash();
    }
}
