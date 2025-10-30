package com.example.cantonesevoicerecognition.engine;

import android.util.Log;
import com.example.cantonesevoicerecognition.audio.AudioBuffer;
import com.example.cantonesevoicerecognition.audio.AudioProcessor;
import com.example.cantonesevoicerecognition.audio.AudioStream;
import com.example.cantonesevoicerecognition.audio.AudioStreamListener;
import com.example.cantonesevoicerecognition.data.model.AudioData;
import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;
import com.example.cantonesevoicerecognition.native.WhisperJNI;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whisper语音识别引擎
 * 提供音频转录功能的Java层封装
 */
public class WhisperEngine {
    private static final String TAG = "WhisperEngine";
    
    // 引擎状态
    private WhisperJNI whisperJNI;
    private boolean isModelLoaded = false;
    private String currentModelPath;
    private ExecutorService executorService;
    private AtomicBoolean isTranscribing = new AtomicBoolean(false);
    
    // 实时转录相关
    private Future<?> realTimeTask;
    private AudioBuffer realTimeBuffer;
    private volatile boolean isRealTimeActive = false;
    
    /**
     * 构造函数
     */
    public WhisperEngine() {
        whisperJNI = new WhisperJNI();
        executorService = Executors.newSingleThreadExecutor();
        realTimeBuffer = new AudioBuffer(3000); // 3秒缓冲
        
        Log.i(TAG, "WhisperEngine initialized");
    }
    
    /**
     * 初始化Whisper模型
     * @param modelPath 模型文件路径
     * @return 是否初始化成功
     */
    public boolean initializeModel(String modelPath) {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            Log.e(TAG, "Model path is null or empty");
            return false;
        }
        
