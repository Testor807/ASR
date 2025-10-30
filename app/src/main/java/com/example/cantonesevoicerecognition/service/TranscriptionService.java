package com.example.cantonesevoicerecognition.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.cantonesevoicerecognition.R;
import com.example.cantonesevoicerecognition.audio.AudioRecorderManager;
import com.example.cantonesevoicerecognition.audio.AudioStream;
import com.example.cantonesevoicerecognition.data.model.AudioData;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;
import com.example.cantonesevoicerecognition.data.repository.RepositoryCallback;
import com.example.cantonesevoicerecognition.data.repository.TranscriptionRepository;
import com.example.cantonesevoicerecognition.engine.OfflineModeHelper;
import com.example.cantonesevoicerecognition.engine.TranscriptionCallback;
import com.example.cantonesevoicerecognition.engine.TranscriptionError;
import com.example.cantonesevoicerecognition.engine.WhisperEngine;
import com.example.cantonesevoicerecognition.engine.WhisperEngineFactory;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 轉錄後台服務
 * 處理長時間的轉錄任務和實時轉錄功能
 */
public class TranscriptionService extends Service {
    private static final String TAG = "TranscriptionService";
    
    // 通知相關常量
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "transcription_channel";
    private static final String CHANNEL_NAME = "轉錄服務";
    
    // 服務動作常量
    public static final String ACTION_START_REAL_TIME = "START_REAL_TIME";
    public static final String ACTION_STOP_REAL_TIME = "STOP_REAL_TIME";
    public static final String ACTION_TRANSCRIBE_FILE = "TRANSCRIBE_FILE";
    public static final String EXTRA_FILE_PATH = "file_path";
    
    // 服務組件
    private final IBinder binder = new TranscriptionBinder();
    private WhisperEngine whisperEngine;
    private AudioRecorderManager audioRecorderManager;
    private AudioStream audioStream;
    private TranscriptionRepository repository;
    private RealTimeTranscriber realTimeTranscriber;
    private ExecutorService executorService;
    
    // 狀態管理
    private AtomicBoolean isRealTimeActive = new AtomicBoolean(false);
    private AtomicBoolean isServiceInitialized = new AtomicBoolean(false);
    private TranscriptionCallback externalCallback;
    
    /**
     * 服務綁定器
     */
    public class TranscriptionBinder extends Binder {
        public TranscriptionService getService() {
            return TranscriptionService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TranscriptionService created");
        
        // 初始化服務組件
        initializeService();
    }
    
    /**
     * 初始化服務組件
     */
    private void initializeService() {
        try {
            // 創建通知渠道
            createNotificationChannel();
            
            // 初始化線程池
            executorService = Executors.newCachedThreadPool();
            
            // 初始化數據庫
            repository = new TranscriptionRepository(getApplication());
            
            // 初始化音頻組件
            audioRecorderManager = new AudioRecorderManager(this);
            audioStream = new AudioStream(audioRecorderManager);
            
            // 初始化Whisper引擎（優先使用離線模式）
            whisperEngine = WhisperEngineFactory.createAutoEngine(this, null);
            
            if (whisperEngine == null) {
                Log.w(TAG, "Failed to initialize WhisperEngine, service will have limited functionality");
            }
            
            // 初始化實時轉錄器
            realTimeTranscriber = new RealTimeTranscriber(whisperEngine, repository);
            
            isServiceInitialized.set(true);
            Log.i(TAG, "TranscriptionService initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TranscriptionService", e);
            isServiceInitialized.set(false);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceInitialized.get()) {
            Log.e(TAG, "Service not properly initialized");
            return START_NOT_STICKY;
        }
        
        if (intent != null) {
            String action = intent.getAction();
            Log.i(TAG, "Received action: " + action);
            
            switch (action != null ? action : "") {
                case ACTION_START_REAL_TIME:
                    startRealTimeTranscription();
                    break;
                case ACTION_STOP_REAL_TIME:
                    stopRealTimeTranscription();
                    break;
                case ACTION_TRANSCRIBE_FILE:
                    String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                    transcribeAudioFile(filePath);
                    break;
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        }
        
        return START_STICKY; // 服務被殺死後自動重啟
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service bound");
        return binder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Service unbound");
        return super.onUnbind(intent);
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "TranscriptionService destroying");
        
        // 停止所有轉錄活動
        stopRealTimeTranscription();
        
        // 釋放資源
        releaseResources();
        
        super.onDestroy();
        Log.i(TAG, "TranscriptionService destroyed");
    }
    
