package com.example.cantonesevoicerecognition.data.model;

public class WordSegment {
    private String word;
    private float startTime;
    private float endTime;
    private float confidence;
    
    // 构造函数
    public WordSegment() {
        this.word = "";
        this.startTime = 0.0f;
        this.endTime = 0.0f;
        this.confidence = 0.0f;
    }
    
    public WordSegment(String word, float startTime, float endTime) {
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
        this.confidence = 0.0f;
    }
    
    public WordSegment(String word, float startTime, float endTime, float confidence) {
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
        this.confidence = confidence;
    }
    
    // Getter方法
    public String getWord() {
        return word;
    }
    
    public float getStartTime() {
        return startTime;
    }
    
    public float getEndTime() {
        return endTime;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    // Setter方法
    public void setWord(String word) {
        this.word = word;
    }
    
    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }
    
    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    // 工具方法
    public float getDuration() {
        return endTime - startTime;
    }
    
    public boolean isValid() {
        return word != null && !word.trim().isEmpty() && 
               startTime >= 0 && endTime > startTime;
    }
    
    public boolean isHighConfidence() {
        return confidence >= 0.8f;
    }
    
    // 格式化时间显示
    public String getFormattedTimeRange() {
        return String.format("%.2fs - %.2fs", startTime, endTime);
    }
    
    // 获取置信度百分比
    public String getConfidencePercentage() {
        return String.format("%.1f%%", confidence * 100);
    }
    
    // 检查是否包含在指定时间范围内
    public boolean isWithinTimeRange(float start, float end) {
        return startTime >= start && endTime <= end;
    }
    
    // 检查是否与另一个词段重叠
    public boolean overlapsWith(WordSegment other) {
        if (other == null) return false;
        return !(endTime <= other.startTime || startTime >= other.endTime);
    }
    
    // 获取与另一个词段的重叠时长
    public float getOverlapDuration(WordSegment other) {
        if (!overlapsWith(other)) return 0.0f;
        
        float overlapStart = Math.max(startTime, other.startTime);
        float overlapEnd = Math.min(endTime, other.endTime);
        return overlapEnd - overlapStart;
    }
    
    @Override
    public String toString() {
        return String.format("WordSegment{word='%s', time=%.2f-%.2f, confidence=%.2f}", 
                           word, startTime, endTime, confidence);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WordSegment that = (WordSegment) obj;
        return Float.compare(that.startTime, startTime) == 0 &&
               Float.compare(that.endTime, endTime) == 0 &&
               Float.compare(that.confidence, confidence) == 0 &&
               word.equals(that.word);
    }
    
    @Override
    public int hashCode() {
        int result = word.hashCode();
        result = 31 * result + Float.hashCode(startTime);
        result = 31 * result + Float.hashCode(endTime);
        result = 31 * result + Float.hashCode(confidence);
        return result;
    }
}