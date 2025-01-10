package vstu.isd.notebin.repository;

import java.util.List;

public interface UniqueRangeRepository {
    List<Long> getNextUniqueRange(int size);
}
