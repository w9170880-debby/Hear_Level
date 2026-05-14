package project.service.algorithms.tfidf;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Indexer implements Serializable {

    // מילון: מילה → רשימת מסמכים עם ציון
    private final HashMap<String, List<DocIdWithFrequency>>
            invertedIndexHashMap = new HashMap<>();

    // מילון: מספר מסמך → מסמך
    private final HashMap<Integer, Doc> documentsHashMap = new HashMap<>();

    private int uniqueId = 0;
    private boolean isTrained = false;

    // שלב אימון — מוסיף מסמך לאינדקס
    public void addDocument(List<String> processedWords) {
        Doc doc = new Doc();
        doc.setId(uniqueId++);
        doc.setWordCount(processedWords.size());

        // 1. טיפול במילים בודדות (Unigrams)
        for (String lemma : processedWords) {
            updateIndex(lemma, doc.getId());
        }
        // 2. טיפול בביגרמות (Bigrams)
        for (int i = 0; i < processedWords.size() - 1; i++) {
            String bigram = processedWords.get(i) + " " + processedWords.get(i + 1);
            updateIndex(bigram, doc.getId());
        }
        documentsHashMap.put(doc.getId(), doc);
    }

    // פונקציית עזר  כדי לא לחזור על הקוד של ה-found
    private void updateIndex(String term, int docId) {
        invertedIndexHashMap.computeIfAbsent(term, k -> new ArrayList<>());
        List<DocIdWithFrequency> list = invertedIndexHashMap.get(term);

        boolean found = false;
        for (DocIdWithFrequency d : list) {
            if (d.getDocId() == docId) {
                d.setFrequency(d.getFrequency() + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(new DocIdWithFrequency(docId, 1));
        }
    }



    // שלב אימון — מחשב TF-IDF על כל ה-DATA
    // קוראים לזה פעם אחת אחרי כל addDocument
    public void computeTFIDF() {
        int totalDocs = documentsHashMap.size();

        for (Map.Entry<String, List<DocIdWithFrequency>> entry
                : invertedIndexHashMap.entrySet()) {
            List<DocIdWithFrequency> list = entry.getValue();

            for (DocIdWithFrequency d : list) {
                Doc doc = documentsHashMap.get(d.getDocId());
                if (doc != null) {
                    // TF = כמה פעמים המילה מופיעה / סך מילים במסמך
                    double tf = d.getFrequency() / doc.getWordCount();

                    // IDF = log(סך מסמכים / מסמכים שמכילים את המילה)
                    double idf = Math.log10(
                            totalDocs / (double) list.size());

                    d.setFrequency(tf * idf);
                }
            }
        }

        isTrained = true; // האינדקס מוכן!
    }

    // שמירת האינדקס לדיסק
    public void save(String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
            System.out.println("✅ אינדקס TF-IDF נשמר: " + path);
        } catch (Exception e) {
            System.err.println("שגיאה בשמירת אינדקס: " + e.getMessage());
        }
    }

    // טעינת אינדקס קיים מדיסק — מחזיר true אם הצליח
    public boolean tryLoad(String path) {
        File file = new File(path);
        if (!file.exists()) return false;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Indexer loaded = (Indexer) ois.readObject();
            this.invertedIndexHashMap.clear();
            this.invertedIndexHashMap.putAll(loaded.invertedIndexHashMap);
            this.documentsHashMap.clear();
            this.documentsHashMap.putAll(loaded.documentsHashMap);
            this.uniqueId = loaded.uniqueId;
            this.isTrained = loaded.isTrained;
            System.out.println("✅ אינדקס TF-IDF נטען מקובץ (" + this.documentsHashMap.size() + " מסמכים)");
            return true;
        } catch (Exception e) {
            System.err.println("שגיאה בטעינת אינדקס, יאומן מחדש: " + e.getMessage());
            return false;
        }
    }

    public double getScoreForWord(String word) {
        if (!isTrained) {
            throw new IllegalStateException(
                    "חייבים לקרוא ל-computeTFIDF() לפני המרה!");
        }

        List<DocIdWithFrequency> list =
                invertedIndexHashMap.get(word.toLowerCase());

        // מילה שלא נראתה באימון — ציון 0
        if (list == null) return 0.0;

        // ממוצע הציונים של המילה בכל המסמכים
        return list.stream()
                .mapToDouble(DocIdWithFrequency::getFrequency)
                .average()
                .orElse(0.0);
    }
}