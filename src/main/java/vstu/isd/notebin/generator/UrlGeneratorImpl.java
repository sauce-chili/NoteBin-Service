package vstu.isd.notebin.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class UrlGeneratorImpl implements UrlGenerator {

    private String URL_PREFIX = "https://urlShortenerProject/";

    private final Base62HashGenerator hashGenerator;

    @Override
    public Stream<String> generateUrls(int amount) {

        Stream<String> hashes = hashGenerator.generateHashes(amount);

        return hashes;
    }

    @Override
    public String generateUrl() {

        int AMOUNT = 1;
        return generateUrls(AMOUNT).findFirst().get();
    }
}
