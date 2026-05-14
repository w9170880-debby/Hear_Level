package project.service.algorithms;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.trees.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import project.dto.TextStats;
import project.service.algorithms.tfidf.Indexer;

import java.util.*;

/**
 * מחלץ 32 מאפיינים מטקסט לצורך סיווג רמת CEFR (A1–C2).
 * המאפיינים מחולקים ל-4 קבוצות:
 *   1. קריאות וסטטיסטיקה (5)
 *   2. לקסיקלי ומילונים (16)
 *   3. חלקי דיבר ותחביר (5)
 *   4. TF-IDF — נדירות מילים (5 + 1 מקבוצה 1)
 *
 * הכלל: הטקסט נסרק פעם אחת בלבד (processDocument), כל שאר החישובים
 * עובדים על הנתונים שנאספו — ללא מעבר נוסף על הטקסט.
 */
@Component
public class FeatureCalculationService {

    @Autowired
    private Indexer indexer;
    @Autowired
    private DictionaryLoader dictionaryLoader;

    private static final String[] CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"};

    public Map<String, Double> calculateAllFeatures(CoreDocument doc) {
        TextStats stats = processDocument(doc);

        Map<String, Double> features = new HashMap<>();
        features.putAll(calculateReadability(stats));
        features.putAll(extractLinguisticFeatures(stats));
        features.putAll(calculateTfidfFeatures(stats));
        return features;
    }

    // =========================================================
    // שלב 1 — מעבר יחיד על הטקסט: איסוף כל הנתונים הגולמיים
    // =========================================================

    /**
     * עוברת פעם אחת על כל המשפטים והמילים ואוספת נתונים גולמיים ל-TextStats.
     * כל החישובים של המאפיינים הסופיים ייעשו לאחר מכן על הנתונים שנאספו כאן.
     */
    private TextStats processDocument(CoreDocument doc) {
        TextStats stats = new TextStats();
        stats.setTotalSentences(doc.sentences().size());

        for (CoreSentence sentence : doc.sentences()) {

            // ---- עומק עץ תחבירי ----
            // סטנפורד בונה עץ היררכי המייצג את מבנה המשפט.
            // ככל שהמשפט מכיל יותר שיעבודים ופסוקיות — העץ עמוק יותר.
            // הערך נצבר על פני כל המשפטים; הממוצע יחושב לאחר המעבר.
            Tree tree = sentence.constituencyParse();
            if (tree != null) stats.addTreeDepth(tree.depth());

            for (CoreLabel token : sentence.tokens()) {
                String word  = token.word().toLowerCase();  // המילה כפי שהיא בטקסט
                String lemma = token.lemma().toLowerCase(); // צורת המקור (running → run)
                String pos   = token.tag();                 // תגית חלק דיבר (NN, VB, JJ...)

                // מסנן מספרים, סימני פיסוק וכל תו שאינו אות אנגלית
                if (!word.matches("[a-z]+")) continue;

                // ---- נתונים בסיסיים לחישובי קריאות ----
                stats.setTotalWords(stats.getTotalWords() + 1);
                stats.setTotalCharacters(stats.getTotalCharacters() + word.length());

                // ספירת הברות — משמשת גם ל-Flesch-Kincaid וגם ל-complex_word_ratio
                int syllables = countSyllables(word);
                stats.setTotalSyllables(stats.getTotalSyllables() + syllables);
                if (syllables >= 3) stats.setComplexWords(stats.getComplexWords() + 1);

                // ---- TF-IDF וגיוון אוצר מילים ----
                // שמירת הלמה (צורת מקור) לחישוב TF-IDF וביגרמות.
                // שימוש בלמה ולא במילה גולמית — עקבי עם אופן האימון של האינדקס.
                stats.addTfidfWord(lemma);
                // סט המילים הייחודיות — לחישוב עושר לקסיקלי (root_ttr)
                stats.addUniqueWord(lemma);

                // ---- חלקי דיבר (POS Tagging) ----
                // סטנפורד מתייג כל מילה לפי תפקידה במשפט.
                // NN/NNS = שם עצם, VB* = פועל, JJ* = תואר שם, RB* = תואר פועל
                if      (pos.startsWith("NN")) stats.incrementNoun();
                else if (pos.startsWith("VB")) stats.incrementVerb();
                else if (pos.startsWith("JJ")) stats.incrementAdjective();
                else if (pos.startsWith("RB")) stats.incrementAdverb();

                // ---- מילון CEFR — 3 שלבי חיפוש ----
                // שלב 1: חיפוש לפי למה (הצורה הבסיסית — כיסוי מרבי)
                // שלב 2: חיפוש לפי המילה המקורית (למקרה שהלמה שגויה)
                // שלב 3: ניסיון נרמול — הסרת תחיליות (un-) וסיומות (-ly)
                //         לדוגמה: "unhappy" → "happy", "quickly" → "quick"
                String level = dictionaryLoader.getWordLevel(lemma);
                if ("UNKNOWN".equals(level)) level = dictionaryLoader.getWordLevel(word);
                if ("UNKNOWN".equals(level)) level = dictionaryLoader.getWordLevel(normalizeWord(word));

                if (!"UNKNOWN".equals(level)) stats.addCefrLevel(level);
                else stats.incrementUnknown(); // מילה שלא נמצאה בשום שלב

                // ---- מילים אקדמיות (AWL) ----
                // בדיקה מול רשימת NAWL —  משפחות מילים אקדמיות.
                // נוכחות מילים אקדמיות מאפיינת רמות B2–C2.
                if (dictionaryLoader.isAcademic(lemma)) stats.incrementAcademic();

                // ---- מילות קישור לפי רמה ----
                // המילון מכיל מילות קישור (however, therefore, although...)
                // עם רמת הקושי שלהן. ברמות גבוהות יש מגוון רחב יותר של קישורים.
                String connectiveLevel = dictionaryLoader.getConnectiveType(lemma);
                if (connectiveLevel != null) {
                    stats.incrementConnectives();
                    stats.incrementConnectiveLevel(connectiveLevel); // ספירה גם לפי רמה
                }

                // ---- גיל רכישה (AoA) ----
                // הגיל הממוצע שבו ילדים דוברי אנגלית רוכשים מילה זו.
                // "house" ≈ 3, "hypothesis" ≈ 14. ממוצע גבוה = מילים מאוחרות = רמה גבוהה.
                double aoa = dictionaryLoader.getAoA(lemma);
                if (aoa > 0) stats.addAoA(aoa);
            }
        }
        return stats;
    }

