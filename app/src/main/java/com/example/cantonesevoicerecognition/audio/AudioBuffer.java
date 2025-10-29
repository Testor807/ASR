package com.example.cantonesevoicerecognition.audio;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 音频缓冲区类
 * 用于管理音频数据的缓存和获取
 */
public class AudioBuffer {
    private static final String TAG = "AudioBuffer";
    
    private final Queue<byte[]> audioQueue = new LinkedList<>();
    private final int maxBufferSize;
    private int currentSize = 0;
    private final Object lock = new Object();
    
    /**
     * 构造函数
     * @param maxBufferSizeMs 最大缓冲区大小（毫秒）
     */
    public AudioBuffer(int maxBufferSizeMs) {
        // 计算缓冲区字节大小：16kHz * 16bit * 1channel * maxBufferSizeMs / 1000
        this.maxBufferSize = 16000 * 2 * maxBufferSizeMs / 1000;
        Log.i(TAG, "AudioBuffer created with max size: " + maxBufferSize + " bytes (" + maxBufferSizeMs + "ms)");
    }
    
    /**
     * 添加音频数据到缓冲区
     * @param data 音频数据
     */
    public void addAudioData(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        
        synchronized (lock) {
            audioQueue.offer(data.clone()); // 创建副本避免外部修改
            currentSize += data.length;
            
            // 限制缓冲区大小，移除最旧的数据
            while (currentSize > maxBufferSize && !audioQueue.isEmpty()) {
                byte[] removed = audioQueue.poll();
                if (removed != null) {
                    currentSize -= removed.length;
                }
            }
        }
    }
    
    /**
     * 获取所有缓冲的音频数据并清空缓冲区
     * @return 合并后的音频数据，如果缓冲区为空则返回null
     */
    public byte[] getBufferedAudio() {
        synchronized (lock) {
            if (audioQueue.isEmpty()) {
                return null;
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(currentSize);
            
            try {
                while (!audioQueue.isEmpty()) {
                    byte[] chunk = audioQueue.poll();
                    if (chunk != null) {
                        outputStream.write(chunk);
                    }
                }
                currentSize = 0;
                return outputStream.toByteArray();
                
            } catch (IOException e) {
                Log.e(TAG, "Error writing audio data to buffer", e);
                return null;
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }
    
    /**
     * 获取部分缓冲音频数据
     * @param percentage 获取的百分比 (0.0 - 1.0)
     * @return 部分音频数据
     */
    public byte[] getPartialAudio(float percentage) {
        if (percentage <= 0 || percentage > 1.0f) {
            return getBufferedAudio();
        }
        
        synchronized (lock) {
            if (audioQueue.isEmpty()) {
                return null;
            }
            
            int targetSize = (int) (currentSize * percentage);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(targetSize);
            
            try {
                int writtenSize = 0;
                Queue<byte[]> tempQueue = new LinkedList<>(audioQueue);
                
                while (!tempQueue.isEmpty() && writtenSize < targetSize) {
                    byte[] chunk = tempQueue.poll();
                    if (chunk != null) {
                        int remainingSpace = targetSize - writtenSize;
                        int writeLength = Math.min(chunk.length, remainingSpace);
                        
                        outputStream.write(chunk, 0, writeLength);
                        writtenSize += writeLength;
                    }
                }
                
                return outputStream.toByteArray();
                
            } catch (IOException e) {
                Log.e(TAG, "Error writing partial audio data", e);
                return null;
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }
    
    /**
     * 获取当前缓冲区大小
     * @return 当前缓冲区字节数
     */
    public int getCurrentSize() {
        synchronized (lock) {
            return currentSize;
        }
    }
    
    /**
     * 获取最大缓冲区大小
     * @return 最大缓冲区字节数
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }
    
    /**
     * 检查缓冲区是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return audioQueue.isEmpty();
        }
    }
    
    /**
     * 获取缓冲区使用率
     * @return 使用率百分比 (0.0 - 1.0)
     */
    public float getUsagePercentage() {
        synchronized (lock) {
            return (float) currentSize / maxBufferSize;
        }
    }
    
    /**
     * 清空缓冲区
     */
    public void clear() {
        synchronized (lock) {
            audioQueue.clear();
            currentSize = 0;
        }
        Log.i(TAG, "Audio buffer cleared");
    }
    
    /**
     * 获取缓冲区中的音频块数量
     * @return 音频块数量
     */
    public int getChunkCount() {
        synchronized (lock) {
            return audioQueue.size();
        }
    }
    
    /**
     * 获取缓冲区状态信息
     * @return 状态信息字符串
     */
    public String getStatusInfo() {
        synchronized (lock) {
            return String.format("AudioBuffer: %d/%d bytes (%.1f%%), %d chunks", 
                               currentSize, maxBufferSize, getUsagePercentage() * 100, audioQueue.size());
        }
    }
}