package vstu.isd.notebin.entity;

import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@ToString
public abstract class BaseNote {
    abstract public Long getId();
    abstract public void setId(Long id);

    abstract public String getTitle();
    abstract public void setTitle(String title);

    abstract public String getContent();
    abstract public void setContent(String content);

    abstract public LocalDateTime getCreatedAt();
    abstract public void setCreatedAt(LocalDateTime createdAt);

    abstract public String getUrl();
    abstract public void setUrl(String url);

    abstract public ExpirationType getExpirationType();
    abstract public void setExpirationType(ExpirationType expirationType);

    abstract public Duration getExpirationPeriod();
    abstract public void setExpirationPeriod(Duration expirationPeriod);

    abstract public LocalDateTime getExpirationFrom();
    abstract public void setExpirationFrom(LocalDateTime expirationFrom);

    abstract public boolean isAvailable();

    abstract public Long getUserId();
    abstract public void setUserId(Long userId);

    public boolean isNotAvailable() {
        return !isAvailable();
    }

    abstract public void setAvailable(boolean available);

    public boolean isExpired() {
        return switch (getExpirationType()) {
            case NEVER -> false;
            case BURN_AFTER_READ -> !isAvailable();
            case BURN_BY_PERIOD -> LocalDateTime.now().isAfter(expireAt());
        };
    }

    public LocalDateTime expireAt() {
        return getExpirationFrom().plus(getExpirationPeriod());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof BaseNote other)) return false;

        if (!getId().equals(other.getId())) return false;
        if (!getTitle().equals(other.getTitle())) return false;
        if (!getContent().equals(other.getContent())) return false;
        if (!localDateTimeAreEquals(getCreatedAt(), other.getCreatedAt())) return false;
        if (!getUrl().equals(other.getUrl())) return false;
        if (getExpirationType() != other.getExpirationType()) return false;
        if (!durationAreEquals(getExpirationPeriod(), other.getExpirationPeriod())) return false;
        if (!localDateTimeAreEquals(getExpirationFrom(), other.getExpirationFrom())) return false;
        return isAvailable() == other.isAvailable();
    }

    // only second or microseconds
    private final static ChronoUnit PRECISION_UNIT = ChronoUnit.SECONDS;
    private final static long TEMP_AMOUNT_PRECISION = 1;

    private static boolean localDateTimeAreEquals(LocalDateTime first, LocalDateTime second) {
        if (first == second) {
            return true;
        } else if (first != null && second != null) {
            return Duration.between(first, second).get(PRECISION_UNIT) < TEMP_AMOUNT_PRECISION;
        }
        return false;
    }

    private static boolean durationAreEquals(Duration first, Duration second) {
        if (first == second) {
            return true;
        } else if (first != null && second != null) {
            return first.minus(second).get(PRECISION_UNIT) < TEMP_AMOUNT_PRECISION;
        }
        return false;
    }
}
