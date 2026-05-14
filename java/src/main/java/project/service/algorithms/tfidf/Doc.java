package project.service.algorithms.tfidf;

import java.io.Serializable;

public class Doc implements Serializable {
    private int id;
    private int wordCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
}
