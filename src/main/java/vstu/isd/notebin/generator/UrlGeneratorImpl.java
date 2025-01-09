package vstu.isd.notebin.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.cache.HashCache;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class UrlGeneratorImpl implements UrlGenerator {

    private final HashCache hashCache;

    @Override
    public String generateUrl() {

        int AMOUNT = 1;
        return hashCache.getHash();
    }
}
