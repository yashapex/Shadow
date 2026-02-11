package hiring_project.shadow_assistant.repository;

import hiring_project.shadow_assistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Used by the Security Config to find users during login
    Optional<User> findByUsername(String username);
}