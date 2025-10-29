package com.example.cantonesevoicerecognition.audio;

import android.util.Log;
import com.example.cantonesevoicerecognition.data.model.AudioData;

/**
 * 音频处理工具类
 * 提供音频格式转换、重采样、归一化等功能
 */
public class AudioProcessor {
    private static final String TAG = "AudioProcessor";
    
    // 音频处理常量
    private static final int TARGET_SAMPLE_RATE = 16000; // Whisper要求的采样率
    private static final int TARGET_CHANNELS = 1; // 单声道
    private static final int TARGET_BIT_DEPTH = 16; // 16位
    private static final float NORMALIZATION_FACTOR = 0.5f; // 归一化系数
    
    /**
     * 将原始音频转换为Whisper所需格式
     * @param rawAudio 原始音频数据
     * @param sampleRate 原始采样率
     * @return 转换后的AudioData对象
     */
    public static AudioData convertToWhisperFormat(byte[] rawAudio, int sampleRate) {
        if (rawAudio == null || rawAudio.length == 0) {
            Log.w(TAG, "Empty audio data provided");
            return new AudioData();
        }
        
        Log.d(TAG, "Converting audio: " + rawAudio.length + " bytes, " + sampleRate + "Hz");
        
        byte[] processedAudio = rawAudio;
        
        // 重采样到16kHz（如果需要）
        if (sampleRate != TARGET_SAMPLE_RATE) {
            processedAudio = resampleAudio(rawAudio, sampleRate, TARGET_SAMPLE_RATE);
            Log.d(TAG, "Resampled from " + sampleRate + "Hz to " + TARGET_SAMPLE_RATE + "Hz");
        }
        
        // 归一化音频数据
        processedAudio = normalizeAudio(processedAudio);
        
        // 创建AudioData对象
        AudioData audioData = new AudioData(processedAudio, TARGET_SAMPLE_RATE, TARGET_CHANNELS, TARGET_BIT_DEPTH);
        
        Log.d(TAG, "Audio conversion completed: " + processedAudio.length + " bytes, duration: " + audioData.getDuration() + "ms");
        
        return audioData;
    }
    
    /**
     * 音频重采样
     * @param input 输入音频数据
     * @param inputRate 输入采样率
     * @param outputRate 输出采样率
     * @return 重采样后的音频数据
     */
    private static byte[] resampleAudio(byte[] input, int inputRate, int outputRate) {
        if (input.length < 2) {
            return input;
        }
        
        double ratio = (double) outputRate / inputRate;
        int outputLength = (int) (input.length * ratio);
        
        // 确保输出长度为偶数（16位音频每个样本2字节）
        if (outputLength % 2 != 0) {
            outputLength--;
        }
        
        byte[] output = new byte[outputLength];
        
        // 线性插值重采样
        for (int i = 0; i < outputLength; i += 2) {
            double inputIndex = i / ratio;
            int index1 = (int) inputIndex;
            int index2 = Math.min(index1 + 2, input.length - 2);
            
            if (index1 >= input.length - 1) {
                break;
            }
            
            // 获取两个相邻样本
            short sample1 = (short) ((input[index1 + 1] << 8) | (input[index1] & 0xFF));
            short sample2 = (short) ((input[index2 + 1] << 8) | (input[index2] & 0xFF));
            
            // 线性插值
            double fraction = (inputIndex - index1) / 2.0;
            short interpolatedSample = (short) (sample1 + fraction * (sample2 - sample1));
            
            // 写入输出
            output[i] = (byte) (interpolatedSample & 0xFF);
            output[i + 1] = (byte) ((interpolatedSample >> 8) & 0xFF);
        }
        
        return output;
    }
    
