package vstu.isd.notebin.generator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UrlGeneratorImpl implements UrlGenerator {
    @Override
    public String generateUrl() {
        return UUID.randomUUID().toString();
    }
}
