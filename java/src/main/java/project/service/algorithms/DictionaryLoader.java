package project.service.algorithms;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.springframework.stereotype.Component;


@Component
public class DictionaryLoader {

    private final Map<String, String> cefrMap; //לטעינת מילון CEFR
    private final Set<String> academicWords;  // לטעינת מילון מילים אקדמאיות
    private final Map<String, Double> aoaMap; //לטעינת מילון גיל רכישת המילה
    private final Map<String, String> connectivesMap; // לטעינת מילות קישור


    public DictionaryLoader() {
        this.cefrMap = loadCefrFromCsv();
        this.academicWords = loadAcademicWords();
        this.aoaMap = loadAoaFromExcelExport();
        this.connectivesMap = loadConnectives();

        System.out.println(">>> All dictionaries loaded successfully!");
    }

    public Map<String, String> getCefrMap() { return cefrMap; }
    public Set<String> getAcademicWords() { return academicWords; }
    public Map<String, Double> getAoaMap() { return aoaMap; }
    public Map<String, String> getConnectivesMap() { return connectivesMap; }



    //  פונקציה לטעינת המילון CEFR
    private Map<String, String> loadCefrFromCsv() {
        Map<String, String> map = new HashMap<>();
        try {
            InputStream is = getClass().getResourceAsStream("/data/cefr_dictionary.csv");
            if (is == null) {
                System.err.println("!!! שגיאה: הקובץ לא נמצא ב-resources !!!");
                return map;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                int totalLines = 0;
                while ((line = br.readLine()) != null) {
                    totalLines++;
                    if (line.trim().isEmpty()) continue;

                    // הסרת BOM ותווים נסתרים
                    line = line.replace("\uFEFF", "").trim();

                    // תמיכה גם בפסיק וגם בנקודה-פסיק
                    String delimiter = line.contains(",") ? "," : ";";
                    String[] parts = line.split(delimiter);

                    if (parts.length >= 3) {
                        String word = parts[0].toLowerCase().replaceAll("[^a-z]", "").trim();
                        String posTag = parts[1].toUpperCase().replaceAll("[^A-Z0-9]", "").trim();
                        String level = parts[2].toUpperCase().replaceAll("[^A-Z0-9]", "").trim();
                        map.put(word, level);
                    } else {
                        if (totalLines < 3) {
                            System.out.println("[DEBUG] Line could not be split: " + line);
                        }
                    }
                }
                System.out.println(">>> המילון נטען! שורות שנקראו: " + totalLines + ", מילים במפה: " + map.size());
            }
        } catch (Exception e) {
            System.err.println("!!! תקלה: " + e.getMessage());
        }
        return map;
    }

    //  טעינת מילים אקדמיות (NAWL) - לוקח את העמודה הראשונה מכל שורה
    private Set<String> loadAcademicWords() {
        Set<String> set = new HashSet<>();
        try (InputStream is = getClass().getResourceAsStream("/data/NAWL_10_lemmatized_for_teaching.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    set.add(parts[0].trim().toLowerCase());
                }
            }
            System.out.println("Academic Words (NAWL) loaded: " + set.size());
        } catch (Exception e) { System.err.println("Error loading NAWL: " + e.getMessage()); }
        return set;
    }

    //  טעינת מילות קישור
    private Map<String, String> loadConnectives() {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/data/connectives.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            br.readLine(); // דילוג על כותרת אם קיימת
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    map.put(parts[0].trim().toLowerCase(), parts[1].trim().toUpperCase());
                }
            }
            System.out.println("Connectives loaded: " + map.size());
        } catch (Exception e) { System.err.println("Error loading Connectives: " + e.getMessage()); }
        return map;
    }

    //  טעינת AoA
    private Map<String, Double> loadAoaFromExcelExport() {
        Map<String, Double> map = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/data/AoA_ratings_Kuperman_et_al_BRM_with_PoS.csv")) {
            if (is == null) {
                System.err.println("!!! AoA file not found in resources !!!");
                return map;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                boolean isFirstLine = true;

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    // ניקוי BOM ותווים נסתרים (כמו בטעינת ה-CEFR)
                    line = line.replace("\uFEFF", "").trim();

                    // תמיכה בפסיק או נקודה-פסיק (Excel לפעמים משתמש ב-;)
                    String delimiter = line.contains(",") ? "," : ";";
                    String[] parts = line.split(delimiter);

                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // דילוג על הכותרת (Word, Rating.Mean)
                    }

                    if (parts.length >= 2) {
                        try {
                            String word = parts[0].trim().toLowerCase();
                            // המרת המספר - ניקוי רווחים וטיפול אפשרי בנקודה/פסיק עשרוני
                            String scoreStr = parts[1].trim().replace(",", ".");
                            Double score = Double.parseDouble(scoreStr);
                            map.put(word, score);
                        } catch (NumberFormatException e) {
                            // שורה לא תקינה - נדלג עליה
                        }
                    }
                }
                System.out.println("AoA Ratings loaded: " + map.size());
            }
        } catch (Exception e) {
            System.err.println("Error loading AoA: " + e.getMessage());
        }
        return map;
    }


    // שליפת רמת ה-CEFR של מילה
    public String getWordLevel(String word) {
        return cefrMap.getOrDefault(word.toLowerCase(), "UNKNOWN");
    }

    // בדיקה אם מילה היא אקדמית
    public boolean isAcademic(String word) {
        return academicWords.contains(word.toLowerCase());
    }

    // שליפת גיל הרכישה (AoA)
    public double getAoA(String word) {
        return aoaMap.getOrDefault(word.toLowerCase(), 0.0);
    }

    // שליפת סוג מילת הקישור
    public String getConnectiveType(String word) {
        return connectivesMap.get(word.toLowerCase());
    }

}
