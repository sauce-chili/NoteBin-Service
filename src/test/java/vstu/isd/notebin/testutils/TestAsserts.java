package vstu.isd.notebin.testutils;

import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.NotePreviewDto;
import vstu.isd.notebin.dto.NoteViewResponseDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.entity.ViewNote;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static vstu.isd.notebin.util.UtilFunc.durationAreEquals;
import static vstu.isd.notebin.util.UtilFunc.localDateTimeAreEquals;

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

    public static void assertNoteCacheableEquals(NoteCacheable expected, NoteCacheable actual) {
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
            assertTrue(localDateTimeAreEquals(expected, actual));
        } else if (expected == null && actual == null) {
            assertTrue(true);
        } else {
            fail();
        }
    }

    private static void assertDurationEquals(Duration expected, Duration actual) {
        if (expected != null && actual != null) {
            assertTrue(durationAreEquals(expected, actual));
        } else if (expected == null && actual == null) {
            assertTrue(true);
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
    }

    public static void assertNotePreviewDtoEquals(NotePreviewDto expected, NotePreviewDto actual) {
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getExpirationType(), actual.getExpirationType());
        assertLocalDateTimeEquals(expected.getExpirationFrom(), actual.getExpirationFrom());
        assertDurationEquals(expected.getExpirationPeriod(), actual.getExpirationPeriod());
    }
}
