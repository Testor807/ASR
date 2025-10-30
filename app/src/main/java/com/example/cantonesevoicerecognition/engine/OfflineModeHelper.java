package com.example.cantonesevoicerecognition.engine;

import android.content.Context;
import android.util.Log;

/**
 * 離線模式輔助類
 * 提供便捷的離線模式操作方法
 */
public class OfflineModeHelper {
    private static final String TAG = "OfflineModeHelper";
    
    /**
     * 設置離線模式
     * @param context 應用上下文
     * @param enabled 是否啟用離線模式
     * @param callback 設置結果回調
     */
    public static void setupOfflineMode(Context context, boolean enabled, OfflineModeSetupCallback callback) {
        OfflineModeManager manager = new OfflineModeManager(context);
        
        if (enabled) {
            // 啟用離線模式
            if (manager.isOfflineModeAvailable()) {
                // 模型已存在，直接啟用
                manager.enableOfflineMode();
                if (callback != null) {
                    callback.onSetupCompleted(true, "離線模式已啟用");
                }
            } else {
                // 需要下載模型
                if (callback != null) {
                    callback.onSetupStarted("正在下載模型文件...");
                }
                
                manager.downloadModel(new ModelDownloadCallback() {
                    @Override
                    public void onDownloadStarted() {
                        if (callback != null) {
                            callback.onSetupProgress(0.0f, "開始下載模型...");
                        }
                    }
                    
                    @Override
                    public void onDownloadProgress(float progress, long downloadedBytes, long totalBytes) {
                        if (callback != null) {
                            String message = String.format("下載中... %.1f%% (%s/%s)", 
                                                          progress * 100,
                                                          formatBytes(downloadedBytes),
                                                          formatBytes(totalBytes));
                            callback.onSetupProgress(progress, message);
                        }
                    }
                    
                    @Override
                    public void onDownloadCompleted(String modelPath) {
                        manager.enableOfflineMode();
                        if (callback != null) {
                            callback.onSetupCompleted(true, "離線模式設置完成");
                        }
                    }
                    
                    @Override
                    public void onDownloadError(Exception error) {
                        Log.e(TAG, "Failed to setup offline mode", error);
                        if (callback != null) {
                            callback.onSetupError("設置離線模式失敗: " + error.getMessage());
                        }
                    }
                    
                    @Override
                    public void onValidationStarted() {
                        if (callback != null) {
                            callback.onSetupProgress(0.9f, "驗證模型文件...");
                        }
                    }
                    
                    @Override
                    public void onValidationCompleted(boolean isValid) {
                        if (callback != null) {
                            callback.onSetupProgress(1.0f, isValid ? "模型驗證成功" : "模型驗證失敗");
                        }
                    }
                });
            }
        } else {
            // 禁用離線模式
            manager.disableOfflineMode();
            if (callback != null) {
                callback.onSetupCompleted(false, "離線模式已禁用");
            }
        }
    }
    
    /**
     * 檢查離線模式狀態
     * @param context 應用上下文
     * @return 離線模式狀態信息
     */
    public static OfflineModeStatus checkOfflineModeStatus(Context context) {
        OfflineModeManager manager = new OfflineModeManager(context);
        OfflineModeManager.ModelInfo modelInfo = manager.getModelInfo();
        
        OfflineModeStatus status = new OfflineModeStatus();
        status.isEnabled = manager.isOfflineModeEnabled();
        status.isAvailable = manager.isOfflineModeAvailable();
        status.isDownloading = manager.isDownloading();
        status.downloadProgress = manager.getModelDownloadProgress();
        status.modelInfo = modelInfo;
        
        return status;
    }
    
    /**
     * 創建配置好的WhisperEngine（如果離線模式可用）
     * @param context 應用上下文
     * @return 配置好的WhisperEngine，如果離線模式不可用則返回null
     */
    public static WhisperEngine createOfflineEngine(Context context) {
        OfflineModeManager manager = new OfflineModeManager(context);
        
        if (!manager.isOfflineModeEnabled() || !manager.isOfflineModeAvailable()) {
            Log.w(TAG, "Offline mode not enabled or not available");
            return null;
        }
        
        String modelPath = manager.getModelPath();
        WhisperEngine engine = new WhisperEngine();
        
        if (engine.initializeModel(modelPath)) {
            Log.i(TAG, "Created offline WhisperEngine with model: " + modelPath);
            return engine;
        } else {
            Log.e(TAG, "Failed to initialize offline WhisperEngine");
            engine.releaseModel();
            return null;
        }
    }
    
    /**
     * 清理離線模式數據
     * @param context 應用上下文
     * @param callback 清理結果回調
     */
    public static void cleanupOfflineMode(Context context, OfflineModeCleanupCallback callback) {
        OfflineModeManager manager = new OfflineModeManager(context);
        
        try {
            // 禁用離線模式
            manager.disableOfflineMode();
            
            // 刪除模型文件
            boolean deleted = manager.deleteModel();
            
            if (callback != null) {
                if (deleted) {
                    callback.onCleanupCompleted("離線模式數據已清理");
                } else {
                    callback.onCleanupError("清理模型文件失敗");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during offline mode cleanup", e);
            if (callback != null) {
                callback.onCleanupError("清理過程中發生錯誤: " + e.getMessage());
            }
        } finally {
            manager.release();
        }
    }
    
    /**
     * 格式化字節數顯示
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 離線模式狀態類
     */
    public static class OfflineModeStatus {
        public boolean isEnabled;
        public boolean isAvailable;
        public boolean isDownloading;
        public float downloadProgress;
        public OfflineModeManager.ModelInfo modelInfo;
        
        @Override
        public String toString() {
            return String.format("OfflineModeStatus{enabled=%s, available=%s, downloading=%s, progress=%.2f, model=%s}",
                               isEnabled, isAvailable, isDownloading, downloadProgress, modelInfo);
        }
        
        /**
         * 獲取狀態描述
         */
        public String getStatusDescription() {
            if (isDownloading) {
                return String.format("正在下載模型 (%.1f%%)", downloadProgress * 100);
            } else if (isEnabled && isAvailable) {
                return "離線模式已啟用且可用";
            } else if (isEnabled && !isAvailable) {
                return "離線模式已啟用但模型不可用";
            } else {
                return "離線模式已禁用";
            }
        }
    }
    
    /**
     * 離線模式設置回調接口
     */
    public interface OfflineModeSetupCallback {
        void onSetupStarted(String message);
        void onSetupProgress(float progress, String message);
        void onSetupCompleted(boolean enabled, String message);
        void onSetupError(String errorMessage);
    }
    
    /**
     * 離線模式清理回調接口
     */
    public interface OfflineModeCleanupCallback {
        void onCleanupCompleted(String message);
        void onCleanupError(String errorMessage);
    }
}