package vstu.isd.notebin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@Table(name = "view_note")
public class ViewNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "note_id")
    private Long noteId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
