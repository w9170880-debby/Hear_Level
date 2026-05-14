package project.dto;

import java.util.*;

public class TextStats {

    // בסיסי
    private int totalWords = 0;
    private int totalSentences = 0;
    private int totalSyllables = 0;
    private int complexWords = 0;
    private int totalCharacters = 0;
    private int totalTreeDepth = 0;

    // חלקי דיבר
    private int nounCount = 0;
    private int verbCount = 0;
    private int adjectiveCount = 0;
    private int adverbCount = 0;

    // TF-IDF וגיוון
    private List<String> tfidfWords = new ArrayList<>();
    private Set<String> uniqueWords = new HashSet<>();

    // מילונים
    private int academicCount = 0;
    private int connectiveCount = 0;
    private double totalAoA = 0;
    private int wordsWithAoA = 0;
    private int unknownWordCount = 0;
    private Map<String, Integer> cefrCounts = new HashMap<>();
    private Map<String, Integer> connectiveLevelCounts = new HashMap<>();

    public TextStats() {}

    // --- עדכון נתונים ---
    public void addTfidfWord(String word)       { tfidfWords.add(word); }
    public void addUniqueWord(String word)       { uniqueWords.add(word); }
    public void addTreeDepth(int depth)          { totalTreeDepth += depth; }
    public void incrementNoun()                  { nounCount++; }
    public void incrementVerb()                  { verbCount++; }
    public void incrementAdjective()             { adjectiveCount++; }
    public void incrementAdverb()                { adverbCount++; }
    public void incrementAcademic()              { academicCount++; }
    public void incrementConnectives()           { connectiveCount++; }
    public void incrementUnknown()               { unknownWordCount++; }
    public void addAoA(double score)             { totalAoA += score; wordsWithAoA++; }

    public void incrementConnectiveLevel(String level) {
        connectiveLevelCounts.merge(level.toUpperCase(), 1, Integer::sum);
    }
    public void addCefrLevel(String level) {
        if (level != null && !level.equals("UNKNOWN")) {
            cefrCounts.merge(level.toUpperCase(), 1, Integer::sum);
        }
    }

    // --- חישובים ---
    public double getAverageWordsPerSentence()  { return totalSentences == 0 ? 0 : (double) totalWords / totalSentences; }
    public double getAverageSyllablesPerWord()  { return totalWords == 0 ? 0 : (double) totalSyllables / totalWords; }

    // --- Getters & Setters ---
    public int getTotalWords()                  { return totalWords; }
    public void setTotalWords(int v)            { totalWords = v; }
    public int getTotalSentences()              { return totalSentences; }
    public void setTotalSentences(int v)        { totalSentences = v; }
    public int getTotalSyllables()              { return totalSyllables; }
    public void setTotalSyllables(int v)        { totalSyllables = v; }
    public int getComplexWords()                { return complexWords; }
    public void setComplexWords(int v)          { complexWords = v; }
    public int getTotalCharacters()             { return totalCharacters; }
    public void setTotalCharacters(int v)       { totalCharacters = v; }
    public int getTotalTreeDepth()              { return totalTreeDepth; }
    public int getNounCount()                   { return nounCount; }
    public int getVerbCount()                   { return verbCount; }
    public int getAdjectiveCount()              { return adjectiveCount; }
    public int getAdverbCount()                 { return adverbCount; }
    public List<String> getTfidfWords()         { return tfidfWords; }
    public int getUniqueWordCount()             { return uniqueWords.size(); }
    public int getAcademicCount()               { return academicCount; }
    public int getConnectiveCount()             { return connectiveCount; }
    public double getTotalAoA()                 { return totalAoA; }
    public int getWordsWithAoA()               { return wordsWithAoA; }
    public int getUnknownWordCount()            { return unknownWordCount; }
    public int getCefrCount(String level)       { return cefrCounts.getOrDefault(level.toUpperCase(), 0); }
    public int getConnectiveLevelCount(String l){ return connectiveLevelCounts.getOrDefault(l.toUpperCase(), 0); }
}
