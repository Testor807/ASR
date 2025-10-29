package com.example.cantonesevoicerecognition.data.model;

public class AudioData {
    private byte[] rawData;
    private int sampleRate = 16000; // 16kHz
    private int channels = 1; // 单声道
    private int bitDepth = 16; // 16位
    private long duration;
    
    // 构造函数
    public AudioData() {
        this.sampleRate = 16000;
        this.channels = 1;
        this.bitDepth = 16;
        this.duration = 0;
    }
    
    public AudioData(byte[] rawData) {
        this();
        this.rawData = rawData;
        if (rawData != null) {
            // 计算音频时长 (毫秒)
            this.duration = calculateDuration();
        }
    }
    
    public AudioData(byte[] rawData, int sampleRate, int channels, int bitDepth) {
        this.rawData = rawData;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitDepth = bitDepth;
        this.duration = calculateDuration();
    }
    
    // Getter方法
    public byte[] getRawData() {
        return rawData;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public int getChannels() {
        return channels;
    }
    
    public int getBitDepth() {
        return bitDepth;
    }
    
    public long getDuration() {
        return duration;
    }
    
    // Setter方法
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
        this.duration = calculateDuration();
    }
    
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        this.duration = calculateDuration();
    }
    
    public void setChannels(int channels) {
        this.channels = channels;
        this.duration = calculateDuration();
    }
    
    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
        this.duration = calculateDuration();
    }
    
    // 音频格式验证方法
    public boolean isValidFormat() {
        return sampleRate > 0 && channels > 0 && 
               bitDepth > 0 && rawData != null && rawData.length > 0;
    }
    
    // 计算音频时长
    private long calculateDuration() {
        if (rawData == null || rawData.length == 0 || sampleRate <= 0) {
            return 0;
        }
        
        // 计算公式: 时长(毫秒) = (数据字节数 / (采样率 * 声道数 * 位深度/8)) * 1000
        int bytesPerSample = (bitDepth / 8) * channels;
        return (long) ((rawData.length / (double) bytesPerSample / sampleRate) * 1000);
    }
    
    // 获取音频数据大小
    public int getDataSize() {
        return rawData != null ? rawData.length : 0;
    }
    
    // 检查是否为空
    public boolean isEmpty() {
        return rawData == null || rawData.length == 0;
    }
    
    // 获取格式化的时长字符串
    public String getFormattedDuration() {
        long durationSeconds = duration / 1000;
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    // 创建音频数据的副本
    public AudioData copy() {
        byte[] dataCopy = null;
        if (rawData != null) {
            dataCopy = new byte[rawData.length];
            System.arraycopy(rawData, 0, dataCopy, 0, rawData.length);
        }
        return new AudioData(dataCopy, sampleRate, channels, bitDepth);
    }
}