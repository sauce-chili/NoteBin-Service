package vstu.isd.notebin.util;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UrlGeneratorImpl implements UrlGenerator {

    @Override
    public String generateUrl() {
        return UUID.randomUUID().toString();
    }
}
