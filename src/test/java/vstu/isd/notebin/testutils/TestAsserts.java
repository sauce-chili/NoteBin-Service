package vstu.isd.notebin.testutils;

import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.NoteViewResponseDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.entity.ViewNote;

import java.time.Duration;
import java.time.LocalDateTime;

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
        assertEquals(expected.getUserId(), actual.getUserId());
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

    public static void assertViewAnalyticsDtoEquals(ViewAnalyticsDto expected, ViewAnalyticsDto actual) {
        assertEquals(expected.getUserViews(), actual.getUserViews());
        assertEquals(expected.getAnonymousViews(), actual.getAnonymousViews());
    }

    public static void assertNoteViewResponseDtoEquals(NoteViewResponseDto expected, NoteViewResponseDto actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getNoteId(), actual.getNoteId());
        assertEquals(expected.getUserId(), actual.getUserId());
    }

    public static void assertViewNoteEquals(ViewNote expected, ViewNote actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getNoteId(), actual.getNoteId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertLocalDateTimeEquals(expected.getViewedAt(), actual.getViewedAt());
    };
}