    // =========================================================
    // שלב 2 — חישוב מאפייני קריאות וסטטיסטיקה
    // =========================================================

    private Map<String, Double> calculateReadability(TextStats stats) {
        Map<String, Double> features = new HashMap<>();
        double wordsDiv = Math.max(stats.getTotalWords(), 1);
        double sentDiv  = Math.max(stats.getTotalSentences(), 1);

        // Flesch-Kincaid Grade Level — נוסחה סטנדרטית לרמת קריאות.
        // התוצאה היא ציון המקביל לשכבת לימוד (6 ≈ כיתה ו').
        // שני גורמים: אורך משפט ממוצע + ממוצע הברות למילה.
        double fkGrade = 0.39 * (wordsDiv / sentDiv)
                       + 11.8 * ((double) stats.getTotalSyllables() / wordsDiv)
                       - 15.59;
        features.put("flesch_kincaid_grade", fkGrade);

        // אורך משפט ממוצע — משפטים ארוכים מעידים על מורכבות תחבירית גבוהה יותר
        features.put("avg_sentence_length", wordsDiv / sentDiv);

        // אורך מילה ממוצע — מילים ארוכות בדרך כלל מורכבות ונדירות יותר
        features.put("avg_word_length", (double) stats.getTotalCharacters() / wordsDiv);

        // ממוצע עומק עץ תחבירי — ככל שעמוק יותר, המשפטים מורכבים יותר תחבירית
        features.put("avg_tree_depth", (double) stats.getTotalTreeDepth() / sentDiv);

        // שיעור מילים עם 3+ הברות — מילים מרובות הברות קשות יותר לעיבוד
        features.put("complex_word_ratio", (double) stats.getComplexWords() / wordsDiv);

        return features;
    }

    // =========================================================
    // שלב 3 — חישוב מאפיינים לקסיקליים ובלשניים
    // =========================================================

    private Map<String, Double> extractLinguisticFeatures(TextStats stats) {
        Map<String, Double> features = new HashMap<>();
        double wordsDiv = Math.max(stats.getTotalWords(), 1);

        // ---- חלקי דיבר ----
        // ניתוח התפלגות חלקי הדיבר מאפשר זיהוי סגנון הטקסט.
        // טקסטים אקדמיים: הרבה שמות עצם ותארים.
        // טקסטים שיחתיים: יותר פעלים ותוארי פועל.
        features.put("noun_ratio",      stats.getNounCount()      / wordsDiv);
        features.put("verb_ratio",      stats.getVerbCount()      / wordsDiv);
        features.put("adjective_ratio", stats.getAdjectiveCount() / wordsDiv);
        features.put("adverb_ratio",    stats.getAdverbCount()    / wordsDiv);

        // צפיפות לקסיקלית — שיעור "מילות תוכן" (שמות עצם + תארים) מסך המילים.
        // צפיפות גבוהה = טקסט עשיר במידע, אופייני לרמות גבוהות.
        features.put("lexical_density", (stats.getNounCount() + stats.getAdjectiveCount()) / wordsDiv);

        // עושר לקסיקלי (Root TTR) — מילים ייחודיות חלקי שורש מספר המילים.
        // החלוקה בשורש מנרמלת את המדד לפי אורך הטקסט,
        // כי טקסטים ארוכים טבעית חוזרים יותר על מילים.
        features.put("root_ttr", stats.getUniqueWordCount() / Math.sqrt(wordsDiv));

        // שיעור מילים אקדמיות (AWL) — מאפיין רמות B2–C2
        features.put("academic_word_ratio", (double) stats.getAcademicCount() / wordsDiv);

        // שיעור מילות קישור כלל — ברמות גבוהות המשפטים "מחוברים" בצורה מגוונת יותר
        features.put("connective_ratio", (double) stats.getConnectiveCount() / wordsDiv);

        // שיעור מילים לא מוכרות — מילים שלא נמצאו במילון CEFR בשלושת שלבי החיפוש
        features.put("unknown_ratio", (double) stats.getUnknownWordCount() / wordsDiv);

        // ממוצע גיל רכישה — ממוצע גבוה = מילים שנלמדות מאוחר = רמה גבוהה יותר
        features.put("avg_aoa", stats.getWordsWithAoA() > 0
                ? stats.getTotalAoA() / stats.getWordsWithAoA() : 0.0);

        // שיעור מילים לפי רמת CEFR (6 מאפיינים: a1_ratio עד c2_ratio)
        // טקסט ברמה גבוהה: a1_ratio נמוך, b2/c1/c2_ratio גבוהים יותר
        for (String l : CEFR_LEVELS) {
            features.put(l.toLowerCase() + "_ratio", stats.getCefrCount(l) / wordsDiv);
        }

        // שיעור מילות קישור לפי רמה (6 מאפיינים: connective_a1_ratio עד connective_c2_ratio)
        // קישורי B2/C1 כגון "consequently", "notwithstanding" מעידים על שיח מתקדם
        for (String l : CEFR_LEVELS) {
            features.put("connective_" + l.toLowerCase() + "_ratio",
                    stats.getConnectiveLevelCount(l) / wordsDiv);
        }

        return features;
    }

