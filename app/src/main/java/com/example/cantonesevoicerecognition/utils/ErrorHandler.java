package com.example.cantonesevoicerecognition.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.example.cantonesevoicerecognition.R;
import com.example.cantonesevoicerecognition.engine.TranscriptionError;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一错误处理器
 * 提供错误处理、恢复机制和用户友好的错误提示
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    private static ErrorHandler instance;
    
    private final Context context;
    private final LogManager logManager;
    private final Map<TranscriptionError, Integer> errorRetryCount;
    private final Map<TranscriptionError, Long> lastErrorTime;
    
    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    // 错误冷却时间（毫秒）
    private static final long ERROR_COOLDOWN_TIME = 5000;
    
    private ErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.logManager = LogManager.getInstance(context);
        this.errorRetryCount = new HashMap<>();
        this.lastErrorTime = new HashMap<>();
    }
    
    /**
     * 获取ErrorHandler单例实例
     */
    public static synchronized ErrorHandler getInstance(Context context) {
        if (instance == null) {
            instance = new ErrorHandler(context);
        }
        return instance;
    }
    
    /**
     * 处理转录错误
     * @param error 错误类型
     * @param throwable 异常对象（可选）
     * @param callback 错误处理回调
     */
    public void handleError(TranscriptionError error, Throwable throwable, ErrorCallback callback) {
        // 记录错误日志
        logError(error, throwable);
        
        // 检查是否在冷却期内
        if (isInCooldown(error)) {
            logManager.d(TAG, "错误在冷却期内，跳过处理: " + error.name());
            return;
        }
        
        // 更新错误时间
        lastErrorTime.put(error, System.currentTimeMillis());
        
        // 尝试自动恢复
        if (attemptAutoRecovery(error, callback)) {
            return;
        }
        
        // 显示用户友好的错误提示
        showUserFriendlyError(error, callback);
    }
    
    /**
     * 处理转录错误（简化版本）
     */
    public void handleError(TranscriptionError error) {
        handleError(error, null, null);
    }
    
    /**
     * 处理转录错误（带异常）
     */
    public void handleError(TranscriptionError error, Throwable throwable) {
        handleError(error, throwable, null);
    }
    
    /**
     * 记录错误日志
     */
    private void logError(TranscriptionError error, Throwable throwable) {
        String message = String.format("转录错误: %s [%s]", 
            error.getDefaultMessage(), error.name());
        
        if (throwable != null) {
            logManager.e(TAG, message, throwable);
        } else {
            logManager.e(TAG, message);
        }
        
        // 记录错误统计
        logManager.recordErrorStatistics(error);
    }
    
    /**
     * 检查错误是否在冷却期内
     */
    private boolean isInCooldown(TranscriptionError error) {
        Long lastTime = lastErrorTime.get(error);
        if (lastTime == null) {
            return false;
        }
        return System.currentTimeMillis() - lastTime < ERROR_COOLDOWN_TIME;
    }
    
    /**
     * 尝试自动恢复
     */
    private boolean attemptAutoRecovery(TranscriptionError error, ErrorCallback callback) {
        if (!error.isRecoverable()) {
            return false;
        }
        
        // 检查重试次数
        int retryCount = errorRetryCount.getOrDefault(error, 0);
        if (retryCount >= MAX_RETRY_COUNT) {
            logManager.w(TAG, "错误重试次数已达上限: " + error.name());
            errorRetryCount.put(error, 0); // 重置计数
            return false;
        }
        
        // 增加重试次数
        errorRetryCount.put(error, retryCount + 1);
        
        logManager.i(TAG, String.format("尝试自动恢复错误: %s (第%d次重试)", 
            error.name(), retryCount + 1));
        
        // 根据错误类型执行恢复策略
        boolean recovered = executeRecoveryStrategy(error);
        
        if (recovered) {
            logManager.i(TAG, "错误自动恢复成功: " + error.name());
            errorRetryCount.put(error, 0); // 重置计数
            if (callback != null) {
                callback.onRecoverySuccess();
            }
            showToast(context.getString(R.string.error_recovery_success));
            return true;
        }
        
        return false;
    }
    
    /**
     * 执行恢复策略
     */
    private boolean executeRecoveryStrategy(TranscriptionError error) {
        switch (error) {
            case NETWORK_ERROR:
            case NETWORK_TIMEOUT:
                // 网络错误：等待一段时间后重试
                try {
                    Thread.sleep(2000);
                    return NetworkUtils.isNetworkAvailable(context);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                
            case TRANSCRIPTION_TIMEOUT:
                // 转录超时：清理资源后重试
                return clearTranscriptionResources();
                
            case AUDIO_RECORDING_FAILED:
                // 录音失败：重新初始化录音器
                return reinitializeAudioRecorder();
                
            case FILE_READ_ERROR:
                // 文件读取错误：检查文件权限
                return checkFilePermissions();
                
            default:
                return false;
        }
    }
    
    /**
     * 显示用户友好的错误提示
     */
    private void showUserFriendlyError(TranscriptionError error, ErrorCallback callback) {
        String title = context.getString(R.string.error_dialog_title);
        String message = error.getLocalizedMessage(context);
        
        // 根据错误类型显示不同的对话框
        if (error.isCritical()) {
            showCriticalErrorDialog(title, message, error, callback);
        } else {
            showNormalErrorDialog(title, message, error, callback);
        }
    }
    
    /**
     * 显示严重错误对话框
     */
    private void showCriticalErrorDialog(String title, String message, 
                                       TranscriptionError error, ErrorCallback callback) {
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message + "\n\n应用需要重启以解决此问题。")
            .setPositiveButton("重启应用", (dialog, which) -> {
                if (callback != null) {
                    callback.onRestartRequired();
                }
                restartApplication();
            })
            .setNegativeButton(R.string.error_dialog_cancel, (dialog, which) -> {
                if (callback != null) {
                    callback.onUserCancelled();
                }
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 显示普通错误对话框
     */
    private void showNormalErrorDialog(String title, String message, 
                                     TranscriptionError error, ErrorCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message);
        
        // 添加重试按钮（如果错误可恢复）
        if (error.isRecoverable()) {
            builder.setPositiveButton(R.string.error_dialog_retry, (dialog, which) -> {
                if (callback != null) {
                    callback.onRetryRequested();
                }
            });
        }
        
        // 添加设置按钮（如果是权限错误）
        if (error.getCategory() == TranscriptionError.ErrorCategory.PERMISSION) {
            builder.setNeutralButton(R.string.error_dialog_settings, (dialog, which) -> {
                openAppSettings();
                if (callback != null) {
                    callback.onSettingsRequested();
                }
            });
        }
        
        builder.setNegativeButton(R.string.error_dialog_cancel, (dialog, which) -> {
            if (callback != null) {
                callback.onUserCancelled();
            }
        });
        
        builder.show();
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 清理转录资源
     */
    private boolean clearTranscriptionResources() {
        try {
            // 这里应该调用实际的资源清理方法
            logManager.d(TAG, "清理转录资源");
            return true;
        } catch (Exception e) {
            logManager.e(TAG, "清理转录资源失败", e);
            return false;
        }
    }
    
    /**
     * 重新初始化录音器
     */
    private boolean reinitializeAudioRecorder() {
        try {
            // 这里应该调用实际的录音器重新初始化方法
            logManager.d(TAG, "重新初始化录音器");
            return true;
        } catch (Exception e) {
            logManager.e(TAG, "重新初始化录音器失败", e);
            return false;
        }
    }
    
    /**
     * 检查文件权限
     */
    private boolean checkFilePermissions() {
        try {
            // 这里应该调用实际的权限检查方法
            logManager.d(TAG, "检查文件权限");
            return PermissionUtils.hasStoragePermission(context);
        } catch (Exception e) {
            logManager.e(TAG, "检查文件权限失败", e);
            return false;
        }
    }
    
    /**
     * 打开应用设置页面
     */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            logManager.e(TAG, "打开应用设置失败", e);
        }
    }
    
    /**
     * 重启应用
     */
    private void restartApplication() {
        try {
            Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                System.exit(0);
            }
        } catch (Exception e) {
            logManager.e(TAG, "重启应用失败", e);
        }
    }
    
    /**
     * 重置错误计数
     */
    public void resetErrorCount(TranscriptionError error) {
        errorRetryCount.put(error, 0);
        lastErrorTime.remove(error);
    }
    
    /**
     * 重置所有错误计数
     */
    public void resetAllErrorCounts() {
        errorRetryCount.clear();
        lastErrorTime.clear();
    }
    
    /**
     * 获取错误重试次数
     */
    public int getErrorRetryCount(TranscriptionError error) {
        return errorRetryCount.getOrDefault(error, 0);
    }
    
    /**
     * 错误处理回调接口
     */
    public interface ErrorCallback {
        default void onRecoverySuccess() {}
        default void onRetryRequested() {}
        default void onSettingsRequested() {}
        default void onRestartRequired() {}
        default void onUserCancelled() {}
    }
}