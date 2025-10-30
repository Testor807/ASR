package com.example.cantonesevoicerecognition.utils;

import android.content.Context;

import com.example.cantonesevoicerecognition.engine.TranscriptionError;

/**
 * 错误处理使用示例
 * 展示如何正确使用ErrorHandler、LogManager和ErrorReporter
 */
public class ErrorHandlingExample {
    
    private static final String TAG = "ErrorHandlingExample";
    
    private final Context context;
    private final ErrorHandler errorHandler;
    private final LogManager logManager;
    private final ErrorReporter errorReporter;
    
    public ErrorHandlingExample(Context context) {
        this.context = context;
        this.errorHandler = ErrorHandler.getInstance(context);
        this.logManager = LogManager.getInstance(context);
        this.errorReporter = ErrorReporter.getInstance(context);
        
        // 配置日志管理器
        logManager.setMinLogLevel(LogManager.LogLevel.DEBUG);
        logManager.setFileLoggingEnabled(true);
        logManager.setConsoleLoggingEnabled(true);
    }
    
    /**
     * 示例1: 处理模型加载错误
     */
    public void handleModelLoadingError() {
        try {
            // 模拟模型加载失败
            throw new RuntimeException("模型文件损坏");
            
        } catch (Exception e) {
            // 使用ErrorHandler处理错误
            errorHandler.handleError(
                TranscriptionError.MODEL_CORRUPTED, 
                e, 
                new ErrorHandler.ErrorCallback() {
                    @Override
                    public void onRetryRequested() {
                        logManager.i(TAG, "用户请求重试模型加载");
                        // 重新尝试加载模型
                        retryModelLoading();
                    }
                    
                    @Override
                    public void onRestartRequired() {
                        logManager.w(TAG, "需要重启应用以解决模型问题");
                        // 执行应用重启逻辑
                    }
                    
                    @Override
                    public void onUserCancelled() {
                        logManager.d(TAG, "用户取消了错误处理");
                    }
                }
            );
        }
    }
    
    /**
     * 示例2: 处理权限错误
     */
    public void handlePermissionError() {
        // 检查麦克风权限
        if (!PermissionUtils.hasAudioPermission(context)) {
            // 处理权限错误
            errorHandler.handleError(
                TranscriptionError.MICROPHONE_PERMISSION_DENIED,
                null,
                new ErrorHandler.ErrorCallback() {
                    @Override
                    public void onSettingsRequested() {
                        logManager.i(TAG, "用户选择打开设置页面");
                        // 权限设置页面已自动打开
                    }
                    
                    @Override
                    public void onRetryRequested() {
                        logManager.i(TAG, "用户请求重试权限检查");
                        // 重新检查权限
                        recheckPermissions();
                    }
                }
            );
        }
    }
    
    /**
     * 示例3: 处理网络错误
     */
    public void handleNetworkError() {
        try {
            // 模拟网络请求失败
            if (!NetworkUtils.isNetworkAvailable(context)) {
                throw new RuntimeException("网络不可用");
            }
            
        } catch (Exception e) {
            // 网络错误会自动尝试恢复
            errorHandler.handleError(
                TranscriptionError.NETWORK_ERROR,
                e,
                new ErrorHandler.ErrorCallback() {
                    @Override
                    public void onRecoverySuccess() {
                        logManager.i(TAG, "网络错误已自动恢复");
                        // 继续执行网络操作
                        continueNetworkOperation();
                    }
                    
                    @Override
                    public void onRetryRequested() {
                        logManager.i(TAG, "用户手动重试网络操作");
                        // 重新执行网络操作
                        retryNetworkOperation();
                    }
                }
            );
        }
    }
    
    /**
     * 示例4: 记录详细的错误报告
     */
    public void generateDetailedErrorReport() {
        try {
            // 模拟转录失败
            throw new RuntimeException("转录引擎初始化失败");
            
        } catch (Exception e) {
            // 生成详细错误报告
            String additionalInfo = "用户正在进行实时转录，音频质量良好，但引擎突然崩溃";
            String report = errorReporter.generateErrorReport(
                TranscriptionError.TRANSCRIPTION_FAILED, 
                e, 
                additionalInfo
            );
            
            // 记录到日志
            logManager.e(TAG, "详细错误报告:\n" + report);
            
            // 保存到文件
            errorReporter.saveErrorReport(
                TranscriptionError.TRANSCRIPTION_FAILED, 
                e, 
                additionalInfo
            );
        }
    }
    
    /**
     * 示例5: 使用不同级别的日志
     */
    public void demonstrateLogging() {
        // 调试信息
        logManager.d(TAG, "开始初始化语音识别引擎");
        
        // 一般信息
        logManager.i(TAG, "语音识别引擎初始化完成");
        
        // 警告信息
        logManager.w(TAG, "音频质量较低，可能影响识别准确率");
        
        // 错误信息
        logManager.e(TAG, "语音识别失败，请检查音频输入");
        
        // 带异常的错误信息
        try {
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            logManager.e(TAG, "捕获到异常", e);
        }
    }
    
    /**
     * 示例6: 错误统计和监控
     */
    public void demonstrateErrorStatistics() {
        // 模拟多个错误
        errorHandler.handleError(TranscriptionError.AUDIO_RECORDING_FAILED);
        errorHandler.handleError(TranscriptionError.NETWORK_TIMEOUT);
        errorHandler.handleError(TranscriptionError.AUDIO_RECORDING_FAILED);
        
        // 获取错误统计
        var statistics = logManager.getErrorStatistics();
        logManager.i(TAG, "当前错误统计: " + statistics.toString());
        
        // 检查特定错误的重试次数
        int retryCount = errorHandler.getErrorRetryCount(TranscriptionError.AUDIO_RECORDING_FAILED);
        logManager.i(TAG, "音频录制失败重试次数: " + retryCount);
        
        // 重置错误计数
        errorHandler.resetErrorCount(TranscriptionError.AUDIO_RECORDING_FAILED);
        logManager.i(TAG, "已重置音频录制错误计数");
    }
    
    // 辅助方法
    private void retryModelLoading() {
        logManager.d(TAG, "重新尝试加载模型");
        // 实际的模型加载逻辑
    }
    
    private void recheckPermissions() {
        logManager.d(TAG, "重新检查权限状态");
        // 实际的权限检查逻辑
    }
    
    private void continueNetworkOperation() {
        logManager.d(TAG, "继续执行网络操作");
        // 实际的网络操作逻辑
    }
    
    private void retryNetworkOperation() {
        logManager.d(TAG, "重试网络操作");
        // 实际的网络重试逻辑
    }
}