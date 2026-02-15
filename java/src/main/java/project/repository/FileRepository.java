package project.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import project.model_entity.UserFile;
import java.util.List;

public interface FileRepository extends MongoRepository<UserFile, String> {
    // פונקציה שתשלוף את כל הקבצים של משתמש ספציפי
    List<UserFile> findByUserId(String userId);
}
