package vstu.isd.notebin.generator;

import java.util.stream.Stream;

public interface UrlGenerator {

    Stream<String> generateUrls(int amount);

    String generateUrl();
}
