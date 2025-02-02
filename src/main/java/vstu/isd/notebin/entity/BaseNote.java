package vstu.isd.notebin.entity;

import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import static vstu.isd.notebin.util.UtilFunc.durationAreEquals;
import static vstu.isd.notebin.util.UtilFunc.localDateTimeAreEquals;

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

    abstract public Long getUserId();

    abstract public void setUserId(Long userId);

    public boolean isNoteOwner(Long userId) {
        if (getId() == null || userId == null) return false;
        return Objects.equals(getUserId(), userId);
    }

    public boolean isNotNoteOwner(Long userId) {
        return !isNoteOwner(userId);
    }

    abstract public boolean isAvailable();

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
    public boolean equals(Object other) {
        if (other == null) return false;
        if (this == other) return true;
        if (!(other instanceof BaseNote o)) return false;

        if (!Objects.equals(getId(), o.getId())) return false;
        if (!Objects.equals(getTitle(), o.getTitle())) return false;
        if (!Objects.equals(getContent(),o.getContent())) return false;
        if (!localDateTimeAreEquals(getCreatedAt(), o.getCreatedAt())) return false;
        if (!Objects.equals(getUrl(), o.getUrl())) return false;
        if (getExpirationType() != o.getExpirationType()) return false;
        if (!durationAreEquals(getExpirationPeriod(), o.getExpirationPeriod())) return false;
        if (!localDateTimeAreEquals(getExpirationFrom(), o.getExpirationFrom())) return false;
        if (!Objects.equals(getUserId(), o.getUserId())) return false;
        return Objects.equals(isAvailable(), o.isAvailable());
    }
}
