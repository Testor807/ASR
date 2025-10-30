package com.example.cantonesevoicerecognition.service;

import android.util.Log;
import com.example.cantonesevoicerecognition.audio.AudioBuffer;
import com.example.cantonesevoicerecognition.audio.AudioProcessor;
import com.example.cantonesevoicerecognition.audio.AudioStream;
import com.example.cantonesevoicerecognition.audio.AudioStreamListener;
import com.example.cantonesevoicerecognition.data.model.AudioData;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;
import com.example.cantonesevoicerecognition.data.repository.RepositoryCallback;
import com.example.cantonesevoicerecognition.data.repository.TranscriptionRepository;
import com.example.cantonesevoicerecognition.engine.TranscriptionCallback;
import com.example.cantonesevoicerecognition.engine.TranscriptionError;
import com.example.cantonesevoicerecognition.engine.WhisperEngine;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 實時轉錄器
 * 處理流式音頻數據的實時轉錄功能
 */
public class RealTimeTranscriber {
    private static final String TAG = "RealTimeTranscriber";
    
    // 配置常量
    private static final int BUFFER_SIZE_MS = 3000; // 3秒緩衝
    private static final float VAD_THRESHOLD = 1200.0f; // 語音活動檢測閾值
    private static final int SENTENCE_TIMEOUT_MS = 2000; // 句子完成超時
    private static final int MIN_AUDIO_LENGTH_MS = 500; // 最小音頻長度
    private static final int PROCESSING_INTERVAL_MS = 800; // 處理間隔
    
    private WhisperEngine whisperEngine;
    private TranscriptionRepository repository;
    private AudioBuffer audioBuffer;
    private ExecutorService processingExecutor;
    
    // 狀態管理
    private AtomicBoolean isActive = new AtomicBoolean(false);
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private long lastVoiceActivity = 0;
    private StringBuilder currentSentence = new StringBuilder();
    private Timer sentenceTimer;
    private TranscriptionCallback callback;
    
    // 統計信息
    private int processedChunks = 0;
    private long totalProcessingTime = 0;
    
    /**
     * 構造函數
     * @param whisperEngine Whisper引擎實例
     * @param repository 轉錄數據庫
     */
    public RealTimeTranscriber(WhisperEngine whisperEngine, TranscriptionRepository repository) {
        this.whisperEngine = whisperEngine;
        this.repository = repository;
        this.audioBuffer = new AudioBuffer(BUFFER_SIZE_MS);
        this.processingExecutor = Executors.newSingleThreadExecutor();
        
        Log.i(TAG, "RealTimeTranscriber initialized");
    }
    
    /**
     * 開始實時轉錄
     * @param audioStream 音頻流
     * @param callback 轉錄回調
     */
    public void startRealTimeTranscription(AudioStream audioStream, TranscriptionCallback callback) {
        if (whisperEngine == null) {
            Log.e(TAG, "WhisperEngine is null");
            if (callback != null) {
                callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
            }
            return;
        }
        
        if (isActive.get()) {
            Log.w(TAG, "Real-time transcription already active");
            return;
        }
        
        this.callback = callback;
        isActive.set(true);
        audioBuffer.clear();
        currentSentence.setLength(0);
        processedChunks = 0;
        totalProcessingTime = 0;
        
        Log.i(TAG, "Starting real-time transcription");
        
        // 設置音頻流監聽器
        audioStream.setListener(new AudioStreamListener() {
            @Override
            public void onAudioDataAvailable(byte[] audioData) {
                if (isActive.get() && audioData != null && audioData.length > 0) {
                    processAudioData(audioData);
                }
            }
            
            @Override
            public void onRecordingStarted() {
                Log.i(TAG, "Audio recording started for real-time transcription");
                if (callback != null) {
                    callback.onTranscriptionStarted();
                }
                startSentenceTimer();
            }
            
            @Override
            public void onRecordingStopped() {
                Log.i(TAG, "Audio recording stopped");
                stopRealTimeTranscription();
            }
            
            @Override
            public void onRecordingError(String errorMessage) {
                Log.e(TAG, "Audio recording error: " + errorMessage);
                if (callback != null) {
                    callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                }
                stopRealTimeTranscription();
            }
            
            @Override
            public void onVolumeChanged(float volume) {
                // 可以用於調整VAD閾值
                if (volume > 0.8f) {
                    // 高音量環境，提高閾值
                } else if (volume < 0.2f) {
                    // 低音量環境，降低閾值
                }
            }
        });
        
        // 啟動音頻流
        if (!audioStream.start()) {
            Log.e(TAG, "Failed to start audio stream");
            isActive.set(false);
            if (callback != null) {
                callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
            }
        }
    }
    
