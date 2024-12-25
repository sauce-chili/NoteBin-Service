package vstu.isd.notebin.testutils;

import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestAsserts {
    public static void assertNoteDtoEquals(NoteDto expected, NoteDto actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getContent(), actual.getContent());
        assertEquals(expected.isAvailable(), actual.isAvailable());
        assertEquals(expected.getExpirationType(), actual.getExpirationType());
        assertLocalDateTimeEquals(expected.getCreatedAt(), actual.getCreatedAt());
        assertLocalDateTimeEquals(expected.getExpirationFrom(), actual.getExpirationFrom());
        assertDurationEquals(expected.getExpirationPeriod(), actual.getExpirationPeriod());
    }

    public static void assertNoteDtoEqualsWithTimeError(NoteDto expected, NoteDto actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getContent(), actual.getContent());
        assertEquals(expected.isAvailable(), actual.isAvailable());
        assertEquals(expected.getExpirationType(), actual.getExpirationType());
        assertDateTimeEqualsNowWithOneSecondError(expected.getCreatedAt(), actual.getCreatedAt());
        assertDateTimeEqualsNowWithOneSecondError(expected.getExpirationFrom(), actual.getExpirationFrom());
        assertDurationEquals(expected.getExpirationPeriod(), actual.getExpirationPeriod());
    }

    public static void assertDateTimeEqualsNowWithOneSecondError(LocalDateTime expected, LocalDateTime actual) {
        Duration difference = Duration.between(expected, actual);

        assertEquals(0, difference.getSeconds());
    }

    private static void assertLocalDateTimeEquals(LocalDateTime expected, LocalDateTime actual) {
        if (expected != null && actual != null) {
            assertTrue(Duration.between(expected, actual).getSeconds() < 1);
        } else if (expected == null && actual == null) {
            assertEquals(expected, actual);
        } else {
            fail();
        }
    }

    private static void assertDurationEquals(Duration expected, Duration actual) {
        if (expected != null && actual != null) {
            assertTrue(expected.minus(actual).getSeconds() < 1);
        } else if (expected == null && actual == null) {
            assertEquals(expected, actual);
        } else {
            fail();
        }
    }

    public static void assertNoteExistsInRepository(NoteDto noteDto, NoteRepository noteRepository) {
        Optional<Note> noteInRepository = noteRepository.findByUrl(noteDto.getUrl());
        assertTrue(noteInRepository.isPresent());
    }

    public static void assertNoteExistsInCache(NoteDto noteDto, NoteCache noteCache) {
        Optional<NoteCacheable> noteInCache = noteCache.get(noteDto.getUrl());
        assertTrue(noteInCache.isPresent());
    }
}
