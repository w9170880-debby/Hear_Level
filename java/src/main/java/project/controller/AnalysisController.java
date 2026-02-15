package project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.model_entity.UserFile;
import project.repository.FileRepository;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {

    private final FileRepository fileRepository;

    public AnalysisController(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }
    // יצירה ושמירה של ניתוח חדש במסד הנתונים בעת העלאת קובץ.
    @PostMapping("/analyze")
    public ResponseEntity<?> handleAnalysis(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("numQuestions") int numQuestions) {

        try {
            if (file == null || file.isEmpty()) {
                System.out.println("ניסיון ניתוח נכשל: קובץ ריק או לא קיים");
                return ResponseEntity.badRequest().body("הקובץ לא התקבל");
            }

            //יצירת האובייקט
            UserFile userFile = new UserFile();
            userFile.setUserId(userId);
            userFile.setFileName(file.getOriginalFilename());
            userFile.setDifficultyLevel("B2"); // קבוע בינתיים
            userFile.setFilePath("uploads/" + file.getOriginalFilename()); // נתיב זמני
            userFile.setQuestions(new ArrayList<>());
            // שמירה במונגו
            UserFile savedFile = fileRepository.save(userFile);

            System.out.println("------------------------------------");
            System.out.println("קובץ חדש נותח ונשמר ב-MongoDB!");
            System.out.println("ID של הניתוח: " + savedFile.getId());
            System.out.println("שייך למשתמש: " + savedFile.getUserId());
            System.out.println("שם הקובץ: " + savedFile.getFileName());
            System.out.println("------------------------------------");

            return ResponseEntity.ok(savedFile);
        }
        catch (Exception e) {
            System.out.println("שגיאה חמורה בניתוח: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // מאפשרת ל-React למשוך את נתוני הניתוח מהמסד לפי ה-ID, למשל אחרי רענון דף או במעבר לדף התוצאות
    @GetMapping("/analyze/{id}")
    public ResponseEntity<?> getAnalysis(@PathVariable String id) {
        System.out.println("שליפת ניתוח עבור ID: " + id);

        return fileRepository.findById(id)
                .map(analysis -> {
                    System.out.println("הניתוח נמצא ונשלח לריאקט.");
                    return ResponseEntity.ok(analysis);
                })
                .orElseGet(() -> {
                    System.out.println("שגיאה: ניתוח עם ID " + id + " לא נמצא במסד הנתונים.");
                    return ResponseEntity.notFound().build();
                });
    }
    // שליפת כל הקבצים השייכים למשתמש ספציפי
    @GetMapping("/user/{userId}/files")
    public ResponseEntity<List<UserFile>> getUserFiles(@PathVariable String userId) {
        System.out.println("שולף רשימת קבצים עבור משתמש: " + userId);

        List<UserFile> files = fileRepository.findByUserId(userId);

        System.out.println("נמצאו " + files.size() + " קבצים.");
        return ResponseEntity.ok(files);
    }
}
