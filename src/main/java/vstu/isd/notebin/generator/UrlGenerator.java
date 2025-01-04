package vstu.isd.notebin.generator;

import java.util.List;
import java.util.stream.Stream;

public interface UrlGenerator {

    List<String> generateUrls(int amount);

    String generateUrl();
}