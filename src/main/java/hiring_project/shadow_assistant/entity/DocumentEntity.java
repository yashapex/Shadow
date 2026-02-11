package hiring_project.shadow_assistant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;    // The User who uploaded it
    private String docType; // "RESUME" or "JD"

    @Column(columnDefinition = "TEXT") // ⚠️ Critical: Allows storing huge strings (PDF content)
    private String fullContent;
}