    /**
     * 停止實時轉錄
     */
    public void stopRealTimeTranscription() {
        if (!isActive.get()) {
            return;
        }
        
        Log.i(TAG, "Stopping real-time transcription");
        
        isActive.set(false);
        
        // 停止句子計時器
        stopSentenceTimer();
        
        // 處理剩餘的音頻數據
        processRemainingAudio();
        
        // 清理資源
        audioBuffer.clear();
        currentSentence.setLength(0);
        
        Log.i(TAG, "Real-time transcription stopped. Processed " + processedChunks + 
                  " chunks, average processing time: " + 
                  (processedChunks > 0 ? totalProcessingTime / processedChunks : 0) + "ms");
    }
    
    /**
     * 處理音頻數據
     */
    private void processAudioData(byte[] audioData) {
        // 添加到緩衝區
        audioBuffer.addAudioData(audioData);
        
        // 檢測語音活動
        boolean hasVoice = AudioProcessor.detectVoiceActivity(audioData, VAD_THRESHOLD);
        
        if (hasVoice) {
            lastVoiceActivity = System.currentTimeMillis();
            
            // 如果緩衝區有足夠的數據且沒有正在處理，開始處理
            if (audioBuffer.getCurrentSize() >= getMinBufferSize() && !isProcessing.get()) {
                scheduleProcessing();
            }
        }
    }
    
    /**
     * 安排音頻處理
     */
    private void scheduleProcessing() {
        if (!isProcessing.compareAndSet(false, true)) {
            return; // 已經在處理中
        }
        
        processingExecutor.execute(() -> {
            try {
                processBufferedAudio();
            } finally {
                isProcessing.set(false);
            }
        });
    }
    
    /**
     * 處理緩衝的音頻數據
     */
    private void processBufferedAudio() {
        if (!isActive.get()) {
            return;
        }
        
        // 獲取部分緩衝音頻（保留一些數據以避免截斷）
        byte[] bufferedAudio = audioBuffer.getPartialAudio(0.7f);
        
        if (bufferedAudio == null || bufferedAudio.length < getMinAudioBytes()) {
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 轉換音頻格式
            AudioData audioData = AudioProcessor.convertToWhisperFormat(bufferedAudio, 16000);
            
            if (audioData.isEmpty()) {
                Log.w(TAG, "Converted audio data is empty");
                return;
            }
            
            // 執行轉錄
            whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                @Override
                public void onTranscriptionStarted() {
                    // 實時轉錄不需要處理開始事件
                }
                
                @Override
                public void onPartialResult(String partialText) {
                    // Whisper通常不提供部分結果
                }
                
                @Override
                public void onTranscriptionCompleted(TranscriptionResult result) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    totalProcessingTime += processingTime;
                    processedChunks++;
                    
                    String text = result.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        processTranscriptionResult(text.trim(), result.getConfidence());
                    }
                    
                    Log.d(TAG, "Processed chunk in " + processingTime + "ms: " + text);
                }
                