    /**
     * 開始實時轉錄
     */
    public void startRealTimeTranscription() {
        if (!isServiceInitialized.get()) {
            Log.e(TAG, "Service not initialized");
            notifyError("服務未初始化");
            return;
        }
        
        if (whisperEngine == null) {
            Log.e(TAG, "WhisperEngine not available");
            notifyError("語音識別引擎不可用");
            return;
        }
        
        if (isRealTimeActive.get()) {
            Log.w(TAG, "Real-time transcription already active");
            return;
        }
        
        Log.i(TAG, "Starting real-time transcription");
        
        // 啟動前台服務
        startForeground(NOTIFICATION_ID, createNotification("實時轉錄進行中..."));
        
        // 開始實時轉錄
        realTimeTranscriber.startRealTimeTranscription(audioStream, new TranscriptionCallback() {
            @Override
            public void onTranscriptionStarted() {
                isRealTimeActive.set(true);
                Log.i(TAG, "Real-time transcription started");
                
                if (externalCallback != null) {
                    externalCallback.onTranscriptionStarted();
                }
                
                // 更新通知
                updateNotification("實時轉錄已開始");
            }
            
            @Override
            public void onPartialResult(String partialText) {
                Log.d(TAG, "Partial result: " + partialText);
                
                if (externalCallback != null) {
                    externalCallback.onPartialResult(partialText);
                }
                
                // 發送廣播
                sendTranscriptionBroadcast(partialText, false);
            }
            
            @Override
            public void onTranscriptionCompleted(TranscriptionResult result) {
                Log.i(TAG, "Real-time transcription completed: " + result.getText());
                
                // 保存轉錄結果
                saveTranscriptionResult(result, true);
                
                if (externalCallback != null) {
                    externalCallback.onTranscriptionCompleted(result);
                }
                
                // 發送廣播
                sendTranscriptionBroadcast(result.getText(), true);
            }
            
            @Override
            public void onTranscriptionError(TranscriptionError error) {
                Log.e(TAG, "Real-time transcription error: " + error.getMessage());
                
                if (externalCallback != null) {
                    externalCallback.onTranscriptionError(error);
                }
                
                notifyError("轉錄錯誤: " + error.getMessage());
                stopRealTimeTranscription();
            }
        });
    }
    
    /**
     * 停止實時轉錄
     */
    public void stopRealTimeTranscription() {
        if (!isRealTimeActive.get()) {
            Log.w(TAG, "Real-time transcription not active");
            return;
        }
        
        Log.i(TAG, "Stopping real-time transcription");
        
        isRealTimeActive.set(false);
        
        // 停止實時轉錄器
        if (realTimeTranscriber != null) {
            realTimeTranscriber.stopRealTimeTranscription();
        }
        
        // 停止前台服務
        stopForeground(true);
        
        Log.i(TAG, "Real-time transcription stopped");
    }
    
    /**
     * 轉錄音頻文件
     */
    public void transcribeAudioFile(String filePath) {
        if (!isServiceInitialized.get() || whisperEngine == null) {
            Log.e(TAG, "Service not ready for file transcription");
            notifyError("服務未就緒");
            return;
        }
        
        if (filePath == null || filePath.trim().isEmpty()) {
            Log.e(TAG, "Invalid file path");
            notifyError("無效的文件路徑");
            return;
        }
        
        Log.i(TAG, "Starting file transcription: " + filePath);
        
        // 啟動前台服務
        startForeground(NOTIFICATION_ID, createNotification("正在轉錄音頻文件..."));
        
        executorService.execute(() -> {
            try {
                // 讀取音頻文件
                AudioData audioData = AudioFileReader.readAudioFile(filePath);
                
                if (audioData == null || audioData.isEmpty()) {
                    notifyError("無法讀取音頻文件");
                    stopForeground(true);
                    return;
                }
                
                // 執行轉錄
                whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                    @Override
                    public void onTranscriptionStarted() {
                        updateNotification("正在分析音頻文件...");
                    }
                    
                    @Override
                    public void onPartialResult(String partialText) {
                        // 文件轉錄通常不需要部分結果
                    }
                    
                    @Override
                    public void onTranscriptionCompleted(TranscriptionResult result) {
                        Log.i(TAG, "File transcription completed: " + result.getText());
                        
                        // 保存轉錄結果
                        saveTranscriptionResult(result, false, filePath);
                        
                        if (externalCallback != null) {
                            externalCallback.onTranscriptionCompleted(result);
                        }
                        
                        // 發送廣播
                        sendTranscriptionBroadcast(result.getText(), true);
                        
                        // 停止前台服務
                        stopForeground(true);
                        
                        updateNotification("文件轉錄完成");
                    }
                    
                    @Override
                    public void onTranscriptionError(TranscriptionError error) {
                        Log.e(TAG, "File transcription error: " + error.getMessage());
                        
                        if (externalCallback != null) {
                            externalCallback.onTranscriptionError(error);
                        }
                        
                        notifyError("文件轉錄失敗: " + error.getMessage());
                        stopForeground(true);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error during file transcription", e);
                notifyError("轉錄過程中發生錯誤: " + e.getMessage());
                stopForeground(true);
            }
        });
    }
    
    /**
     * 保存轉錄結果
     */
    private void saveTranscriptionResult(TranscriptionResult result, boolean isRealTime) {
        saveTranscriptionResult(result, isRealTime, null);
    }
    
    private void saveTranscriptionResult(TranscriptionResult result, boolean isRealTime, String audioFilePath) {
        if (result == null || result.getText() == null || result.getText().trim().isEmpty()) {
            Log.w(TAG, "Empty transcription result, not saving");
            return;
        }
        
        TranscriptionRecord record = new TranscriptionRecord();
        record.setOriginalText(result.getText());
        record.setEditedText(result.getText());
        record.setTimestamp(System.currentTimeMillis());
        record.setConfidence(result.getConfidence());
        record.setRealTime(isRealTime);
        record.setDuration((int) result.getProcessingTime());
        record.setAudioFilePath(audioFilePath);
        
        repository.saveTranscription(record, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                Log.i(TAG, "Transcription saved with ID: " + id);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Failed to save transcription", error);
            }
        });
    }
    
