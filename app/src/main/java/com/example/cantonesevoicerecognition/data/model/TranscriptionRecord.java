package com.example.cantonesevoicerecognition.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "transcriptions")
public class TranscriptionRecord {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "original_text")
    private String originalText;
    
    @ColumnInfo(name = "edited_text")
    private String editedText;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "audio_file_path")
    private String audioFilePath;
    
    @ColumnInfo(name = "duration")
    private int duration; // 毫秒
    
    @ColumnInfo(name = "confidence")
    private float confidence;
    
    @ColumnInfo(name = "is_real_time")
    private boolean isRealTime;
    
    // 构造函数
    public TranscriptionRecord() {
        this.timestamp = System.currentTimeMillis();
        this.confidence = 0.0f;
        this.isRealTime = false;
        this.duration = 0;
    }
    
    public TranscriptionRecord(String originalText, String editedText, String audioFilePath, 
                             int duration, float confidence, boolean isRealTime) {
        this();
        this.originalText = originalText;
        this.editedText = editedText;
        this.audioFilePath = audioFilePath;
        this.duration = duration;
        this.confidence = confidence;
        this.isRealTime = isRealTime;
    }
    
    // Getter方法
    public long getId() {
        return id;
    }
    
    public String getOriginalText() {
        return originalText;
    }
    
    public String getEditedText() {
        return editedText;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getAudioFilePath() {
        return audioFilePath;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public boolean isRealTime() {
        return isRealTime;
    }
    
    // Setter方法
    public void setId(long id) {
        this.id = id;
    }
    
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
    
    public void setEditedText(String editedText) {
        this.editedText = editedText;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public void setRealTime(boolean realTime) {
        this.isRealTime = realTime;
    }
    
    // 工具方法
    public boolean hasAudioFile() {
        return audioFilePath != null && !audioFilePath.trim().isEmpty();
    }
    
    public boolean isHighConfidence() {
        return confidence >= 0.8f;
    }
    
    public String getFormattedDuration() {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}