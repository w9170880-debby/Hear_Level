package project.service;

import project.service.algorithms.TextFeatureExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import project.service.algorithms.tfidf.Indexer;

import java.io.*;
import java.util.*;

@Service
public class AnalysisService {

    @Autowired
    private TextFeatureExtractor extractor;
    @Autowired
    private Indexer indexer;

    private static final String INDEX_PATH =
            System.getProperty("user.dir") + "/tfidf_index.ser";

    @PostConstruct
    public void trainIndexer() {
        if (indexer.tryLoad(INDEX_PATH)) return; // אינדקס קיים — אין צורך לאמן

        System.out.println("אינדקס TF-IDF לא נמצא, מתחיל אימון...");
        List<String> texts = loadTrainingData();
        int count = 0;
        for (String text : texts) {
            extractor.trainIndexerWithText(text);
            if (++count % 50 == 0)
                System.out.println("עובדו: " + count + " / " + texts.size() + " טקסטים");
        }
        indexer.computeTFIDF();
        indexer.save(INDEX_PATH);
        System.out.println("✅ אימון הושלם — " + texts.size() + " טקסטים");
    }

    private List<String> loadTrainingData() {
        List<String> texts = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(
                "/training_data/cefr_leveled_texts.csv");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is))) {

            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // דלג כותרת

                // העמודה הראשונה היא הטקסט
                int lastComma = line.lastIndexOf(",");
                if (lastComma > 0) {
                    String text = line.substring(0, lastComma)
                            .replace("\"", "").trim();
                    if (!text.isEmpty()) texts.add(text);
                }
            }
        } catch (Exception e) {
            System.out.println("שגיאה בטעינת DATA: " + e.getMessage());
        }

        return texts;
    }

    // 1. פונקציה להרצת סקריפט הפייתון (Speech to Text)
    public String convertAudioToText(String audioFilePath) {
        StringBuilder output = new StringBuilder();
        try {
            // ודאי שהנתיב לסקריפט הפייתון מדויק
            ProcessBuilder pb = new ProcessBuilder("python", "python/speech_to_text.py", audioFilePath);
            pb.redirectErrorStream(true); // מאחד שגיאות לפלט הסטנדרטי
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // הוספתי את שורת ההדפסה הזו כדי שתראי את הפלט של פייתון בטרמינל
                    System.out.println("Python Output: " + line);
                    output.append(line).append(" ");
                }
            }
            process.waitFor(); // מחכה לסיום הסקריפט
        } catch (Exception e) {
            e.printStackTrace();
            return "Error in transcription: " + e.getMessage();
        }
        return output.toString().trim();
    }

    // פונקציה לניקוי הטקסט מהאזהרות של פייתון
    public String cleanTranscribedText(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";

        String cleanText = rawText;

        // 1. חילוץ הטקסט שבין התגים שהגדרנו בסקריפט הפייתון
        if (cleanText.contains("--- RESULT START ---") && cleanText.contains("--- RESULT END ---")) {
            int start = cleanText.indexOf("--- RESULT START ---") + "--- RESULT START ---".length();
            int end = cleanText.indexOf("--- RESULT END ---");
            cleanText = cleanText.substring(start, end);
        }

        // 2. הסרת מילים בסוגריים כמו [Music] או (Laughter)
        cleanText = cleanText.replaceAll("\\[.*?\\]", "");
        cleanText = cleanText.replaceAll("\\(.*?\\)", "");

        // 3. ניקוי רווחים כפולים וירידות שורה
        cleanText = cleanText.trim().replaceAll("\\s+", " ");

        return cleanText;
    }

    // חילוץ מהירות דיבור (WPM) מהפלט הגולמי של Python
    public double extractWpmFromOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) return 0.0;
        if (rawOutput.contains("--- WPM START ---") && rawOutput.contains("--- WPM END ---")) {
            int start = rawOutput.indexOf("--- WPM START ---") + "--- WPM START ---".length();
            int end   = rawOutput.indexOf("--- WPM END ---");
            try {
                return Double.parseDouble(rawOutput.substring(start, end).trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // חילוץ מאפיינים מטקסט + הוספת מהירות דיבור
    public Map<String, Double> processText(String text, double wpm) {
        if (text == null || text.isBlank()) return new HashMap<>();
        String cleaned = cleanTranscribedText(text);
        Map<String, Double> features = extractor.extractFeature(cleaned);
        features.put("words_per_minute", wpm);
        return features;
    }



    //רק לבינתים עד שיעבוד המודל של סיווג רמה
    public String calculateLevel(Map<String, Double> features) {
        double score = features.getOrDefault("flesch_kincaid_grade", 0.0);

        if (score < 4) return "A1";
        if (score < 7) return "A2";
        if (score < 10) return "B1";
        if (score < 13) return "B2";
        if (score < 16) return "C1";
        return "C2";
    }
}