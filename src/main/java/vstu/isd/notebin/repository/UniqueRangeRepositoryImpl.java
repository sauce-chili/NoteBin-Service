package vstu.isd.notebin.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UniqueRangeRepositoryImpl implements UniqueRangeRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Long> getNextUniqueRange(int size) {
        String q = "SELECT nextval('unique_hash_number_seq') FROM generate_series(1, ?) FOR UPDATE";
        return jdbcTemplate.queryForList(q, Long.class, size);
    }
}