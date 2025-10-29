package com.example.cantonesevoicerecognition.audio;

/**
 * 音频流监听器接口
 * 用于接收音频录制过程中的各种事件和数据
 */
public interface AudioStreamListener {
    
    /**
     * 当有新的音频数据可用时调用
     * @param audioData 音频数据字节数组
     */
    void onAudioDataAvailable(byte[] audioData);
    
    /**
     * 当录音开始时调用
     */
    void onRecordingStarted();
    
    /**
     * 当录音停止时调用
     */
    void onRecordingStopped();
    
    /**
     * 当录音暂停时调用
     */
    default void onRecordingPaused() {
        // 默认空实现
    }
    
    /**
     * 当录音恢复时调用
     */
    default void onRecordingResumed() {
        // 默认空实现
    }
    
    /**
     * 当录音过程中发生错误时调用
     * @param errorMessage 错误信息
     */
    void onRecordingError(String errorMessage);
    
    /**
     * 当音频质量发生变化时调用（可选）
     * @param quality 音频质量指标（0.0-1.0）
     */
    default void onAudioQualityChanged(float quality) {
        // 默认空实现
    }
    
    /**
     * 当检测到音频音量变化时调用（可选）
     * @param volume 音量级别（0.0-1.0）
     */
    default void onVolumeChanged(float volume) {
        // 默认空实现
    }
}