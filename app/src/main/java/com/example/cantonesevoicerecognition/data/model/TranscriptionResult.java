package com.example.cantonesevoicerecognition.data.model;

import com.example.cantonesevoicerecognition.engine.TranscriptionError;
import java.util.List;
import java.util.ArrayList;

public class TranscriptionResult {
    private String text;
    private float confidence;
    private long processingTime;
    private List<WordSegment> segments;
    private boolean isComplete;
    private TranscriptionError error;
    
    // 构造函数
    public TranscriptionResult() {
        this.text = "";
        this.confidence = 0.0f;
        this.processingTime = 0;
        this.segments = new ArrayList<>();
        this.isComplete = false;
        this.error = null;
    }
    
    public TranscriptionResult(String text, float confidence) {
        this();
        this.text = text;
        this.confidence = confidence;
        this.isComplete = true;
    }
    
    public TranscriptionResult(String text, float confidence, long processingTime, 
                             List<WordSegment> segments) {
        this.text = text;
        this.confidence = confidence;
        this.processingTime = processingTime;
        this.segments = segments != null ? segments : new ArrayList<>();
        this.isComplete = true;
        this.error = null;
    }
    
    // Getter方法
    public String getText() {
        return text;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public long getProcessingTime() {
        return processingTime;
    }
    
    public List<WordSegment> getSegments() {
        return segments;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public TranscriptionError getError() {
        return error;
    }
    
    // Setter方法
    public void setText(String text) {
        this.text = text;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }
    
    public void setSegments(List<WordSegment> segments) {
        this.segments = segments != null ? segments : new ArrayList<>();
    }
    
    public void setComplete(boolean complete) {
        this.isComplete = complete;
    }
    
    public void setError(TranscriptionError error) {
        this.error = error;
    }
    
    // 结果验证和格式化方法
    public boolean isValid() {
        return text != null && !text.trim().isEmpty() && error == null;
    }
    
    public boolean hasError() {
        return error != null;
    }
    
    public boolean isHighConfidence() {
        return confidence >= 0.8f;
    }
    
    public boolean hasSegments() {
        return segments != null && !segments.isEmpty();
    }
    
    // 添加词段
    public void addSegment(WordSegment segment) {
        if (segments == null) {
            segments = new ArrayList<>();
        }
        segments.add(segment);
    }
    
    // 获取词数
    public int getWordCount() {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        // 简单的中文分词计数
        return text.replaceAll("[\\s\\p{Punct}]", "").length();
    }
    
    // 获取字符数
    public int getCharacterCount() {
        return text != null ? text.length() : 0;
    }
    
    // 格式化处理时间
    public String getFormattedProcessingTime() {
        if (processingTime < 1000) {
            return processingTime + "ms";
        } else {
            return String.format("%.1fs", processingTime / 1000.0);
        }
    }
    
    // 获取置信度百分比
    public String getConfidencePercentage() {
        return String.format("%.1f%%", confidence * 100);
    }
    
    // 创建错误结果
    public static TranscriptionResult createErrorResult(TranscriptionError error) {
        TranscriptionResult result = new TranscriptionResult();
        result.setError(error);
        result.setComplete(true);
        return result;
    }
    
    // 创建部分结果
    public static TranscriptionResult createPartialResult(String partialText, float confidence) {
        TranscriptionResult result = new TranscriptionResult();
        result.setText(partialText);
        result.setConfidence(confidence);
        result.setComplete(false);
        return result;
    }
}