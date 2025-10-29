package com.example.cantonesevoicerecognition.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音频录制管理器
 * 负责管理设备麦克风输入和音频数据采集
 */
public class AudioRecorderManager {
    private static final String TAG = "AudioRecorderManager";
    
    // 音频配置常量
    private static final int SAMPLE_RATE = 16000; // 16kHz采样率
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 16位PCM
    private static final int BUFFER_SIZE_MULTIPLIER = 2; // 缓冲区大小倍数
    
    private Context context;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private AudioStreamListener listener;
    private ExecutorService recordingExecutor;
    private int bufferSize;
    private long recordingStartTime;
    private long totalRecordingTime;
    
    /**
     * 构造函数
     * @param context 应用上下文
     */
    public AudioRecorderManager(Context context) {
        this.context = context.getApplicationContext();
        this.recordingExecutor = Executors.newSingleThreadExecutor();
        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        
        // 确保缓冲区大小有效
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            bufferSize = SAMPLE_RATE * 2; // 1秒的缓冲区作为后备
        } else {
            bufferSize *= BUFFER_SIZE_MULTIPLIER; // 增加缓冲区大小以提高稳定性
        }
        
        Log.i(TAG, "AudioRecorderManager initialized with buffer size: " + bufferSize);
    }
    
    /**
     * 设置音频流监听器
     * @param listener 音频流监听器
     */
    public void setAudioStreamListener(AudioStreamListener listener) {
        this.listener = listener;
    }
    
    /**
     * 开始录音
     * @return 是否成功开始录音
     */
    public boolean startRecording() {
        if (isRecording) {
            Log.w(TAG, "Recording is already in progress");
            return true;
        }
        
        if (!checkPermissions()) {
            Log.e(TAG, "Recording permission not granted");
            if (listener != null) {
                listener.onRecordingError("录音权限未授予");
            }
            return false;
        }
        
        try {
            // 创建AudioRecord实例
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            // 检查AudioRecord状态
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                if (listener != null) {
                    listener.onRecordingError("音频录制器初始化失败");
                }
                return false;
            }
            
            // 开始录音
            audioRecord.startRecording();
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            
            // 在后台线程中执行录音循环
            recordingExecutor.execute(this::recordingLoop);
            
            Log.i(TAG, "Recording started successfully");
            if (listener != null) {
                listener.onRecordingStarted();
            }
            
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when starting recording", e);
            if (listener != null) {
                listener.onRecordingError("录音权限被拒绝");
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception when starting recording", e);
            if (listener != null) {
                listener.onRecordingError("录音启动失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 暂停录音
     */
    public void pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true;
            totalRecordingTime += System.currentTimeMillis() - recordingStartTime;
            Log.i(TAG, "Recording paused");
            if (listener != null) {
                listener.onRecordingPaused();
            }
        }
    }
    
    /**
     * 恢复录音
     */
    public void resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            Log.i(TAG, "Recording resumed");
            if (listener != null) {
                listener.onRecordingResumed();
            }
        }
    }
    
    /**
     * 停止录音
     */
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress");
            return;
        }
        
        isRecording = false;
        isPaused = false;
        
        if (!isPaused) {
            totalRecordingTime += System.currentTimeMillis() - recordingStartTime;
        }
        
        try {
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }
            
            Log.i(TAG, "Recording stopped. Total duration: " + totalRecordingTime + "ms");
            if (listener != null) {
                listener.onRecordingStopped();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            if (listener != null) {
                listener.onRecordingError("停止录音时发生错误: " + e.getMessage());
            }
        }
        
        // 重置计时器
        totalRecordingTime = 0;
    }
    
    /**
     * 录音循环 - 在后台线程中运行
     */
    private void recordingLoop() {
        byte[] buffer = new byte[bufferSize];
        
        Log.i(TAG, "Recording loop started");
        
        while (isRecording) {
            try {
                if (!isPaused && audioRecord != null) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        // 创建音频数据副本
                        byte[] audioData = Arrays.copyOf(buffer, bytesRead);
                        
                        // 通知监听器有新的音频数据
                        if (listener != null) {
                            listener.onAudioDataAvailable(audioData);
                        }
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "Invalid operation during recording");
                        break;
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "Bad value during recording");
                        break;
                    }
                } else {
                    // 暂停时短暂休眠
                    Thread.sleep(10);
                }
                
            } catch (InterruptedException e) {
                Log.i(TAG, "Recording loop interrupted");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in recording loop", e);
                if (listener != null) {
                    listener.onRecordingError("录音过程中发生错误: " + e.getMessage());
                }
                break;
            }
        }
        
        Log.i(TAG, "Recording loop ended");
    }
    
    /**
     * 检查录音权限
     * @return 是否有录音权限
     */
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 获取当前录音状态
     * @return 是否正在录音
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * 获取当前暂停状态
     * @return 是否已暂停
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * 获取录音时长
     * @return 录音时长（毫秒）
     */
    public long getRecordingDuration() {
        if (isRecording && !isPaused) {
            return totalRecordingTime + (System.currentTimeMillis() - recordingStartTime);
        }
        return totalRecordingTime;
    }
    
    /**
     * 获取音频配置信息
     * @return 音频配置字符串
     */
    public String getAudioConfig() {
        return String.format("采样率: %dHz, 声道: 单声道, 位深: 16位, 缓冲区: %d字节", 
                           SAMPLE_RATE, bufferSize);
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopRecording();
        
        if (recordingExecutor != null && !recordingExecutor.isShutdown()) {
            recordingExecutor.shutdown();
        }
        
        Log.i(TAG, "AudioRecorderManager released");
    }
}