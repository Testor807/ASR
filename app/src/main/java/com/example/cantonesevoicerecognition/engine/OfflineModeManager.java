package com.example.cantonesevoicerecognition.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 離線模式管理器
 * 負責管理模型文件的下載、存儲、驗證和版本控制
 */
public class OfflineModeManager {
    private static final String TAG = "OfflineModeManager";
    
    // 配置常量
    private static final String MODEL_FILENAME = "whisper_cantonese.onnx";
    private static final String MODEL_URL = "https://huggingface.co/openai/whisper-base/resolve/main/model.onnx";
    private static final String BACKUP_MODEL_URL = "https://github.com/openai/whisper/releases/download/v20230314/base.pt";
    private static final String PREFS_NAME = "offline_mode_prefs";
    private static final String KEY_OFFLINE_ENABLED = "offline_enabled";
    private static final String KEY_MODEL_VERSION = "model_version";
    private static final String KEY_MODEL_HASH = "model_hash";
    private static final String KEY_LAST_UPDATE_CHECK = "last_update_check";
    private static final String KEY_DOWNLOAD_PROGRESS = "download_progress";
    
    // 文件大小限制
    private static final long MIN_MODEL_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_MODEL_SIZE = 500 * 1024 * 1024; // 500MB
    private static final int DOWNLOAD_TIMEOUT = 30000; // 30秒
    private static final int READ_TIMEOUT = 60000; // 60秒
    
    private Context context;
    private SharedPreferences preferences;
    private boolean isOfflineModeEnabled = false;
    private ExecutorService downloadExecutor;
    private Future<?> currentDownloadTask;
    private AtomicBoolean isDownloading = new AtomicBoolean(false);
    
    /**
     * 構造函數
     * @param context 應用上下文
     */
    public OfflineModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.isOfflineModeEnabled = preferences.getBoolean(KEY_OFFLINE_ENABLED, false);
        this.downloadExecutor = Executors.newSingleThreadExecutor();
        
