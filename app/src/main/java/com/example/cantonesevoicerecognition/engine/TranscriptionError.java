package com.example.cantonesevoicerecognition.engine;

import android.content.Context;
import com.example.cantonesevoicerecognition.R;

/**
 * 转录错误类型枚举
 * 提供完整的错误分类和本地化消息支持
 */
public enum TranscriptionError {
    // 模型相关错误
    MODEL_NOT_LOADED("语音模型未加载", R.string.error_model_not_loaded),
    MODEL_CORRUPTED("模型文件损坏", R.string.error_model_corrupted),
    MODEL_LOADING_FAILED("模型加载失败", R.string.error_model_loading_failed),
    MODEL_INITIALIZATION_FAILED("模型初始化失败", R.string.error_model_initialization_failed),
    
    // 音频相关错误
    AUDIO_FORMAT_UNSUPPORTED("不支持的音频格式", R.string.error_audio_format_unsupported),
    AUDIO_RECORDING_FAILED("音频录制失败", R.string.error_audio_recording_failed),
    AUDIO_PROCESSING_FAILED("音频处理失败", R.string.error_audio_processing_failed),
    MICROPHONE_UNAVAILABLE("麦克风不可用", R.string.error_microphone_unavailable),
    AUDIO_QUALITY_TOO_LOW("音频质量过低", R.string.error_audio_quality_too_low),
    
    // 权限相关错误
    PERMISSION_DENIED("权限被拒绝", R.string.error_permission_denied),
    MICROPHONE_PERMISSION_DENIED("麦克风权限被拒绝", R.string.error_microphone_permission_denied),
    STORAGE_PERMISSION_DENIED("存储权限被拒绝", R.string.error_storage_permission_denied),
    
    // 存储相关错误
    INSUFFICIENT_STORAGE("存储空间不足", R.string.error_insufficient_storage),
    FILE_NOT_FOUND("文件未找到", R.string.error_file_not_found),
    FILE_READ_ERROR("文件读取错误", R.string.error_file_read_error),
    FILE_WRITE_ERROR("文件写入错误", R.string.error_file_write_error),
    DATABASE_ERROR("数据库错误", R.string.error_database_error),
    
    // 网络相关错误
    NETWORK_ERROR("网络错误", R.string.error_network_error),
    NETWORK_TIMEOUT("网络超时", R.string.error_network_timeout),
    SERVER_ERROR("服务器错误", R.string.error_server_error),
    MODEL_DOWNLOAD_FAILED("模型下载失败", R.string.error_model_download_failed),
    
    // 转录相关错误
    TRANSCRIPTION_FAILED("转录失败", R.string.error_transcription_failed),
    TRANSCRIPTION_TIMEOUT("转录超时", R.string.error_transcription_timeout),
    TRANSCRIPTION_INTERRUPTED("转录被中断", R.string.error_transcription_interrupted),
    REAL_TIME_TRANSCRIPTION_FAILED("实时转录失败", R.string.error_real_time_transcription_failed),
    
    // 系统相关错误
    INSUFFICIENT_MEMORY("内存不足", R.string.error_insufficient_memory),
    DEVICE_NOT_SUPPORTED("设备不支持", R.string.error_device_not_supported),
    SERVICE_UNAVAILABLE("服务不可用", R.string.error_service_unavailable),
    INITIALIZATION_FAILED("初始化失败", R.string.error_initialization_failed),
    
    // 通用错误
    UNKNOWN_ERROR("未知错误", R.string.error_unknown_error),
    OPERATION_CANCELLED("操作已取消", R.string.error_operation_cancelled),
    INVALID_PARAMETER("无效参数", R.string.error_invalid_parameter);
    
    private final String defaultMessage;
    private final int stringResourceId;
    
    TranscriptionError(String defaultMessage, int stringResourceId) {
        this.defaultMessage = defaultMessage;
        this.stringResourceId = stringResourceId;
    }
    
    /**
     * 获取默认错误消息
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    /**
     * 获取本地化错误消息
     * @param context Android上下文
     * @return 本地化的错误消息
     */
    public String getLocalizedMessage(Context context) {
        try {
            return context.getString(stringResourceId);
        } catch (Exception e) {
            return defaultMessage;
        }
    }
    
    /**
     * 获取字符串资源ID
     */
    public int getStringResourceId() {
        return stringResourceId;
    }
    
    /**
     * 检查是否为严重错误（需要立即处理）
     */
    public boolean isCritical() {
        switch (this) {
            case MODEL_CORRUPTED:
            case INSUFFICIENT_MEMORY:
            case DATABASE_ERROR:
            case DEVICE_NOT_SUPPORTED:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为可恢复错误
     */
    public boolean isRecoverable() {
        switch (this) {
            case NETWORK_ERROR:
            case NETWORK_TIMEOUT:
            case TRANSCRIPTION_TIMEOUT:
            case AUDIO_RECORDING_FAILED:
            case FILE_READ_ERROR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 获取错误类别
     */
    public ErrorCategory getCategory() {
        switch (this) {
            case MODEL_NOT_LOADED:
            case MODEL_CORRUPTED:
            case MODEL_LOADING_FAILED:
            case MODEL_INITIALIZATION_FAILED:
                return ErrorCategory.MODEL;
                
            case AUDIO_FORMAT_UNSUPPORTED:
            case AUDIO_RECORDING_FAILED:
            case AUDIO_PROCESSING_FAILED:
            case MICROPHONE_UNAVAILABLE:
            case AUDIO_QUALITY_TOO_LOW:
                return ErrorCategory.AUDIO;
                
            case PERMISSION_DENIED:
            case MICROPHONE_PERMISSION_DENIED:
            case STORAGE_PERMISSION_DENIED:
                return ErrorCategory.PERMISSION;
                
            case INSUFFICIENT_STORAGE:
            case FILE_NOT_FOUND:
            case FILE_READ_ERROR:
            case FILE_WRITE_ERROR:
            case DATABASE_ERROR:
                return ErrorCategory.STORAGE;
                
            case NETWORK_ERROR:
            case NETWORK_TIMEOUT:
            case SERVER_ERROR:
            case MODEL_DOWNLOAD_FAILED:
                return ErrorCategory.NETWORK;
                
            case TRANSCRIPTION_FAILED:
            case TRANSCRIPTION_TIMEOUT:
            case TRANSCRIPTION_INTERRUPTED:
            case REAL_TIME_TRANSCRIPTION_FAILED:
                return ErrorCategory.TRANSCRIPTION;
                
            default:
                return ErrorCategory.SYSTEM;
        }
    }
    
    @Override
    public String toString() {
        return defaultMessage;
    }
    
    /**
     * 错误类别枚举
     */
    public enum ErrorCategory {
        MODEL("模型"),
        AUDIO("音频"),
        PERMISSION("权限"),
        STORAGE("存储"),
        NETWORK("网络"),
        TRANSCRIPTION("转录"),
        SYSTEM("系统");
        
        private final String displayName;
        
        ErrorCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}