    /**
     * 發送轉錄廣播
     */
    private void sendTranscriptionBroadcast(String text, boolean isComplete) {
        Intent intent = new Intent("com.example.cantonesevoicerecognition.TRANSCRIPTION_RESULT");
        intent.putExtra("text", text);
        intent.putExtra("isComplete", isComplete);
        intent.putExtra("timestamp", System.currentTimeMillis());
        sendBroadcast(intent);
    }
    
    /**
     * 創建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("語音轉錄服務通知");
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 創建通知
     */
    private Notification createNotification(String content) {
        Intent intent = new Intent(this, getMainActivityClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("粵語語音識別")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }
    
    /**
     * 更新通知
     */
    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }
    
    /**
     * 通知錯誤
     */
    private void notifyError(String errorMessage) {
        Log.e(TAG, "Service error: " + errorMessage);
        
        // 發送錯誤廣播
        Intent intent = new Intent("com.example.cantonesevoicerecognition.TRANSCRIPTION_ERROR");
        intent.putExtra("error", errorMessage);
        intent.putExtra("timestamp", System.currentTimeMillis());
        sendBroadcast(intent);
    }
    
    /**
     * 獲取主Activity類（需要根據實際情況調整）
     */
    private Class<?> getMainActivityClass() {
        try {
            return Class.forName("com.example.cantonesevoicerecognition.MainActivity");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "MainActivity not found, using default");
            return null;
        }
    }
    
    /**
     * 釋放資源
     */
    private void releaseResources() {
        Log.i(TAG, "Releasing service resources");
        
        // 停止實時轉錄
        if (realTimeTranscriber != null) {
            realTimeTranscriber.release();
        }
        
        // 釋放音頻組件
        if (audioStream != null) {
            audioStream.release();
        }
        
        if (audioRecorderManager != null) {
            audioRecorderManager.release();
        }
        
        // 釋放Whisper引擎
        if (whisperEngine != null) {
            whisperEngine.releaseModel();
        }
        
        // 關閉線程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.i(TAG, "Service resources released");
    }
    
    // 公共接口方法
    
    /**
     * 設置外部轉錄回調
     */
    public void setTranscriptionCallback(TranscriptionCallback callback) {
        this.externalCallback = callback;
    }
    
    /**
     * 檢查實時轉錄是否活躍
     */
    public boolean isRealTimeActive() {
        return isRealTimeActive.get();
    }
    
    /**
     * 檢查服務是否已初始化
     */
    public boolean isServiceInitialized() {
        return isServiceInitialized.get();
    }
    
    /**
     * 獲取服務狀態信息
     */
    public String getServiceStatus() {
        return String.format("TranscriptionService - Initialized: %s, RealTime: %s, Engine: %s",
                           isServiceInitialized.get(),
                           isRealTimeActive.get(),
                           whisperEngine != null ? "Available" : "Not Available");
    }
}