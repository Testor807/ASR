package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 设置管理器 - 处理应用设置的保存和读取
 */
public class SettingsManager {
    
    private static final String PREFS_NAME = "cantonese_voice_settings";
    
    // 设置键名
    public static final String KEY_OFFLINE_MODE = "offline_mode";
    public static final String KEY_CONFIDENCE_THRESHOLD = "confidence_threshold";
    public static final String KEY_AUTO_SAVE = "auto_save";
    public static final String KEY_AUDIO_QUALITY = "audio_quality";
    public static final String KEY_NOISE_REDUCTION = "noise_reduction";
    public static final String KEY_VAD_ENABLED = "vad_enabled";
    public static final String KEY_THEME = "theme";
    public static final String KEY_LANGUAGE = "language";
    
    // 音频质量选项
    public static final String AUDIO_QUALITY_HIGH = "high";
    public static final String AUDIO_QUALITY_STANDARD = "standard";
    public static final String AUDIO_QUALITY_LOW = "low";
    
    // 主题选项
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    
    // 语言选项
    public static final String LANGUAGE_CHINESE = "zh";
    public static final String LANGUAGE_ENGLISH = "en";
    
    private SharedPreferences preferences;
    private static SettingsManager instance;
    
    private SettingsManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // 转录设置
    
    /**
     * 获取离线模式设置
     */
    public boolean isOfflineModeEnabled() {
        return preferences.getBoolean(KEY_OFFLINE_MODE, false);
    }
    
    /**
     * 设置离线模式
     */
    public void setOfflineModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_OFFLINE_MODE, enabled).apply();
    }
    
    /**
     * 获取置信度阈值
     */
    public float getConfidenceThreshold() {
        return preferences.getFloat(KEY_CONFIDENCE_THRESHOLD, Constants.DEFAULT_CONFIDENCE_THRESHOLD);
    }
    
    /**
     * 设置置信度阈值
     */
    public void setConfidenceThreshold(float threshold) {
        preferences.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, threshold).apply();
    }
    
    /**
     * 获取自动保存设置
     */
    public boolean isAutoSaveEnabled() {
        return preferences.getBoolean(KEY_AUTO_SAVE, true);
    }
    
    /**
     * 设置自动保存
     */
    public void setAutoSaveEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply();
    }
    
    // 音频设置
    
    /**
     * 获取音频质量设置
     */
    public String getAudioQuality() {
        return preferences.getString(KEY_AUDIO_QUALITY, AUDIO_QUALITY_STANDARD);
    }
    
    /**
     * 设置音频质量
     */
    public void setAudioQuality(String quality) {
        preferences.edit().putString(KEY_AUDIO_QUALITY, quality).apply();
    }
    
    /**
     * 获取音频质量对应的采样率
     */
    public int getAudioSampleRate() {
        String quality = getAudioQuality();
        switch (quality) {
            case AUDIO_QUALITY_HIGH:
                return 44100;
            case AUDIO_QUALITY_LOW:
                return 8000;
            case AUDIO_QUALITY_STANDARD:
            default:
                return 16000;
        }
    }
    
    /**
     * 获取噪声抑制设置
     */
    public boolean isNoiseReductionEnabled() {
        return preferences.getBoolean(KEY_NOISE_REDUCTION, true);
    }
    
    /**
     * 设置噪声抑制
     */
    public void setNoiseReductionEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOISE_REDUCTION, enabled).apply();
    }
    
    /**
     * 获取语音活动检测设置
     */
    public boolean isVadEnabled() {
        return preferences.getBoolean(KEY_VAD_ENABLED, true);
    }
    
    /**
     * 设置语音活动检测
     */
    public void setVadEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VAD_ENABLED, enabled).apply();
    }
    
    // 应用设置
    
    /**
     * 获取主题设置
     */
    public String getTheme() {
        return preferences.getString(KEY_THEME, THEME_SYSTEM);
    }
    
    /**
     * 设置主题
     */
    public void setTheme(String theme) {
        preferences.edit().putString(KEY_THEME, theme).apply();
    }
    
    /**
     * 获取语言设置
     */
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, LANGUAGE_CHINESE);
    }
    
    /**
     * 设置语言
     */
    public void setLanguage(String language) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    /**
     * 获取主题显示名称
     */
    public String getThemeDisplayName() {
        String theme = getTheme();
        switch (theme) {
            case THEME_LIGHT:
                return "浅色主题";
            case THEME_DARK:
                return "深色主题";
            case THEME_SYSTEM:
            default:
                return "跟随系统";
        }
    }
    
    /**
     * 获取语言显示名称
     */
    public String getLanguageDisplayName() {
        String language = getLanguage();
        switch (language) {
            case LANGUAGE_ENGLISH:
                return "English";
            case LANGUAGE_CHINESE:
            default:
                return "简体中文";
        }
    }
    
    /**
     * 获取音频质量显示名称
     */
    public String getAudioQualityDisplayName() {
        String quality = getAudioQuality();
        switch (quality) {
            case AUDIO_QUALITY_HIGH:
                return "高质量 (44.1kHz, 16bit)";
            case AUDIO_QUALITY_LOW:
                return "低质量 (8kHz, 16bit)";
            case AUDIO_QUALITY_STANDARD:
            default:
                return "标准质量 (16kHz, 16bit)";
        }
    }
    
    /**
     * 重置所有设置为默认值
     */
    public void resetToDefaults() {
        preferences.edit().clear().apply();
    }
    
    /**
     * 注册设置变更监听器
     */
    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }
    
    /**
     * 注销设置变更监听器
     */
    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }
}