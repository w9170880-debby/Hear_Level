package project.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import project.model_entity.User;

import java.util.Optional;
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    // בדיקה אם אימייל כבר קיים במערכת (בשביל הרשמה)
    Boolean existsByEmail(String email);
}
