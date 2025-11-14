package com.example.offlinecantoneseasr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

// AudioUtils.java
public class AudioUtils {
    private static final String TAG = "AudioUtils";

    public static short[] loadAudioFile(String filePath, int targetSampleRate) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("音频文件不存在: " + filePath);
        }

        // 根据文件扩展名选择加载方式
        String extension = getFileExtension(filePath);

        switch (extension.toLowerCase()) {
            case "wav":
                return loadWavFile(file, targetSampleRate);
            case "pcm":
                return loadPcmFile(file);
            default:
                throw new IOException("不支持的音频格式: " + extension);
        }
    }

    private static short[] loadWavFile(File file, int targetSampleRate) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

            // 读取WAV文件头
            byte[] header = new byte[44];
            dis.read(header);

            // 解析WAV头信息
            int sampleRate = bytesToInt(header, 24, 4);
            int bitsPerSample = bytesToInt(header, 34, 2);
            int dataSize = bytesToInt(header, 40, 4);

            if (bitsPerSample != 16) {
                throw new IOException("只支持16位PCM音频");
            }

            // 读取音频数据
            int numSamples = dataSize / 2;
            short[] audioData = new short[numSamples];

            for (int i = 0; i < numSamples; i++) {
                audioData[i] = dis.readShort();
            }

            // 如果需要重采样
            if (sampleRate != targetSampleRate) {
                audioData = resampleAudio(audioData, sampleRate, targetSampleRate);
            }

            return audioData;
        }
    }

    private static short[] loadPcmFile(File file) throws IOException {
        int fileSize = (int) file.length();
        int numSamples = fileSize / 2;

        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

            short[] audioData = new short[numSamples];
            for (int i = 0; i < numSamples; i++) {
                audioData[i] = dis.readShort();
            }

            return audioData;
        }
    }

    private static short[] resampleAudio(short[] input, int inputRate, int outputRate) {
        if (inputRate == outputRate) {
            return input;
        }

        double ratio = (double) inputRate / outputRate;
        int outputLength = (int) (input.length / ratio);
        short[] output = new short[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double index = i * ratio;
            int indexInt = (int) index;
            double fraction = index - indexInt;

            if (indexInt < input.length - 1) {
                output[i] = (short) (input[indexInt] * (1 - fraction) + input[indexInt + 1] * fraction);
            } else {
                output[i] = input[Math.min(indexInt, input.length - 1)];
            }
        }

        return output;
    }

    private static int bytesToInt(byte[] bytes, int offset, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value |= (bytes[offset + i] & 0xFF) << (8 * i);
        }
        return value;
    }

    private static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filePath.substring(lastDot + 1);
    }

    public static void saveAudioToFile(short[] audioData, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            for (short sample : audioData) {
                dos.writeShort(sample);
            }
        }
    }
}
