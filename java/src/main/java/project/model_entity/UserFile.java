package project.model_entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Document(collection = "analyses")
public class UserFile {
    @Id
    private String id;
    private String userId;
    private String fileName;
    private String filePath;
    private String difficultyLevel;
    private Double score;
    private String transcribedText;
    private Map<String, Double> extractedFeatures;
    //רשימת שאלות ותשובות
    private List<Map<String, Object>> questions;
    private List<String> answers;
    //סטטוס ניתוח
    private int status = 1;// 1: Audio, 2: Mapping, 3: Context, 4: Questions
    private Date createdAt;


    public UserFile() {}

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Map<String, Object>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Map<String, Object>> questions) {
        this.questions = questions;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getTranscribedText() {
        return transcribedText;
    }

    public void setTranscribedText(String transcribedText) {
        this.transcribedText = transcribedText;
    }

    public Map<String, Double> getExtractedFeatures() {
        return extractedFeatures;
    }

    public void setExtractedFeatures(Map<String, Double> extractedFeatures) {
        this.extractedFeatures = extractedFeatures;
    }
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }


}
