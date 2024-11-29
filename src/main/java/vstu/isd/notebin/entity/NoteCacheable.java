package vstu.isd.notebin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.time.Duration;
import java.time.LocalDateTime;

@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("note")
public class NoteCacheable extends BaseNote {
    @Id
    private String url;

    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private boolean isAvailable;
    @Enumerated(EnumType.STRING)
    private ExpirationType expirationType;
    private Duration expirationPeriod;
    private LocalDateTime expirationFrom;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ExpirationType getExpirationType() {
        return expirationType;
    }

    @Override
    public void setExpirationType(ExpirationType expirationType) {
        this.expirationType = expirationType;
    }

    @Override
    public Duration getExpirationPeriod() {
        return expirationPeriod;
    }

    @Override
    public void setExpirationPeriod(Duration expirationPeriod) {
        this.expirationPeriod = expirationPeriod;
    }

    @Override
    public LocalDateTime getExpirationFrom() {
        return expirationFrom;
    }

    @Override
    public void setExpirationFrom(LocalDateTime expirationFrom) {
        this.expirationFrom = expirationFrom;
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    @JsonIgnore
    public boolean isExpired() {
        return super.isExpired();
    }

    @Override
    @JsonIgnore
    public LocalDateTime expireAt() {
        return super.expireAt();
    }
}