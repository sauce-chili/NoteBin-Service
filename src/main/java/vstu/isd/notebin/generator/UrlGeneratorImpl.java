package vstu.isd.notebin.generator;

import java.util.stream.Stream;

public class UrlGeneratorImpl {

    private String URL_PREFIX = "https://urlShortenerProject/";

    Base62HashGenerator hashGenerator;

    public Stream<String> generateUrl(int amount) {

        Stream<String> hashes = hashGenerator.generateHashes(amount);

        return hashes.
                map(hash -> URL_PREFIX + hash);
    }

    public String generateUrl() {

        return generateUrl(1).findFirst().get();
    }
}
