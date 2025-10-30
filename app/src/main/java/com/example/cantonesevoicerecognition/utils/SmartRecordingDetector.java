package com.example.cantonesevoicerecognition.utils;

import android.util.Log;
import com.example.cantonesevoicerecognition.audio.AudioProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能录音检测器
 * 通过语音活动检测(VAD)和智能算法优化录音过程，节省电池
 */
public class SmartRecordingDetector {
    private static final String TAG = "SmartRecordingDetector";
    
    // VAD配置参数
    private static final float DEFAULT_ENERGY_THRESHOLD = 1000.0f;
    private static final int SILENCE_DURATION_MS = 2000; // 2秒静音后停止录音
    private static final int MIN_SPEECH_DURATION_MS = 500; // 最小语音持续时间
    private static final int ENERGY_WINDOW_SIZE = 10; // 能量窗口大小
    
    // 自适应阈值参数
    private static final float THRESHOLD_ADAPTATION_RATE = 0.1f;
    private static final float MIN_THRESHOLD_MULTIPLIER = 0.5f;
    private static final float MAX_THRESHOLD_MULTIPLIER = 3.0f;
    
    private final List<Float> energyHistory;
    private final List<Long> speechSegments;
    private final AtomicBoolean isDetecting;
    
    private float currentEnergyThreshold;
    private float backgroundNoiseLevel;
    private long lastSpeechTime;
    private long currentSpeechStart;
    private boolean isSpeechActive;
    private int consecutiveSilenceFrames;
    private int consecutiveSpeechFrames;
    
    // 检测状态
    private DetectionMode detectionMode = DetectionMode.ADAPTIVE;
    private boolean isLearningMode = true;
    private int learningFrameCount = 0;
    private static final int LEARNING_FRAMES = 50; // 学习前50帧来确定背景噪音
    
    /**
     * 检测模式
     */
    public enum DetectionMode {
        SIMPLE,     // 简单阈值检测
        ADAPTIVE,   // 自适应阈值检测
        ADVANCED    // 高级检测（包含频域分析）
    }
    
    /**
     * 检测结果
     */
    public static class DetectionResult {
        public final boolean isSpeechDetected;
        public final float confidence;
        public final float energyLevel;
        public final long timestamp;
        public final boolean shouldStartRecording;
        public final boolean shouldStopRecording;
        
        public DetectionResult(boolean isSpeechDetected, float confidence, float energyLevel, 
                             long timestamp, boolean shouldStartRecording, boolean shouldStopRecording) {
            this.isSpeechDetected = isSpeechDetected;
            this.confidence = confidence;
            this.energyLevel = energyLevel;
            this.timestamp = timestamp;
            this.shouldStartRecording = shouldStartRecording;
            this.shouldStopRecording = shouldStopRecording;
        }
        
        @Override
        public String toString() {
            return String.format("DetectionResult{speech=%s, confidence=%.2f, energy=%.2f, start=%s, stop=%s}",
                               isSpeechDetected, confidence, energyLevel, shouldStartRecording, shouldStopRecording);
        }
    }
    
    /**
     * 检测监听器
     */
    public interface DetectionListener {
        void onSpeechStarted(long timestamp);
        void onSpeechEnded(long timestamp, long duration);
        void onSilenceDetected(long duration);
        void onBackgroundNoiseUpdated(float noiseLevel);
    }
    
    private DetectionListener listener;
    
    public SmartRecordingDetector() {
        this.energyHistory = new ArrayList<>();
        this.speechSegments = new ArrayList<>();
        this.isDetecting = new AtomicBoolean(false);
        this.currentEnergyThreshold = DEFAULT_ENERGY_THRESHOLD;
        this.backgroundNoiseLevel = 0.0f;
        this.lastSpeechTime = 0;
        this.currentSpeechStart = 0;
        this.isSpeechActive = false;
        this.consecutiveSilenceFrames = 0;
        this.consecutiveSpeechFrames = 0;
        
        Log.i(TAG, "SmartRecordingDetector initialized");
    }
    