                @Override
                public void onTranscriptionError(TranscriptionError error) {
                    Log.w(TAG, "Transcription error for audio chunk: " + error.getMessage());
                    // 對於實時轉錄，單個塊的錯誤不應該停止整個過程
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing buffered audio", e);
        }
    }
    
    /**
     * 處理轉錄結果
     */
    private void processTranscriptionResult(String text, float confidence) {
        if (!isActive.get()) {
            return;
        }
        
        // 檢查是否是句子結束
        boolean isSentenceEnd = text.matches(".*[。！？.!?]\\s*$");
        
        if (isSentenceEnd) {
            // 完整句子
            currentSentence.append(text);
            String completeSentence = currentSentence.toString().trim();
            
            if (!completeSentence.isEmpty()) {
                // 創建轉錄結果
                TranscriptionResult result = new TranscriptionResult();
                result.setText(completeSentence);
                result.setComplete(true);
                result.setConfidence(confidence);
                result.setProcessingTime(System.currentTimeMillis() - lastVoiceActivity);
                
                // 通知回調
                if (callback != null) {
                    callback.onTranscriptionCompleted(result);
                }
                
                // 保存到數據庫
                saveRealTimeResult(result);
                
                Log.i(TAG, "Complete sentence: " + completeSentence);
            }
            
            // 清空當前句子緩衝
            currentSentence.setLength(0);
            
        } else {
            // 部分結果或句子片段
            currentSentence.append(text);
            
            // 發送部分結果
            if (callback != null) {
                callback.onPartialResult(currentSentence.toString());
            }
        }
    }
    
    /**
     * 啟動句子計時器
     */
    private void startSentenceTimer() {
        stopSentenceTimer(); // 確保沒有重複的計時器
        
        sentenceTimer = new Timer();
        sentenceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkSentenceCompletion();
            }
        }, SENTENCE_TIMEOUT_MS, SENTENCE_TIMEOUT_MS);
    }
    
    /**
     * 停止句子計時器
     */
    private void stopSentenceTimer() {
        if (sentenceTimer != null) {
            sentenceTimer.cancel();
            sentenceTimer = null;
        }
    }
    
    /**
     * 檢查句子完成
     */
    private void checkSentenceCompletion() {
        if (!isActive.get()) {
            return;
        }
        
        long timeSinceLastVoice = System.currentTimeMillis() - lastVoiceActivity;
        
        // 如果超時且有內容，認為句子結束
        if (timeSinceLastVoice > SENTENCE_TIMEOUT_MS && currentSentence.length() > 0) {
            String completeSentence = currentSentence.toString().trim();
            
            if (!completeSentence.isEmpty()) {
                // 創建轉錄結果
                TranscriptionResult result = new TranscriptionResult();
                result.setText(completeSentence);
                result.setComplete(true);
                result.setConfidence(0.75f); // 稍低的置信度，因為可能不完整
                result.setProcessingTime(timeSinceLastVoice);
                
                // 通知回調
                if (callback != null) {
                    callback.onTranscriptionCompleted(result);
                }
                
                // 保存到數據庫
                saveRealTimeResult(result);
                
                Log.i(TAG, "Timeout sentence: " + completeSentence);
            }
            
            // 清空緩衝
            currentSentence.setLength(0);
        }
    }
    
    /**
     * 處理剩餘音頻
     */
    private void processRemainingAudio() {
        if (currentSentence.length() > 0) {
            String remainingText = currentSentence.toString().trim();
            
            if (!remainingText.isEmpty()) {
                TranscriptionResult result = new TranscriptionResult();
                result.setText(remainingText);
                result.setComplete(true);
                result.setConfidence(0.70f);
                result.setProcessingTime(0);
                
                if (callback != null) {
                    callback.onTranscriptionCompleted(result);
                }
                
                saveRealTimeResult(result);
                
                Log.i(TAG, "Remaining text: " + remainingText);
            }
        }
    }
    
    /**
     * 保存實時轉錄結果
     */
    private void saveRealTimeResult(TranscriptionResult result) {
        if (repository == null || result == null || result.getText() == null) {
            return;
        }
        
        TranscriptionRecord record = new TranscriptionRecord();
        record.setOriginalText(result.getText());
        record.setEditedText(result.getText());
        record.setTimestamp(System.currentTimeMillis());
        record.setConfidence(result.getConfidence());
        record.setRealTime(true);
        record.setDuration((int) result.getProcessingTime());
        
        repository.saveTranscription(record, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                Log.d(TAG, "Real-time result saved with ID: " + id);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Failed to save real-time result", error);
            }
        });
    }
    
    /**
     * 獲取最小緩衝區大小
     */
    private int getMinBufferSize() {
        // 16kHz * 2 bytes * MIN_AUDIO_LENGTH_MS / 1000
        return 16000 * 2 * MIN_AUDIO_LENGTH_MS / 1000;
    }
    
    /**
     * 獲取最小音頻字節數
     */
    private int getMinAudioBytes() {
        return getMinBufferSize();
    }
    
    /**
     * 檢查是否活躍
     */
    public boolean isActive() {
        return isActive.get();
    }
    
    /**
     * 獲取統計信息
     */
    public String getStatistics() {
        return String.format("RealTimeTranscriber - Active: %s, Processed: %d chunks, Avg time: %dms",
                           isActive.get(), processedChunks, 
                           processedChunks > 0 ? totalProcessingTime / processedChunks : 0);
    }
    
    /**
     * 釋放資源
     */
    public void release() {
        Log.i(TAG, "Releasing RealTimeTranscriber resources");
        
        stopRealTimeTranscription();
        
        if (processingExecutor != null && !processingExecutor.isShutdown()) {
            processingExecutor.shutdown();
        }
        
        if (audioBuffer != null) {
            audioBuffer.clear();
        }
        
        Log.i(TAG, "RealTimeTranscriber resources released");
    }
}