    // =========================================================
    // שלב 4 — חישוב מאפייני TF-IDF (נדירות מילים)
    // =========================================================

    /**
     * TF-IDF מודד כמה "נדירה" כל מילה ביחס למאגר האימון (~17,000 טקסטים).
     * ציון גבוה = מילה נדירה = עשויה להצביע על רמה גבוהה.
     * החישוב נעשה גם על מילים בודדות (unigrams) וגם על זוגות סמוכים (bigrams).
     * היתרון על מילונים: TF-IDF דינמי — מגיב לנושא הספציפי של הטקסט,
     * גם כאשר מילונים סטטיים לא מכסים מונחים טכניים או מקצועיים.
     */
    private Map<String, Double> calculateTfidfFeatures(TextStats stats) {
        Map<String, Double> features = new HashMap<>();
        List<String> words = stats.getTfidfWords();
        List<Double> scores = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            scores.add(indexer.getScoreForWord(words.get(i)));
            if (i < words.size() - 1)
                scores.add(indexer.getScoreForWord(words.get(i) + " " + words.get(i + 1)));
        }

        if (scores.isEmpty()) return features;

        double avg      = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double max      = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double variance = scores.stream().mapToDouble(s -> Math.pow(s - avg, 2)).average().orElse(0.0);
        long rareCount  = scores.stream().filter(s -> s > 0.5).count();

        List<Double> sorted = new ArrayList<>(scores);
        Collections.sort(sorted);
        double median = sorted.get(sorted.size() / 2);

        // ממוצע ציוני הנדירות של כלל המילים בטקסט
        features.put("tfidf_avg", avg);
        // המילה הנדירה ביותר בטקסט
        features.put("tfidf_max", max);
        // סטיית תקן — האם הנדירות אחידה או שיש קפיצות חדות
        features.put("tfidf_std", Math.sqrt(variance));
        // שיעור המילים עם ציון מעל 0.5 — "באמת נדירות", אינדיקטור לרמות C
        features.put("tfidf_rare_ratio", (double) rareCount / scores.size());
        // חציון — פחות רגיש לערכי קיצון מאשר הממוצע
        features.put("tfidf_median", median);

        return features;
    }

    // =========================================================
    // פונקציות עזר פנימיות
    // =========================================================

    /**
     * סופרת הברות במילה על פי כלל האנגלית:
     * - כל קבוצת תנועות רצופות = הברה אחת
     * - e שקטה בסוף מילה — מחסירים הברה
     * - מינימום הברה אחת לכל מילה
     */
    private int countSyllables(String word) {
        String vowels = "aeiouy";
        word = word.toLowerCase();
        int count = 0;
        boolean lastWasVowel = false;
        for (char c : word.toCharArray()) {
            boolean isVowel = vowels.indexOf(c) >= 0;
            if (isVowel && !lastWasVowel) count++;
            lastWasVowel = isVowel;
        }
        if (word.length() > 2 && word.endsWith("e")) count--;
        return Math.max(count, 1);
    }

    /**
     * מנרמל מילה לצורך חיפוש במילון CEFR כאשר הלמה לא נמצאה:
     * - מסיר תחילית "un" (unhappy → happy)
     * - מסיר סיומת "ly" (quickly → quick)
     * מטרה: הגדלת כיסוי המילון ללא תלות בצורות מוטות.
     */
    private String normalizeWord(String word) {
        String w = word.toLowerCase();
        if (w.startsWith("un") && w.length() > 4) w = w.substring(2);
        if (w.endsWith("ly") && w.length() > 4)   w = w.substring(0, w.length() - 2);
        return w;
    }
}