        Log.i(TAG, "OfflineModeManager initialized, offline mode: " + isOfflineModeEnabled);
    }
    
    /**
     * 檢查離線模式是否可用
     * @return 是否可用
     */
    public boolean isOfflineModeAvailable() {
        File modelFile = new File(getModelPath());
        boolean exists = modelFile.exists();
        boolean validSize = exists && modelFile.length() >= MIN_MODEL_SIZE;
        boolean validHash = exists && validateModelHash(modelFile);
        
        Log.d(TAG, "Offline mode availability - exists: " + exists + 
                  ", validSize: " + validSize + ", validHash: " + validHash);
        
        return exists && validSize && validHash;
    }
    
    /**
     * 啟用離線模式
     */
    public void enableOfflineMode() {
        isOfflineModeEnabled = true;
        preferences.edit().putBoolean(KEY_OFFLINE_ENABLED, true).apply();
        Log.i(TAG, "Offline mode enabled");
    }
    
    /**
     * 禁用離線模式
     */
    public void disableOfflineMode() {
        isOfflineModeEnabled = false;
        preferences.edit().putBoolean(KEY_OFFLINE_ENABLED, false).apply();
        Log.i(TAG, "Offline mode disabled");
    }
    
    /**
     * 檢查離線模式是否已啟用
     * @return 是否已啟用
     */
    public boolean isOfflineModeEnabled() {
        return isOfflineModeEnabled;
    }
    
    /**
     * 下載模型文件
     * @param callback 下載回調
     */
    public void downloadModel(ModelDownloadCallback callback) {
        if (callback == null) {
            Log.w(TAG, "Download callback is null");
            return;
        }
        
        // 檢查是否已經在下載
        if (!isDownloading.compareAndSet(false, true)) {
            Log.w(TAG, "Download already in progress");
            callback.onDownloadError(new IllegalStateException("下載已在進行中"));
            return;
        }
        
        // 如果模型已存在且有效，直接返回
        if (isOfflineModeAvailable()) {
            Log.i(TAG, "Model already exists and is valid");
            callback.onDownloadCompleted(getModelPath());
            isDownloading.set(false);
            return;
        }
        
        Log.i(TAG, "Starting model download");
        
        currentDownloadTask = downloadExecutor.submit(() -> {
            try {
                callback.onDownloadStarted();
                
                // 嘗試主要URL
                boolean success = downloadFromUrl(MODEL_URL, callback);
                
                // 如果主要URL失敗，嘗試備用URL
                if (!success) {
                    Log.w(TAG, "Primary download failed, trying backup URL");
                    success = downloadFromUrl(BACKUP_MODEL_URL, callback);
                }
                
                if (success) {
                    // 驗證下載的模型文件
                    callback.onValidationStarted();
                    
                    String modelPath = getModelPath();
                    if (validateModelFile(modelPath)) {
                        // 計算並保存文件哈希
                        String hash = calculateFileHash(new File(modelPath));
                        preferences.edit()
                                .putString(KEY_MODEL_HASH, hash)
                                .putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis())
                                .putFloat(KEY_DOWNLOAD_PROGRESS, 1.0f)
                                .apply();
                        
                        callback.onValidationCompleted(true);
                        callback.onDownloadCompleted(modelPath);
                        Log.i(TAG, "Model download and validation completed successfully");
                    } else {
                        callback.onValidationCompleted(false);
                        callback.onDownloadError(new Exception("模型文件驗證失敗"));
                    }
                } else {
                    callback.onDownloadError(new Exception("所有下載源都失敗了"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Download failed with exception", e);
                callback.onDownloadError(e);
            } finally {
                isDownloading.set(false);
            }
        });
    }
    
    /**
     * 從指定URL下載模型
     */
    private boolean downloadFromUrl(String urlString, ModelDownloadCallback callback) {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "CantoneseVoiceRecognition/1.0");
            
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: " + responseCode + " for URL: " + urlString);
                return false;
            }
            
            long fileLength = connection.getContentLengthLong();
            if (fileLength > MAX_MODEL_SIZE) {
                Log.e(TAG, "Model file too large: " + fileLength + " bytes");
                return false;
            }
            
            Log.i(TAG, "Downloading model from: " + urlString + ", size: " + fileLength + " bytes");
            
            // 創建目標目錄
            File modelFile = new File(getModelPath());
            File parentDir = modelFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            input = new BufferedInputStream(connection.getInputStream());
            output = new FileOutputStream(modelFile);
            
            byte[] buffer = new byte[8192];
            long totalDownloaded = 0;
            int bytesRead;
            long lastProgressUpdate = 0;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalDownloaded += bytesRead;
                
                // 更新進度（每100KB更新一次）
                if (totalDownloaded - lastProgressUpdate >= 102400 || totalDownloaded == fileLength) {
                    float progress = fileLength > 0 ? (float) totalDownloaded / fileLength : 0;
                    callback.onDownloadProgress(progress, totalDownloaded, fileLength);
                    
                    // 保存進度到SharedPreferences
                    preferences.edit().putFloat(KEY_DOWNLOAD_PROGRESS, progress).apply();
                    
                    lastProgressUpdate = totalDownloaded;
                }
                
                // 檢查是否被取消
                if (Thread.currentThread().isInterrupted()) {
                    Log.i(TAG, "Download cancelled");
                    callback.onDownloadCancelled();
                    return false;
                }
            }
            
            output.flush();
            Log.i(TAG, "Download completed: " + totalDownloaded + " bytes");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading from URL: " + urlString, e);
            return false;
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * 取消當前下載
     */
    public void cancelDownload() {
        if (currentDownloadTask != null && !currentDownloadTask.isDone()) {
            currentDownloadTask.cancel(true);
            Log.i(TAG, "Download cancelled");
        }
        isDownloading.set(false);
    }
    
    /**
     * 檢查是否正在下載
     * @return 是否正在下載
     */
    public boolean isDownloading() {
        return isDownloading.get();
    }
    
    /**
     * 獲取模型文件路徑
     * @return 模型文件路徑
     */
    public String getModelPath() {
        File modelsDir = new File(context.getFilesDir(), "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        return new File(modelsDir, MODEL_FILENAME).getAbsolutePath();
    }
    
    /**
     * 驗證模型文件
     */
    private boolean validateModelFile(String modelPath) {
        File modelFile = new File(modelPath);
        
        // 檢查文件是否存在
        if (!modelFile.exists()) {
            Log.w(TAG, "Model file does not exist: " + modelPath);
            return false;
        }
        
        // 檢查文件大小
        long fileSize = modelFile.length();
        if (fileSize < MIN_MODEL_SIZE) {
            Log.w(TAG, "Model file too small: " + fileSize + " bytes");
            return false;
        }
        
        if (fileSize > MAX_MODEL_SIZE) {
            Log.w(TAG, "Model file too large: " + fileSize + " bytes");
            return false;
        }
        
        // 檢查文件是否可讀
        if (!modelFile.canRead()) {
            Log.w(TAG, "Cannot read model file: " + modelPath);
            return false;
        }
        
        // 簡單的文件頭檢查（ONNX文件應該以特定字節開始）
        try (FileInputStream fis = new FileInputStream(modelFile)) {
            byte[] header = new byte[8];
            int bytesRead = fis.read(header);
            
            if (bytesRead >= 4) {
                // ONNX文件通常以特定的魔術數字開始
                // 這裡做簡單檢查，確保不是空文件或損壞文件
                boolean hasContent = false;
                for (byte b : header) {
                    if (b != 0) {
                        hasContent = true;
                        break;
                    }
                }
                
                if (!hasContent) {
                    Log.w(TAG, "Model file appears to be empty or corrupted");
                    return false;
                }
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading model file header", e);
            return false;
        }
        
        Log.i(TAG, "Model file validation passed: " + modelPath);
        return true;
    }
    
    /**
     * 驗證模型文件哈希
     */
    private boolean validateModelHash(File modelFile) {
        String savedHash = preferences.getString(KEY_MODEL_HASH, null);
        if (savedHash == null) {
            Log.d(TAG, "No saved hash found, skipping hash validation");
            return true; // 如果沒有保存的哈希，跳過驗證
        }
        
        String currentHash = calculateFileHash(modelFile);
        boolean isValid = savedHash.equals(currentHash);
        
        Log.d(TAG, "Hash validation - saved: " + savedHash + ", current: " + currentHash + ", valid: " + isValid);
        return isValid;
    }
    
    /**
     * 計算文件哈希值
     */
    private String calculateFileHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating file hash", e);
            return null;
        }
    }
    
    /**
     * 獲取模型下載進度
     * @return 下載進度 (0.0 - 1.0)
     */
    public float getModelDownloadProgress() {
        return preferences.getFloat(KEY_DOWNLOAD_PROGRESS, 0.0f);
    }
    
    /**
     * 獲取模型文件信息
     * @return 模型文件信息
     */
    public ModelInfo getModelInfo() {
        String modelPath = getModelPath();
        File modelFile = new File(modelPath);
        
        ModelInfo info = new ModelInfo();
        info.path = modelPath;
        info.exists = modelFile.exists();
        info.size = modelFile.exists() ? modelFile.length() : 0;
        info.lastModified = modelFile.exists() ? modelFile.lastModified() : 0;
        info.version = preferences.getString(KEY_MODEL_VERSION, "unknown");
        info.hash = preferences.getString(KEY_MODEL_HASH, null);
        info.isValid = isOfflineModeAvailable();
        
        return info;
    }
    
    /**
     * 刪除模型文件
     * @return 是否成功刪除
     */
    public boolean deleteModel() {
        File modelFile = new File(getModelPath());
        boolean deleted = false;
        
        if (modelFile.exists()) {
            deleted = modelFile.delete();
            if (deleted) {
                // 清除相關的偏好設置
                preferences.edit()
                        .remove(KEY_MODEL_HASH)
                        .remove(KEY_MODEL_VERSION)
                        .remove(KEY_DOWNLOAD_PROGRESS)
                        .apply();
                
                Log.i(TAG, "Model file deleted successfully");
            } else {
                Log.e(TAG, "Failed to delete model file");
            }
        } else {
            Log.w(TAG, "Model file does not exist, nothing to delete");
            deleted = true; // 文件不存在也算刪除成功
        }
        
        return deleted;
    }
    
    /**
     * 檢查模型更新
     * @param callback 檢查結果回調
     */
    public void checkForModelUpdate(ModelUpdateCallback callback) {
        // 簡單實現：檢查上次更新時間
        long lastCheck = preferences.getLong(KEY_LAST_UPDATE_CHECK, 0);
        long now = System.currentTimeMillis();
        long daysSinceLastCheck = (now - lastCheck) / (24 * 60 * 60 * 1000);
        
        if (daysSinceLastCheck >= 7) { // 一週檢查一次
            // 這裡可以實現更複雜的版本檢查邏輯
            if (callback != null) {
                callback.onUpdateCheckCompleted(false, "暫無可用更新");
            }
        } else {
            if (callback != null) {
                callback.onUpdateCheckCompleted(false, "最近已檢查過更新");
            }
        }
    }
    
    /**
     * 釋放資源
     */
    public void release() {
        Log.i(TAG, "Releasing OfflineModeManager resources");
        
        // 取消當前下載
        cancelDownload();
        
        // 關閉線程池
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            downloadExecutor.shutdown();
            try {
                if (!downloadExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    downloadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                downloadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        Log.i(TAG, "OfflineModeManager resources released");
    }
    
    /**
     * 模型信息類
     */
    public static class ModelInfo {
        public String path;
        public boolean exists;
        public long size;
        public long lastModified;
        public String version;
        public String hash;
        public boolean isValid;
        
        @Override
        public String toString() {
            return String.format("ModelInfo{path='%s', exists=%s, size=%d, version='%s', isValid=%s}",
                               path, exists, size, version, isValid);
        }
    }
    
    /**
     * 模型更新檢查回調接口
     */
    public interface ModelUpdateCallback {
        void onUpdateCheckCompleted(boolean hasUpdate, String message);
        void onUpdateCheckError(Exception error);
    }
}