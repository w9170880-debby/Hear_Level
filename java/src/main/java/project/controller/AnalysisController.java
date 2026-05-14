package project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.model_entity.UserFile;
import project.repository.FileRepository;
import project.service.AnalysisService;
import java.io.File;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {

    private final FileRepository fileRepository;
    private final AnalysisService analysisService;

    public AnalysisController(FileRepository fileRepository, AnalysisService analysisService) {
        this.fileRepository = fileRepository;
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> handleAnalysis(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("numQuestions") int numQuestions) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("הקובץ לא התקבל");
            }
            System.out.println("\n========== תחילת תהליך ניתוח חדש ==========");
            System.out.println("[שלב 1] קובץ התקבל מ-React: " + file.getOriginalFilename());
            System.out.println("[שלב 1] מזהה משתמש: " + userId);

            // 1. הגדרת נתיב אבסולוטי לתיקיית uploads בתוך הפרויקט
            String rootDirectory = System.getProperty("user.dir");
            Path uploadPath = Paths.get(rootDirectory, "uploads");

            // 2. יצירת התיקייה אם היא לא קיימת
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created directory: " + uploadPath.toAbsolutePath());
            }

            // 3. יצירת שם קובץ ייחודי ומניעת תקלות עם תווים מיוחדים
            String cleanFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path filePath = uploadPath.resolve(cleanFileName);

            // 4. שמירת הקובץ לנתיב האבסולוטי
            file.transferTo(filePath.toFile());
            System.out.println("[שלב 2] הקובץ נשמר בהצלחה בנתיב: " + filePath.toAbsolutePath());

            // --- השינוי לטובת הסימולציה מתחיל כאן ---

            // יצירת האובייקט הראשוני ושמירה ב-DB מיד כדי של-React יהיה ID לעקוב אחריו!
            UserFile userFile = new UserFile();
            userFile.setUserId(userId);
            userFile.setFileName(file.getOriginalFilename());
            userFile.setFilePath(filePath.toAbsolutePath().toString());
            userFile.setQuestions(new ArrayList<>());
            // הוספתי סטטוס אם קיים אצלך מנגנון כזה, אם לא, זה לא יפריע:
            // userFile.setStatus("PROCESSING");
            UserFile savedFile = fileRepository.save(userFile);

            // עטיפת הניתוח בתהליך רקע (Thread) כדי לשחרר את ה-React לסימולציה
            new Thread(() -> {
                try {
                    System.out.println("[שלב 3] שולח לסקריפט Python לתמלול (Speech-to-Text)...");
                    String rawText   = analysisService.convertAudioToText(filePath.toAbsolutePath().toString());
                    String cleanText = analysisService.cleanTranscribedText(rawText);
                    double wpm       = analysisService.extractWpmFromOutput(rawText);
                    System.out.println("[שלב 3] התמלול הסתיים בהצלחה. מהירות דיבור: " + wpm + " מילים לדקה.");

                    System.out.println("[שלב 4] מתחיל חילוץ מאפיינים לשוניים (StanfordNLP)...");
                    Map<String, Double> features = analysisService.processText(cleanText, wpm);
                    System.out.println("[שלב 4] חילוץ המאפיינים הושלם: " + features.size() + " מאפיינים נמצאו.");

                    System.out.println("[שלב 5] מחשב רמה ומעדכן ב-MongoDB...");
                    String calculatedLevel = analysisService.calculateLevel(features);

                    // עדכון הרשומה הקיימת שכבר שמרנו קודם
                    savedFile.setTranscribedText(cleanText);
                    savedFile.setExtractedFeatures(features);
                    savedFile.setDifficultyLevel(calculatedLevel);
                    // savedFile.setStatus("COMPLETED");

                    fileRepository.save(savedFile);
                    System.out.println("[שלב 6] הנתונים עודכנו ב-DB עבור ID: " + savedFile.getId());
                    System.out.println("========== סיום תהליך בהצלחה! ==========\n");

                } catch (Exception e) {
                    System.err.println("!!! שגיאה בתהליך הניתוח ברקע: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

            // מחזיר את ה-ID מיד ל-React, מה שמאפשר לסימולציה להתחיל!
            return ResponseEntity.ok(savedFile);

        } catch (Exception e) {
            System.err.println("!!! שגיאה בתהליך הקבלה: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/analyze/{id}")
    public ResponseEntity<?> getAnalysis(@PathVariable String id) {
        return fileRepository.findById(id)
                .map(analysis -> ResponseEntity.ok(analysis))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/files")
    public ResponseEntity<List<UserFile>> getUserFiles(@PathVariable String userId) {
        System.out.println("שולף רשימת קבצים עבור משתמש: " + userId);
        List<UserFile> files = fileRepository.findByUserId(userId);
        System.out.println("נמצאו " + files.size() + " קבצים.");
        return ResponseEntity.ok(files);
    }
}