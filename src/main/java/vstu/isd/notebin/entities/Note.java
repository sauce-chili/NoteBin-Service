package vstu.isd.notebin.entities;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "note")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "note_id_gen")
    @SequenceGenerator(name = "note_id_gen", sequenceName = "note_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 128)
    @Column(name = "title", length = 128)
    private String title;

    @NotNull
    @Column(name = "note_text", nullable = false, length = Integer.MAX_VALUE)
    private String noteText;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_at")
    private OffsetDateTime createAt;

    @NotNull
    @Column(name = "url", nullable = false, length = 16)
    private String url;

    @Column(name = "expiration_type")
    @Enumerated(EnumType.STRING)
    private ExpirationType expirationType;

    @ColumnDefault("true")
    @Column(name = "is_available")
    private Boolean isAvailable;

    public Note(String title, String noteText, ExpirationType expirationType) {
        this.title = title;
        this.noteText = noteText;
        this.expirationType = expirationType;
    }
}