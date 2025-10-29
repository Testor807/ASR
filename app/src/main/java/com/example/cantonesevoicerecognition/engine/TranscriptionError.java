package com.example.cantonesevoicerecognition.engine;

/**
 * Enumeration of transcription error types
 */
public enum TranscriptionError {
    MODEL_NOT_LOADED("语音模型未加载"),
    AUDIO_FORMAT_UNSUPPORTED("不支持的音频格式"),
    INSUFFICIENT_STORAGE("存储空间不足"),
    PERMISSION_DENIED("权限被拒绝"),
    NETWORK_ERROR("网络错误"),
    MODEL_CORRUPTED("模型文件损坏"),
    TRANSCRIPTION_FAILED("转录失败"),
    UNKNOWN_ERROR("未知错误");
    
    private final String message;
    
    TranscriptionError(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return message;
    }
}