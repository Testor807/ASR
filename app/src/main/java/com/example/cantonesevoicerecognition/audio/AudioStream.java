package com.example.cantonesevoicerecognition.audio;

import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频流类
 * 用于管理音频数据流和监听器
 */
public class AudioStream {
    private static final String TAG = "AudioStream";
    
    private AudioStreamListener listener;
    private AtomicBoolean isActive = new AtomicBoolean(false);
    private AudioRecorderManager recorderManager;
    
    /**
     * 构造函数
     * @param recorderManager 音频录制管理器
     */
    public AudioStream(AudioRecorderManager recorderManager) {
        this.recorderManager = recorderManager;
    }
    
    /**
     * 设置音频流监听器
     * @param listener 监听器
     */
    public void setListener(AudioStreamListener listener) {
        this.listener = listener;
        
        // 将监听器设置到录制管理器
        if (recorderManager != null) {
            recorderManager.setAudioStreamListener(listener);
        }
    }
    
    /**
     * 开始音频流
     * @return 是否成功开始
     */
    public boolean start() {
        if (isActive.get()) {
            Log.w(TAG, "AudioStream is already active");
            return true;
        }
        
        if (recorderManager == null) {
            Log.e(TAG, "RecorderManager is null");
            return false;
        }
        
        boolean started = recorderManager.startRecording();
        if (started) {
            isActive.set(true);
            Log.i(TAG, "AudioStream started");
        } else {
            Log.e(TAG, "Failed to start AudioStream");
        }
        
        return started;
    }
    
    /**
     * 停止音频流
     */
    public void stop() {
        if (!isActive.get()) {
            Log.w(TAG, "AudioStream is not active");
            return;
        }
        
        if (recorderManager != null) {
            recorderManager.stopRecording();
        }
        
        isActive.set(false);
        Log.i(TAG, "AudioStream stopped");
    }
    
    /**
     * 暂停音频流
     */
    public void pause() {
        if (recorderManager != null && isActive.get()) {
            recorderManager.pauseRecording();
            Log.i(TAG, "AudioStream paused");
        }
    }
    
    /**
     * 恢复音频流
     */
    public void resume() {
        if (recorderManager != null && isActive.get()) {
            recorderManager.resumeRecording();
            Log.i(TAG, "AudioStream resumed");
        }
    }
    
    /**
     * 检查音频流是否活跃
     * @return 是否活跃
     */
    public boolean isActive() {
        return isActive.get();
    }
    
    /**
     * 获取录制时长
     * @return 录制时长（毫秒）
     */
    public long getDuration() {
        return recorderManager != null ? recorderManager.getRecordingDuration() : 0;
    }
    
    /**
     * 获取音频配置信息
     * @return 配置信息
     */
    public String getAudioConfig() {
        return recorderManager != null ? recorderManager.getAudioConfig() : "未配置";
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        
        if (recorderManager != null) {
            recorderManager.release();
        }
        
        listener = null;
        Log.i(TAG, "AudioStream released");
    }
}