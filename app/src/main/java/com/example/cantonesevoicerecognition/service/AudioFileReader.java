package com.example.cantonesevoicerecognition.service;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import com.example.cantonesevoicerecognition.data.model.AudioData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音頻文件讀取器
 * 支持多種音頻格式的讀取和轉換
 */
public class AudioFileReader {
    private static final String TAG = "AudioFileReader";
    
    // 支持的音頻格式
    private static final String[] SUPPORTED_FORMATS = {
        "audio/mpeg", "audio/mp4", "audio/wav", "audio/3gpp", 
        "audio/amr", "audio/flac", "audio/ogg"
    };
    
    /**
     * 讀取音頻文件
     * @param filePath 文件路徑
     * @return AudioData對象，失敗時返回null
     */
    public static AudioData readAudioFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            Log.e(TAG, "Invalid file path");
            return null;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + filePath);
            return null;
        }
        
        if (!file.canRead()) {
            Log.e(TAG, "Cannot read file: " + filePath);
            return null;
        }
        
        Log.i(TAG, "Reading audio file: " + filePath);
        
        // 獲取文件信息
        AudioFileInfo fileInfo = getAudioFileInfo(filePath);
        if (fileInfo == null) {
            Log.e(TAG, "Failed to get audio file info");
            return null;
        }
        
        Log.i(TAG, "Audio file info: " + fileInfo);
        
        // 根據文件格式選擇讀取方法
        if (isWavFile(filePath)) {
            return readWavFile(filePath);
        } else {
            return readAudioFileWithMediaExtractor(filePath, fileInfo);
        }
    }
    
    /**
     * 使用MediaExtractor讀取音頻文件
     */
    private static AudioData readAudioFileWithMediaExtractor(String filePath, AudioFileInfo fileInfo) {
        MediaExtractor extractor = new MediaExtractor();
        
        try {
            extractor.setDataSource(filePath);
            
            // 找到音頻軌道
            int audioTrackIndex = -1;
            MediaFormat format = null;
            
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    format = trackFormat;
                    break;
                }
            }
            
            if (audioTrackIndex == -1) {
                Log.e(TAG, "No audio track found in file");
                return null;
            }
            
            extractor.selectTrack(audioTrackIndex);
            
            // 獲取音頻參數
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? 
                           format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 16000;
            int channelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? 
                             format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            
            Log.i(TAG, "Audio format - Sample rate: " + sampleRate + ", Channels: " + channelCount);
            
            // 讀取音頻數據
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1MB緩衝
            ByteBuffer audioDataBuffer = ByteBuffer.allocate(10 * 1024 * 1024); // 10MB最大
            
            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                
                // 檢查緩衝區空間
                if (audioDataBuffer.remaining() < sampleSize) {
                    Log.w(TAG, "Audio data too large, truncating");
                    break;
                }
                
                audioDataBuffer.put(buffer.array(), 0, sampleSize);
                extractor.advance();
                buffer.clear();
            }
            
            // 創建AudioData對象
            byte[] audioBytes = new byte[audioDataBuffer.position()];
            audioDataBuffer.rewind();
            audioDataBuffer.get(audioBytes);
            
            AudioData audioData = new AudioData(audioBytes, sampleRate, channelCount, 16);
            
            Log.i(TAG, "Successfully read audio file: " + audioBytes.length + " bytes");
            return audioData;
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading audio file with MediaExtractor", e);
            return null;
        } finally {
            extractor.release();
        }
    }
    
    /**
     * 讀取WAV文件
     */
    private static AudioData readWavFile(String filePath) {
        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(filePath);
            
            // 讀取WAV文件頭
            byte[] header = new byte[44];
            int bytesRead = fis.read(header);
            
            if (bytesRead < 44) {
                Log.e(TAG, "Invalid WAV file: header too short");
                return null;
            }
            
            // 解析WAV頭部信息
            WavHeader wavHeader = parseWavHeader(header);
            if (wavHeader == null) {
                Log.e(TAG, "Failed to parse WAV header");
                return null;
            }
            
            Log.i(TAG, "WAV file info: " + wavHeader);
            
            // 讀取音頻數據
            int dataSize = wavHeader.dataSize;
            if (dataSize <= 0 || dataSize > 50 * 1024 * 1024) { // 50MB限制
                Log.e(TAG, "Invalid WAV data size: " + dataSize);
                return null;
            }
            
            byte[] audioData = new byte[dataSize];
            int totalRead = 0;
            
            while (totalRead < dataSize) {
                int read = fis.read(audioData, totalRead, dataSize - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            
            if (totalRead != dataSize) {
                Log.w(TAG, "Expected " + dataSize + " bytes, but read " + totalRead + " bytes");
            }
            
            // 轉換為目標格式（如果需要）
            byte[] processedData = convertAudioFormat(audioData, wavHeader);
            
            AudioData result = new AudioData(processedData, wavHeader.sampleRate, 
                                           wavHeader.channels, wavHeader.bitsPerSample);
            
            Log.i(TAG, "Successfully read WAV file: " + processedData.length + " bytes");
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading WAV file", e);
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file stream", e);
                }
            }
        }
    }
    
    /**
     * 解析WAV文件頭
     */
    private static WavHeader parseWavHeader(byte[] header) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(header);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // 檢查RIFF標識
            byte[] riff = new byte[4];
            buffer.get(riff);
            if (!new String(riff).equals("RIFF")) {
                Log.e(TAG, "Not a valid RIFF file");
                return null;
            }
            
            int fileSize = buffer.getInt();
            
            // 檢查WAVE標識
            byte[] wave = new byte[4];
            buffer.get(wave);
            if (!new String(wave).equals("WAVE")) {
                Log.e(TAG, "Not a valid WAVE file");
                return null;
            }
            
            // 檢查fmt標識
            byte[] fmt = new byte[4];
            buffer.get(fmt);
            if (!new String(fmt).equals("fmt ")) {
                Log.e(TAG, "Invalid fmt chunk");
                return null;
            }
            
            int fmtSize = buffer.getInt();
            short audioFormat = buffer.getShort();
            short channels = buffer.getShort();
            int sampleRate = buffer.getInt();
            int byteRate = buffer.getInt();
            short blockAlign = buffer.getShort();
            short bitsPerSample = buffer.getShort();
            
            // 跳過可能的額外fmt數據
            if (fmtSize > 16) {
                buffer.position(buffer.position() + (fmtSize - 16));
            }
            
            // 尋找data chunk
            while (buffer.remaining() >= 8) {
                byte[] chunkId = new byte[4];
                buffer.get(chunkId);
                int chunkSize = buffer.getInt();
                
                if (new String(chunkId).equals("data")) {
                    WavHeader wavHeader = new WavHeader();
                    wavHeader.audioFormat = audioFormat;
                    wavHeader.channels = channels;
                    wavHeader.sampleRate = sampleRate;
                    wavHeader.bitsPerSample = bitsPerSample;
                    wavHeader.dataSize = chunkSize;
                    return wavHeader;
                }
                
                // 跳過其他chunk
                if (buffer.remaining() >= chunkSize) {
                    buffer.position(buffer.position() + chunkSize);
                }
            }
            
            Log.e(TAG, "Data chunk not found");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WAV header", e);
            return null;
        }
    }
    
    /**
     * 轉換音頻格式
     */
    private static byte[] convertAudioFormat(byte[] audioData, WavHeader header) {
        // 如果已經是目標格式，直接返回
        if (header.sampleRate == 16000 && header.channels == 1 && header.bitsPerSample == 16) {
            return audioData;
        }
        
        // 這裡可以實現更複雜的格式轉換
        // 目前只做簡單的處理
        
        if (header.channels == 2 && header.bitsPerSample == 16) {
            // 立體聲轉單聲道
            return stereoToMono16Bit(audioData);
        }
        
        return audioData;
    }
    
    /**
     * 立體聲轉單聲道（16位）
     */
    private static byte[] stereoToMono16Bit(byte[] stereoData) {
        byte[] monoData = new byte[stereoData.length / 2];
        
        for (int i = 0; i < monoData.length; i += 2) {
            int stereoIndex = i * 2;
            
            if (stereoIndex + 3 < stereoData.length) {
                // 讀取左右聲道樣本
                short left = (short) ((stereoData[stereoIndex + 1] << 8) | (stereoData[stereoIndex] & 0xFF));
                short right = (short) ((stereoData[stereoIndex + 3] << 8) | (stereoData[stereoIndex + 2] & 0xFF));
                
                // 平均值
                short mono = (short) ((left + right) / 2);
                
                // 寫入單聲道數據
                monoData[i] = (byte) (mono & 0xFF);
                monoData[i + 1] = (byte) ((mono >> 8) & 0xFF);
            }
        }
        
        return monoData;
    }
    
    /**
     * 獲取音頻文件信息
     */
    private static AudioFileInfo getAudioFileInfo(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        
        try {
            retriever.setDataSource(filePath);
            
            AudioFileInfo info = new AudioFileInfo();
            info.filePath = filePath;
            info.fileSize = new File(filePath).length();
            
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            info.duration = duration != null ? Long.parseLong(duration) : 0;
            
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            info.mimeType = mimeType != null ? mimeType : "unknown";
            
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            info.bitrate = bitrate != null ? Integer.parseInt(bitrate) : 0;
            
            return info;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio file info", e);
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
    }
    
    /**
     * 檢查是否為WAV文件
     */
    private static boolean isWavFile(String filePath) {
        return filePath.toLowerCase().endsWith(".wav");
    }
    
    /**
     * 檢查文件格式是否支持
     */
    public static boolean isSupportedFormat(String filePath) {
        AudioFileInfo info = getAudioFileInfo(filePath);
        if (info == null) {
            return false;
        }
        
        for (String format : SUPPORTED_FORMATS) {
            if (format.equals(info.mimeType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * WAV文件頭信息
     */
    private static class WavHeader {
        short audioFormat;
        short channels;
        int sampleRate;
        short bitsPerSample;
        int dataSize;
        
        @Override
        public String toString() {
            return String.format("WavHeader{format=%d, channels=%d, sampleRate=%d, bits=%d, dataSize=%d}",
                               audioFormat, channels, sampleRate, bitsPerSample, dataSize);
        }
    }
    
    /**
     * 音頻文件信息
     */
    private static class AudioFileInfo {
        String filePath;
        long fileSize;
        long duration; // 毫秒
        String mimeType;
        int bitrate;
        
        @Override
        public String toString() {
            return String.format("AudioFileInfo{path='%s', size=%d, duration=%dms, mime='%s', bitrate=%d}",
                               filePath, fileSize, duration, mimeType, bitrate);
        }
    }
}