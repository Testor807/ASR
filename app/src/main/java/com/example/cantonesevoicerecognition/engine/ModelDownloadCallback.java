package com.example.cantonesevoicerecognition.engine;

/**
 * 模型下載回調接口
 * 用於監聽模型文件下載過程中的各種事件
 */
public interface ModelDownloadCallback {
    
    /**
     * 下載開始時調用
     */
    void onDownloadStarted();
    
    /**
     * 下載進度更新時調用
     * @param progress 下載進度 (0.0 - 1.0)
     * @param downloadedBytes 已下載字節數
     * @param totalBytes 總字節數
     */
    void onDownloadProgress(float progress, long downloadedBytes, long totalBytes);
    
    /**
     * 下載完成時調用
     * @param modelPath 下載完成的模型文件路徑
     */
    void onDownloadCompleted(String modelPath);
    
    /**
     * 下載出錯時調用
     * @param error 錯誤信息
     */
    void onDownloadError(Exception error);
    
    /**
     * 下載被取消時調用
     */
    default void onDownloadCancelled() {
        // 默認空實現
    }
    
    /**
     * 模型驗證開始時調用
     */
    default void onValidationStarted() {
        // 默認空實現
    }
    
    /**
     * 模型驗證完成時調用
     * @param isValid 模型是否有效
     */
    default void onValidationCompleted(boolean isValid) {
        // 默認空實現
    }
}