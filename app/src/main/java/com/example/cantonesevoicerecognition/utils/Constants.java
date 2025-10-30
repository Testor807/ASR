package com.example.cantonesevoicerecognition.utils;

/**
 * Application constants
 */
public class Constants {
    
    // Audio Configuration
    public static final int SAMPLE_RATE = 16000; // 16kHz
    public static final int CHANNEL_CONFIG = 1; // Mono
    public static final int BIT_DEPTH = 16; // 16-bit
    public static final int BUFFER_SIZE = 1024;
    
    // Model Configuration
    public static final String MODEL_FILE_NAME = "whisper_cantonese.onnx";
    public static final String MODEL_DIRECTORY = "models";
    
    // Database Configuration
    public static final String DATABASE_NAME = "transcription_database";
    public static final int DATABASE_VERSION = 1;
    
    // Transcription Configuration
    public static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.7f;
    public static final int MAX_AUDIO_DURATION_SECONDS = 300; // 5 minutes
    public static final int REAL_TIME_BUFFER_DURATION_MS = 2000; // 2 seconds
    
    // UI Configuration
    public static final int ANIMATION_DURATION_MS = 300;
    public static final int DEBOUNCE_DELAY_MS = 500;
    
    // Permissions
    public static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
    public static final int PERMISSION_REQUEST_STORAGE = 1002;
    public static final int PERMISSION_REQUEST_ALL = 1003;
    public static final int PERMISSION_REQUEST_FOREGROUND_SERVICE = 1004;
    
    // Intent Actions
    public static final String ACTION_START_TRANSCRIPTION = "com.example.cantonesevoicerecognition.START_TRANSCRIPTION";
    public static final String ACTION_STOP_TRANSCRIPTION = "com.example.cantonesevoicerecognition.STOP_TRANSCRIPTION";
    
    // Shared Preferences Keys
    public static final String PREF_REAL_TIME_MODE = "pref_real_time_mode";
    public static final String PREF_AUTO_SAVE = "pref_auto_save";
    public static final String PREF_CONFIDENCE_THRESHOLD = "pref_confidence_threshold";
    
    private Constants() {
        // Prevent instantiation
    }
}