    /**
     * 音频归一化
     * @param audio 输入音频数据
     * @return 归一化后的音频数据
     */
    private static byte[] normalizeAudio(byte[] audio) {
        if (audio.length < 2) {
            return audio;
        }
        
        // 找到最大振幅
        short maxAmplitude = 0;
        for (int i = 0; i < audio.length; i += 2) {
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
            maxAmplitude = (short) Math.max(maxAmplitude, Math.abs(sample));
        }
        
        if (maxAmplitude == 0) {
            Log.w(TAG, "Audio contains only silence");
            return audio;
        }
        
        // 计算归一化系数
        float normalizationFactor = (Short.MAX_VALUE * NORMALIZATION_FACTOR) / maxAmplitude;
        
        // 如果音频已经足够大声，不需要归一化
        if (normalizationFactor <= 1.0f) {
            return audio;
        }
        
        byte[] normalized = new byte[audio.length];
        
        for (int i = 0; i < audio.length; i += 2) {
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
            sample = (short) Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, sample * normalizationFactor));
            
            normalized[i] = (byte) (sample & 0xFF);
            normalized[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        Log.d(TAG, "Audio normalized with factor: " + normalizationFactor);
        return normalized;
    }
    
    /**
     * 语音活动检测 (VAD)
     * @param audioData 音频数据
     * @param threshold 阈值
     * @return 是否检测到语音活动
     */
    public static boolean detectVoiceActivity(byte[] audioData, float threshold) {
        if (audioData == null || audioData.length < 2) {
            return false;
        }
        
        double energy = calculateAudioEnergy(audioData);
        boolean hasVoice = energy > threshold;
        
        Log.v(TAG, "VAD: energy=" + energy + ", threshold=" + threshold + ", hasVoice=" + hasVoice);
        return hasVoice;
    }
    
    /**
     * 计算音频能量
     * @param audioData 音频数据
     * @return 音频能量值
     */
    public static double calculateAudioEnergy(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0.0;
        }
        
        double sum = 0;
        int sampleCount = 0;
        
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
            sampleCount++;
        }
        
        return sampleCount > 0 ? sum / sampleCount : 0.0;
    }
    
    /**
     * 计算音频音量级别
     * @param audioData 音频数据
     * @return 音量级别 (0.0 - 1.0)
     */
    public static float calculateVolumeLevel(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0.0f;
        }
        
        short maxAmplitude = 0;
        
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            maxAmplitude = (short) Math.max(maxAmplitude, Math.abs(sample));
        }
        
        return (float) maxAmplitude / Short.MAX_VALUE;
    }
    
    /**
     * 应用简单的噪声抑制
     * @param audioData 音频数据
     * @param noiseThreshold 噪声阈值
     * @return 处理后的音频数据
     */
    public static byte[] applyNoiseReduction(byte[] audioData, float noiseThreshold) {
        if (audioData == null || audioData.length < 2) {
            return audioData;
        }
        
        byte[] processed = new byte[audioData.length];
        
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            
            // 如果样本幅度低于噪声阈值，将其设为0
            if (Math.abs(sample) < noiseThreshold * Short.MAX_VALUE) {
                sample = 0;
            }
            
            processed[i] = (byte) (sample & 0xFF);
            processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return processed;
    }
    
    /**
     * 检查音频数据是否有效
     * @param audioData 音频数据
     * @return 是否有效
     */
    public static boolean isValidAudioData(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return false;
        }
        
        // 检查是否为偶数长度（16位音频）
        if (audioData.length % 2 != 0) {
            return false;
        }
        
        // 检查是否全为静音
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            if (sample != 0) {
                return true;
            }
        }
        
        return false; // 全为静音
    }
    
    /**
     * 获取音频统计信息
     * @param audioData 音频数据
     * @return 统计信息字符串
     */
    public static String getAudioStats(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return "无效音频数据";
        }
        
        double energy = calculateAudioEnergy(audioData);
        float volume = calculateVolumeLevel(audioData);
        int samples = audioData.length / 2;
        long durationMs = samples * 1000L / TARGET_SAMPLE_RATE;
        
        return String.format("样本数: %d, 时长: %dms, 能量: %.2f, 音量: %.2f", 
                           samples, durationMs, energy, volume);
    }
}