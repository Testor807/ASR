package com.example.cantonesevoicerecognition.utils;

import android.util.Log;
import com.example.cantonesevoicerecognition.audio.AudioProcessor;
import com.example.cantonesevoicerecognition.data.model.AudioData;

/**
 * 优化的音频处理器
 * 集成性能监控和优化功能的音频处理示例
 */
public class OptimizedAudioProcessor {
    private static final String TAG = "OptimizedAudioProcessor";
    
    private final PerformanceMonitor performanceMonitor;
    private final ThreadPoolManager threadPoolManager;
    private final MemoryLeakDetector leakDetector;
    
    public OptimizedAudioProcessor() {
        this.performanceMonitor = PerformanceMonitor.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.leakDetector = MemoryLeakDetector.getInstance();
        
        // 跟踪这个对象以检测内存泄漏
        leakDetector.trackObject(this, "OptimizedAudioProcessor");
        
        Log.i(TAG, "OptimizedAudioProcessor initialized");
    }
    
    /**
     * 优化的音频格式转换
     */
    public void convertAudioFormatAsync(byte[] audioData, int sampleRate, AudioConversionCallback callback) {
        if (audioData == null || callback == null) {
            Log.w(TAG, "Invalid parameters for audio conversion");
            return;
        }
        
        // 使用音频处理线程池执行转换
        threadPoolManager.executeAudioProcessing(() -> {
            performanceMonitor.measureBlock("audio_format_conversion", () -> {
                try {
                    // 执行音频格式转换
                    AudioData convertedAudio = AudioProcessor.convertToWhisperFormat(audioData, sampleRate);
                    
                    if (convertedAudio != null && !convertedAudio.isEmpty()) {
                        callback.onConversionSuccess(convertedAudio);
                    } else {
                        callback.onConversionError("转换结果为空");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Audio conversion failed", e);
                    callback.onConversionError("转换失败: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * 优化的音频能量计算
     */
    public void calculateAudioEnergyAsync(byte[] audioData, EnergyCalculationCallback callback) {
        if (audioData == null || callback == null) {
            Log.w(TAG, "Invalid parameters for energy calculation");
            return;
        }
        
        // 使用音频处理线程池执行计算
        threadPoolManager.executeAudioProcessing(() -> {
            performanceMonitor.measureBlock("audio_energy_calculation", () -> {
                try {
                    float energy = AudioProcessor.calculateAudioEnergy(audioData);
                    callback.onEnergyCalculated(energy);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Energy calculation failed", e);
                    callback.onCalculationError("能量计算失败: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * 优化的语音活动检测
     */
    public void detectVoiceActivityAsync(byte[] audioData, float threshold, VoiceActivityCallback callback) {
        if (audioData == null || callback == null) {
            Log.w(TAG, "Invalid parameters for voice activity detection");
            return;
        }
        
        // 使用音频处理线程池执行检测
        threadPoolManager.executeAudioProcessing(() -> {
            performanceMonitor.measureBlock("voice_activity_detection", () -> {
                try {
                    boolean voiceDetected = AudioProcessor.detectVoiceActivity(audioData, threshold);
                    float confidence = calculateVoiceConfidence(audioData, threshold);
                    
                    callback.onVoiceActivityDetected(voiceDetected, confidence);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Voice activity detection failed", e);
                    callback.onDetectionError("语音检测失败: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * 批量音频处理（优化版本）
     */
    public void processBatchAudioAsync(byte[][] audioBatch, BatchProcessingCallback callback) {
        if (audioBatch == null || audioBatch.length == 0 || callback == null) {
            Log.w(TAG, "Invalid parameters for batch processing");
            return;
        }
        
        // 使用后台线程池执行批量处理
        threadPoolManager.executeBackground(() -> {
            performanceMonitor.measureBlock("batch_audio_processing", () -> {
                try {
                    AudioData[] results = new AudioData[audioBatch.length];
                    int successCount = 0;
                    
                    for (int i = 0; i < audioBatch.length; i++) {
                        try {
                            if (audioBatch[i] != null && audioBatch[i].length > 0) {
                                results[i] = AudioProcessor.convertToWhisperFormat(audioBatch[i], 16000);
                                if (results[i] != null && !results[i].isEmpty()) {
                                    successCount++;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to process audio batch item " + i, e);
                            results[i] = null;
                        }
                        
                        // 报告进度
                        final int progress = (i + 1) * 100 / audioBatch.length;
                        callback.onProgressUpdate(progress);
                    }
                    
                    callback.onBatchProcessingComplete(results, successCount);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Batch processing failed", e);
                    callback.onBatchProcessingError("批量处理失败: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * 计算语音置信度
     */
    private float calculateVoiceConfidence(byte[] audioData, float threshold) {
        try {
            float energy = AudioProcessor.calculateAudioEnergy(audioData);
            
            if (energy <= threshold) {
                return 0.0f;
            }
            
            // 简单的置信度计算
            float ratio = energy / threshold;
            return Math.min(ratio / 3.0f, 1.0f); // 归一化到0-1范围
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to calculate voice confidence", e);
            return 0.0f;
        }
    }
    
    /**
     * 获取处理器性能统计
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("OptimizedAudioProcessor Performance Stats:\n");
        
        // 获取相关性能指标
        PerformanceMonitor.PerformanceMetric conversionMetric = 
            performanceMonitor.getMetric("audio_format_conversion");
        if (conversionMetric != null) {
            stats.append("Format Conversion: ").append(conversionMetric.getFormattedStats()).append("\n");
        }
        
        PerformanceMonitor.PerformanceMetric energyMetric = 
            performanceMonitor.getMetric("audio_energy_calculation");
        if (energyMetric != null) {
            stats.append("Energy Calculation: ").append(energyMetric.getFormattedStats()).append("\n");
        }
        
        PerformanceMonitor.PerformanceMetric vadMetric = 
            performanceMonitor.getMetric("voice_activity_detection");
        if (vadMetric != null) {
            stats.append("Voice Activity Detection: ").append(vadMetric.getFormattedStats()).append("\n");
        }
        
        PerformanceMonitor.PerformanceMetric batchMetric = 
            performanceMonitor.getMetric("batch_audio_processing");
        if (batchMetric != null) {
            stats.append("Batch Processing: ").append(batchMetric.getFormattedStats()).append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 清理资源
     */
    public void release() {
        // 停止跟踪对象
        leakDetector.untrackObject(this, "OptimizedAudioProcessor");
        
        Log.i(TAG, "OptimizedAudioProcessor released");
    }
    
    // 回调接口定义
    
    /**
     * 音频转换回调
     */
    public interface AudioConversionCallback {
        void onConversionSuccess(AudioData convertedAudio);
        void onConversionError(String error);
    }
    
    /**
     * 能量计算回调
     */
    public interface EnergyCalculationCallback {
        void onEnergyCalculated(float energy);
        void onCalculationError(String error);
    }
    
    /**
     * 语音活动检测回调
     */
    public interface VoiceActivityCallback {
        void onVoiceActivityDetected(boolean voiceDetected, float confidence);
        void onDetectionError(String error);
    }
    
    /**
     * 批量处理回调
     */
    public interface BatchProcessingCallback {
        void onProgressUpdate(int progress);
        void onBatchProcessingComplete(AudioData[] results, int successCount);
        void onBatchProcessingError(String error);
    }
}