package vstu.isd.notebin.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.entity.NoteCacheable;


import java.util.List;

@Component
@RequiredArgsConstructor
public class NoteCacheHeater {

    public List<NoteCacheable> getMostUsedNotes(int amount) {
        return List.of(); // TODO implement
    }
}