        // 检查模型文件是否存在
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found: " + modelPath);
            return false;
        }
        
        if (!modelFile.canRead()) {
            Log.e(TAG, "Cannot read model file: " + modelPath);
            return false;
        }
        
        Log.i(TAG, "Initializing Whisper model: " + modelPath);
        
        try {
            // 如果已经加载了模型，先释放
            if (isModelLoaded) {
                releaseModel();
            }
            
            // 初始化新模型
            isModelLoaded = whisperJNI.initializeModel(modelPath);
            
            if (isModelLoaded) {
                currentModelPath = modelPath;
                Log.i(TAG, "Model loaded successfully: " + modelPath);
            } else {
                Log.e(TAG, "Failed to load model: " + modelPath);
            }
            
            return isModelLoaded;
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during model initialization", e);
            isModelLoaded = false;
            return false;
        }
    }
    
    /**
     * 转录音频数据
     * @param audioData 音频数据
     * @param callback 转录回调
     */
    public void transcribe(AudioData audioData, TranscriptionCallback callback) {
        if (callback == null) {
            Log.w(TAG, "Transcription callback is null");
            return;
        }
        
        if (!isModelLoaded) {
            Log.e(TAG, "Model not loaded");
            callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
            return;
        }
        
        if (audioData == null || audioData.isEmpty()) {
            Log.w(TAG, "Audio data is null or empty");
            callback.onTranscriptionError(TranscriptionError.AUDIO_FORMAT_UNSUPPORTED);
            return;
        }
        
        if (!audioData.isValidFormat()) {
            Log.w(TAG, "Invalid audio format");
            callback.onTranscriptionError(TranscriptionError.AUDIO_FORMAT_UNSUPPORTED);
            return;
        }
        
        // 检查是否已经在转录中
        if (!isTranscribing.compareAndSet(false, true)) {
            Log.w(TAG, "Transcription already in progress");
            callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
            return;
        }
        
        executorService.execute(() -> {
            try {
                callback.onTranscriptionStarted();
                
                long startTime = System.currentTimeMillis();
                
                // 转换音频数据为Whisper所需格式
                AudioData processedAudio = AudioProcessor.convertToWhisperFormat(
                    audioData.getRawData(), audioData.getSampleRate());
                
                if (processedAudio.isEmpty()) {
                    callback.onTranscriptionError(TranscriptionError.AUDIO_FORMAT_UNSUPPORTED);
                    return;
                }
                
                Log.d(TAG, "Starting transcription for " + processedAudio.getDataSize() + " bytes");
                
                // 调用native方法进行转录
                String result = whisperJNI.transcribeAudio(processedAudio.getRawData());
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (result != null && !result.trim().isEmpty()) {
                    // 创建转录结果
                    TranscriptionResult transcriptionResult = new TranscriptionResult();
                    transcriptionResult.setText(result.trim());
                    transcriptionResult.setProcessingTime(processingTime);
                    transcriptionResult.setComplete(true);
                    transcriptionResult.setConfidence(calculateConfidence(result));
                    
                    Log.i(TAG, "Transcription completed: \"" + result + "\" (took " + processingTime + "ms)");
                    callback.onTranscriptionCompleted(transcriptionResult);
                } else {
                    Log.w(TAG, "Transcription returned empty result");
                    callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Transcription failed", e);
                callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
            } finally {
                isTranscribing.set(false);
            }
        });
    }
    
    /**
     * 开始实时转录
     * @param audioStream 音频流
     * @param callback 转录回调
     */
    public void startRealTimeTranscription(AudioStream audioStream, TranscriptionCallback callback) {
        if (!isModelLoaded) {
            Log.e(TAG, "Model not loaded for real-time transcription");
            if (callback != null) {
                callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
            }
            return;
        }
        
        if (isRealTimeActive) {
            Log.w(TAG, "Real-time transcription already active");
            return;
        }
        
        isRealTimeActive = true;
        realTimeBuffer.clear();
        
        Log.i(TAG, "Starting real-time transcription");
        
        // 设置音频流监听器
        if (audioStream != null) {
            audioStream.setListener(new AudioStreamListener() {
                @Override
                public void onAudioDataAvailable(byte[] audioData) {
                    if (isRealTimeActive && audioData != null && audioData.length > 0) {
                        realTimeBuffer.addAudioData(audioData);
                        
                        // 检测语音活动
                        if (AudioProcessor.detectVoiceActivity(audioData, 1000.0f)) {
                            processRealTimeAudio(callback);
                        }
                    }
                }
                
                @Override
                public void onRecordingStarted() {
                    if (callback != null) {
                        callback.onTranscriptionStarted();
                    }
                }
                
                @Override
                public void onRecordingStopped() {
                    stopRealTimeTranscription();
                }
                
                @Override
                public void onRecordingError(String errorMessage) {
                    Log.e(TAG, "Recording error in real-time transcription: " + errorMessage);
                    if (callback != null) {
                        callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                    }
                    stopRealTimeTranscription();
                }
            });
            
            // 启动音频流
            if (!audioStream.start()) {
                Log.e(TAG, "Failed to start audio stream");
                isRealTimeActive = false;
                if (callback != null) {
                    callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                }
                return;
            }
        }
        
        // 启动实时处理任务
        realTimeTask = executorService.submit(() -> {
            while (isRealTimeActive) {
                try {
                    Thread.sleep(500); // 每500ms检查一次
                    
                    if (realTimeBuffer.getCurrentSize() > 16000 * 2) { // 超过1秒的音频数据
                        processRealTimeAudio(callback);
                    }
                    
                } catch (InterruptedException e) {
                    Log.i(TAG, "Real-time transcription task interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in real-time transcription task", e);
                    if (callback != null) {
                        callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                    }
                    break;
                }
            }
        });
    }
    
    /**
     * 停止实时转录
     */
    public void stopRealTimeTranscription() {
        if (!isRealTimeActive) {
            return;
        }
        
        Log.i(TAG, "Stopping real-time transcription");
        
        isRealTimeActive = false;
        
        if (realTimeTask != null && !realTimeTask.isDone()) {
            realTimeTask.cancel(true);
        }
        
        // 处理剩余的音频数据
        if (realTimeBuffer.getCurrentSize() > 0) {
            byte[] remainingAudio = realTimeBuffer.getBufferedAudio();
            if (remainingAudio != null && remainingAudio.length > 0) {
                // 可以选择处理剩余音频或忽略
                Log.d(TAG, "Discarding " + remainingAudio.length + " bytes of remaining audio");
            }
        }
        
        realTimeBuffer.clear();
    }
    
    /**
     * 处理实时音频数据
     */
    private void processRealTimeAudio(TranscriptionCallback callback) {
        if (!isRealTimeActive || callback == null) {
            return;
        }
        
        byte[] bufferedAudio = realTimeBuffer.getPartialAudio(0.6f); // 获取60%的缓冲数据
        
        if (bufferedAudio != null && bufferedAudio.length > 0) {
            try {
                AudioData audioData = AudioProcessor.convertToWhisperFormat(bufferedAudio, 16000);
                
                if (!audioData.isEmpty()) {
                    String partialResult = whisperJNI.transcribeAudio(audioData.getRawData());
                    
                    if (partialResult != null && !partialResult.trim().isEmpty()) {
                        Log.d(TAG, "Real-time partial result: " + partialResult);
                        callback.onPartialResult(partialResult.trim());
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing real-time audio", e);
            }
        }
    }
    
    /**
     * 计算转录置信度（简单实现）
     */
    private float calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0f;
        }
        
        // 简单的置信度计算：基于文本长度和字符类型
        float baseConfidence = 0.7f;
        
        // 文本越长，置信度稍微提高
        int length = text.trim().length();
        if (length > 10) {
            baseConfidence += 0.1f;
        }
        
        // 包含标点符号，置信度提高
        if (text.matches(".*[。！？，、；：].*")) {
            baseConfidence += 0.1f;
        }
        
        return Math.min(1.0f, baseConfidence);
    }
    
    /**
     * 检查模型是否已加载
     * @return 是否已加载
     */
    public boolean isModelLoaded() {
        return isModelLoaded;
    }
    
    /**
     * 获取当前模型路径
     * @return 模型路径
     */
    public String getCurrentModelPath() {
        return currentModelPath;
    }
    
    /**
     * 检查是否正在转录
     * @return 是否正在转录
     */
    public boolean isTranscribing() {
        return isTranscribing.get();
    }
    
    /**
     * 检查实时转录是否活跃
     * @return 是否活跃
     */
    public boolean isRealTimeActive() {
        return isRealTimeActive;
    }
    
    /**
     * 获取引擎状态信息
     * @return 状态信息
     */
    public String getEngineStatus() {
        return String.format("WhisperEngine - Model: %s, Loaded: %s, Transcribing: %s, RealTime: %s",
                           currentModelPath != null ? new File(currentModelPath).getName() : "None",
                           isModelLoaded,
                           isTranscribing.get(),
                           isRealTimeActive);
    }
    
    /**
     * 释放模型和资源
     */
    public void releaseModel() {
        Log.i(TAG, "Releasing Whisper model and resources");
        
        // 停止实时转录
        stopRealTimeTranscription();
        
        // 等待当前转录任务完成
        if (isTranscribing.get()) {
            Log.i(TAG, "Waiting for current transcription to complete...");
            try {
                Thread.sleep(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 释放native资源
        if (isModelLoaded) {
            try {
                whisperJNI.releaseModel();
                isModelLoaded = false;
                currentModelPath = null;
                Log.i(TAG, "Native model released");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing native model", e);
            }
        }
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 清理缓冲区
        if (realTimeBuffer != null) {
            realTimeBuffer.clear();
        }
        
        Log.i(TAG, "WhisperEngine resources released");
    }
    
    /**
     * 析构函数 - 确保资源被释放
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            releaseModel();
        } finally {
            super.finalize();
        }
    }
}