    /**
     * 设置检测监听器
     */
    public void setDetectionListener(DetectionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置检测模式
     */
    public void setDetectionMode(DetectionMode mode) {
        this.detectionMode = mode;
        Log.i(TAG, "Detection mode set to: " + mode);
    }
    
    /**
     * 开始检测
     */
    public void startDetection() {
        if (isDetecting.compareAndSet(false, true)) {
            reset();
            Log.i(TAG, "Smart recording detection started");
        }
    }
    
    /**
     * 停止检测
     */
    public void stopDetection() {
        if (isDetecting.compareAndSet(true, false)) {
            Log.i(TAG, "Smart recording detection stopped");
        }
    }
    
    /**
     * 重置检测状态
     */
    public void reset() {
        energyHistory.clear();
        speechSegments.clear();
        backgroundNoiseLevel = 0.0f;
        lastSpeechTime = 0;
        currentSpeechStart = 0;
        isSpeechActive = false;
        consecutiveSilenceFrames = 0;
        consecutiveSpeechFrames = 0;
        isLearningMode = true;
        learningFrameCount = 0;
        currentEnergyThreshold = DEFAULT_ENERGY_THRESHOLD;
        
        Log.d(TAG, "Detection state reset");
    }
    
    /**
     * 处理音频数据并进行检测
     */
    public DetectionResult processAudioData(byte[] audioData, int sampleRate) {
        if (!isDetecting.get() || audioData == null || audioData.length == 0) {
            return new DetectionResult(false, 0.0f, 0.0f, System.currentTimeMillis(), false, false);
        }
        
        long timestamp = System.currentTimeMillis();
        
        // 计算音频能量
        float energy = AudioProcessor.calculateAudioEnergy(audioData);
        
        // 更新能量历史
        updateEnergyHistory(energy);
        
        // 学习背景噪音
        if (isLearningMode) {
            updateBackgroundNoise(energy);
            learningFrameCount++;
            
            if (learningFrameCount >= LEARNING_FRAMES) {
                isLearningMode = false;
                adaptThreshold();
                Log.i(TAG, String.format("Learning completed. Background noise: %.2f, Threshold: %.2f", 
                                        backgroundNoiseLevel, currentEnergyThreshold));
                
                if (listener != null) {
                    listener.onBackgroundNoiseUpdated(backgroundNoiseLevel);
                }
            }
            
            // 学习期间不进行语音检测
            return new DetectionResult(false, 0.0f, energy, timestamp, false, false);
        }
        
        // 执行语音检测
        DetectionResult result = performDetection(energy, timestamp);
        
        // 更新检测状态
        updateDetectionState(result);
        
        // 自适应阈值调整
        if (detectionMode == DetectionMode.ADAPTIVE) {
            adaptThreshold();
        }
        
        return result;
    }
    
    /**
     * 执行语音检测
     */
    private DetectionResult performDetection(float energy, long timestamp) {
        boolean speechDetected = false;
        float confidence = 0.0f;
        boolean shouldStartRecording = false;
        boolean shouldStopRecording = false;
        
        switch (detectionMode) {
            case SIMPLE:
                speechDetected = energy > currentEnergyThreshold;
                confidence = speechDetected ? Math.min(energy / currentEnergyThreshold, 2.0f) : 0.0f;
                break;
                
            case ADAPTIVE:
                speechDetected = performAdaptiveDetection(energy);
                confidence = calculateConfidence(energy);
                break;
                
            case ADVANCED:
                speechDetected = performAdvancedDetection(energy);
                confidence = calculateAdvancedConfidence(energy);
                break;
        }
        
        // 决定是否开始/停止录音
        if (speechDetected) {
            consecutiveSpeechFrames++;
            consecutiveSilenceFrames = 0;
            
            if (!isSpeechActive && consecutiveSpeechFrames >= 3) {
                // 检测到语音开始
                shouldStartRecording = true;
                isSpeechActive = true;
                currentSpeechStart = timestamp;
                lastSpeechTime = timestamp;
                
                if (listener != null) {
                    listener.onSpeechStarted(timestamp);
                }
            } else if (isSpeechActive) {
                lastSpeechTime = timestamp;
            }
        } else {
            consecutiveSilenceFrames++;
            consecutiveSpeechFrames = 0;
            
            if (isSpeechActive) {
                long silenceDuration = timestamp - lastSpeechTime;
                
                if (silenceDuration > SILENCE_DURATION_MS) {
                    // 检测到语音结束
                    shouldStopRecording = true;
                    isSpeechActive = false;
                    
                    long speechDuration = lastSpeechTime - currentSpeechStart;
                    if (speechDuration >= MIN_SPEECH_DURATION_MS) {
                        speechSegments.add(speechDuration);
                    }
                    
                    if (listener != null) {
                        listener.onSpeechEnded(timestamp, speechDuration);
                        listener.onSilenceDetected(silenceDuration);
                    }
                }
            }
        }
        
        return new DetectionResult(speechDetected, confidence, energy, timestamp, 
                                 shouldStartRecording, shouldStopRecording);
    }
    
    /**
     * 自适应检测
     */
    private boolean performAdaptiveDetection(float energy) {
        // 基于背景噪音的动态阈值
        float adaptiveThreshold = backgroundNoiseLevel * 2.0f + currentEnergyThreshold * 0.3f;
        
        // 考虑最近的能量变化
        if (energyHistory.size() >= 3) {
            float recentAverage = 0.0f;
            for (int i = energyHistory.size() - 3; i < energyHistory.size(); i++) {
                recentAverage += energyHistory.get(i);
            }
            recentAverage /= 3.0f;
            
            // 如果最近能量显著高于背景噪音，降低阈值
            if (recentAverage > backgroundNoiseLevel * 1.5f) {
                adaptiveThreshold *= 0.8f;
            }
        }
        
        return energy > adaptiveThreshold;
    }
    
    /**
     * 高级检测（可以扩展为频域分析）
     */
    private boolean performAdvancedDetection(float energy) {
        // 目前使用改进的自适应检测
        // 未来可以添加频域分析、零交叉率等特征
        
        boolean energyDetection = performAdaptiveDetection(energy);
        
        // 添加连续性检查
        if (energyDetection && energyHistory.size() >= 5) {
            // 检查能量是否持续上升或保持高位
            int risingCount = 0;
            for (int i = energyHistory.size() - 4; i < energyHistory.size(); i++) {
                if (energyHistory.get(i) > backgroundNoiseLevel * 1.2f) {
                    risingCount++;
                }
            }
            
            return risingCount >= 3;
        }
        
        return energyDetection;
    }
    
    /**
     * 计算置信度
     */
    private float calculateConfidence(float energy) {
        if (energy <= backgroundNoiseLevel) {
            return 0.0f;
        }
        
        float ratio = energy / Math.max(currentEnergyThreshold, backgroundNoiseLevel * 2.0f);
        return Math.min(ratio, 1.0f);
    }
    
    /**
     * 计算高级置信度
     */
    private float calculateAdvancedConfidence(float energy) {
        float baseConfidence = calculateConfidence(energy);
        
        // 基于历史数据调整置信度
        if (energyHistory.size() >= 5) {
            float stability = calculateEnergyStability();
            baseConfidence *= (0.7f + stability * 0.3f);
        }
        
        return Math.min(baseConfidence, 1.0f);
    }
    
    /**
     * 计算能量稳定性
     */
    private float calculateEnergyStability() {
        if (energyHistory.size() < 5) {
            return 0.5f;
        }
        
        float mean = 0.0f;
        int startIndex = Math.max(0, energyHistory.size() - 5);
        
        for (int i = startIndex; i < energyHistory.size(); i++) {
            mean += energyHistory.get(i);
        }
        mean /= (energyHistory.size() - startIndex);
        
        float variance = 0.0f;
        for (int i = startIndex; i < energyHistory.size(); i++) {
            float diff = energyHistory.get(i) - mean;
            variance += diff * diff;
        }
        variance /= (energyHistory.size() - startIndex);
        
        float stability = 1.0f / (1.0f + variance / (mean * mean + 1.0f));
        return Math.max(0.0f, Math.min(1.0f, stability));
    }
    
    /**
     * 更新能量历史
     */
    private void updateEnergyHistory(float energy) {
        energyHistory.add(energy);
        
        // 限制历史记录大小
        if (energyHistory.size() > 100) {
            energyHistory.remove(0);
        }
    }
    
    /**
     * 更新背景噪音水平
     */
    private void updateBackgroundNoise(float energy) {
        if (learningFrameCount == 0) {
            backgroundNoiseLevel = energy;
        } else {
            // 使用指数移动平均
            backgroundNoiseLevel = backgroundNoiseLevel * 0.9f + energy * 0.1f;
        }
    }
    
    /**
     * 自适应阈值调整
     */
    private void adaptThreshold() {
        if (energyHistory.size() < ENERGY_WINDOW_SIZE) {
            return;
        }
        
        // 计算最近窗口的平均能量
        float recentAverage = 0.0f;
        int startIndex = energyHistory.size() - ENERGY_WINDOW_SIZE;
        
        for (int i = startIndex; i < energyHistory.size(); i++) {
            recentAverage += energyHistory.get(i);
        }
        recentAverage /= ENERGY_WINDOW_SIZE;
        
        // 基于背景噪音和最近平均值调整阈值
        float targetThreshold = Math.max(
            backgroundNoiseLevel * MIN_THRESHOLD_MULTIPLIER,
            Math.min(recentAverage * 1.2f, backgroundNoiseLevel * MAX_THRESHOLD_MULTIPLIER)
        );
        
        // 平滑调整阈值
        currentEnergyThreshold = currentEnergyThreshold * (1.0f - THRESHOLD_ADAPTATION_RATE) + 
                               targetThreshold * THRESHOLD_ADAPTATION_RATE;
    }
    
    /**
     * 更新检测状态
     */
    private void updateDetectionState(DetectionResult result) {
        // 记录性能指标
        PerformanceMonitor.getInstance().recordMeasurement("vad_detection", 1);
        
        if (result.isSpeechDetected) {
            PerformanceMonitor.getInstance().recordMeasurement("speech_detected", 1);
        }
    }
    
    /**
     * 获取检测统计信息
     */
    public String getDetectionStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Smart Recording Detection Stats:\n");
        stats.append("Detection Mode: ").append(detectionMode).append("\n");
        stats.append("Background Noise: ").append(String.format("%.2f", backgroundNoiseLevel)).append("\n");
        stats.append("Current Threshold: ").append(String.format("%.2f", currentEnergyThreshold)).append("\n");
        stats.append("Speech Active: ").append(isSpeechActive).append("\n");
        stats.append("Speech Segments: ").append(speechSegments.size()).append("\n");
        
        if (!speechSegments.isEmpty()) {
            long totalDuration = speechSegments.stream().mapToLong(Long::longValue).sum();
            long averageDuration = totalDuration / speechSegments.size();
            stats.append("Average Speech Duration: ").append(averageDuration).append("ms\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 获取当前背景噪音水平
     */
    public float getBackgroundNoiseLevel() {
        return backgroundNoiseLevel;
    }
    
    /**
     * 获取当前阈值
     */
    public float getCurrentThreshold() {
        return currentEnergyThreshold;
    }
    
    /**
     * 检查是否正在检测语音
     */
    public boolean isSpeechActive() {
        return isSpeechActive;
    }
    
    /**
     * 手动设置阈值
     */
    public void setEnergyThreshold(float threshold) {
        this.currentEnergyThreshold = threshold;
        Log.i(TAG, "Energy threshold manually set to: " + threshold);
    }
    
    /**
     * 获取语音段统计
     */
    public List<Long> getSpeechSegments() {
        return new ArrayList<>(speechSegments);
    }
    
    /**
     * 清除语音段历史
     */
    public void clearSpeechHistory() {
        speechSegments.clear();
        Log.d(TAG, "Speech segment history cleared");
    }
}