package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    // We will use this later to show the user their uploaded file
    Optional<DocumentEntity> findByUserIdAndDocType(Long userId, String docType);
}