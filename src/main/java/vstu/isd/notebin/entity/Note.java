package vstu.isd.notebin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import vstu.isd.notebin.util.converter.DurationToIntervalConverter;

import java.time.Duration;
import java.time.LocalDateTime;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "note")
public class Note extends BaseNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @SequenceGenerator(name = "note_id_gen", sequenceName = "note_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", length = 128, nullable = false)
    private String title;

    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "url", nullable = false, length = 16)
    private String url;

    @Column(name = "expiration_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExpirationType expirationType;

    @Column(name = "expiration_period")
    @Convert(converter = DurationToIntervalConverter.class)
    private Duration expirationPeriod;

    /**
     * <p>Used to calculate the expiration date for the {@link ExpirationType} by period.</p>
     * Should be updated when the expiration type changes
     */
    @Column(name = "expiration_from")
    private LocalDateTime expirationFrom;

    @ColumnDefault("true")
    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

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
}