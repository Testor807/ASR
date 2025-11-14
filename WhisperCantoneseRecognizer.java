package com.example.offlinecantoneseasr;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.util.HashMap;
import java.util.Map;

// WhisperCantoneseRecognizer.java
public class WhisperCantoneseRecognizer {
    private static final String TAG = "OfflineWhisperASR";

    private static final String MODEL_PATH = "models/whisper-small-cantonese.tflite";
    private static final String TOKENIZER_PATH = "models/tokenizer.json";

    private Interpreter tflite;
    private CantoneseTokenizer tokenizer;
    private boolean isInitialized = false;

    // 音频处理参数
    private static final int SAMPLE_RATE = 16000;
    private static final int N_FFT = 400;
    private static final int HOP_LENGTH = 160;
    private static final int N_MELS = 80;
    private static final int MAX_AUDIO_LENGTH = 30 * SAMPLE_RATE; // 30秒

    public WhisperCantoneseRecognizer(Context context) throws Exception {
        initializeModel(context);
    }

    private void initializeModel(Context context) throws Exception {
        Log.d(TAG, "开始初始化离线Whisper模型...");

        // 加载TensorFlow Lite模型
        tflite = TensorFlowLiteHelper.loadModel(context, MODEL_PATH);
        if (tflite == null) {
            throw new Exception("无法加载TFLite模型");
        }

        // 加载tokenizer
        String tokenizerJson = TensorFlowLiteHelper.loadTokenizer(context, TOKENIZER_PATH);
        if (tokenizerJson == null) {
            throw new Exception("无法加载tokenizer");
        }

        tokenizer = new CantoneseTokenizer(tokenizerJson);

        // 预热模型
        warmUpModel();

        isInitialized = true;
        Log.d(TAG, "离线Whisper模型初始化完成");
    }

    private void warmUpModel() {
        try {
            // 创建空的输入数据进行预热
            float[][][] dummyInput = new float[1][3000][N_MELS];
            Map<Integer, Object> outputMap = new HashMap<>();
            float[][][] dummyOutput = new float[1][448][51865];
            outputMap.put(0, dummyOutput);

            tflite.runForMultipleInputsOutputs(new Object[]{dummyInput}, outputMap);
            Log.d(TAG, "模型预热完成");
        } catch (Exception e) {
            Log.w(TAG, "模型预热失败: " + e.getMessage());
        }
    }

    public String recognizeAudio(short[] audioData) {
        if (!isInitialized) {
            return "模型未初始化";
        }

        try {
            Log.d(TAG, "开始音频识别，数据长度: " + audioData.length);

            // 预处理音频
            float[] floatAudio = preprocessAudio(audioData);

            // 计算Mel频谱
            float[][][] melSpectrogram = computeMelSpectrogram(floatAudio);

            // 运行推理
            String result = runInference(melSpectrogram);

            Log.d(TAG, "识别完成: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "识别过程出错: " + e.getMessage());
            return "识别失败: " + e.getMessage();
        }
    }

    public String recognizeAudioFile(String filePath) {
        try {
            short[] audioData = AudioUtils.loadAudioFile(filePath, SAMPLE_RATE);
            return recognizeAudio(audioData);
        } catch (Exception e) {
            Log.e(TAG, "读取音频文件失败: " + e.getMessage());
            return "读取音频文件失败";
        }
    }

    private float[] preprocessAudio(short[] audioData) {
        float[] floatAudio = new float[audioData.length];

        // 转换为float并归一化
        for (int i = 0; i < audioData.length; i++) {
            floatAudio[i] = audioData[i] / 32768.0f;
        }

        return floatAudio;
    }

    private float[][][] computeMelSpectrogram(float[] audioData) {
        // 实现简化的Mel频谱计算
        // 注意：这里需要完整的DSP实现，以下为简化版本

        int nFrames = Math.min((audioData.length - N_FFT) / HOP_LENGTH + 1, 3000);
        float[][][] mel = new float[1][nFrames][N_MELS];

        // 简化的频谱计算（实际项目需要完整实现）
        for (int i = 0; i < nFrames; i++) {
            int start = i * HOP_LENGTH;
            float[] frame = new float[N_FFT];
            System.arraycopy(audioData, start, frame, 0, Math.min(N_FFT, audioData.length - start));

            // 应用窗函数
            applyHannWindow(frame);

            // 计算功率谱
            float[] powerSpectrum = computePowerSpectrum(frame);

            // 应用Mel滤波器组
            float[] melSpectrum = applyMelFilterBank(powerSpectrum);

            // 对数压缩
            for (int j = 0; j < N_MELS; j++) {
                mel[0][i][j] = (float) Math.log(melSpectrum[j] + 1e-6);
            }
        }

        return mel;
    }

    private void applyHannWindow(float[] frame) {
        for (int i = 0; i < frame.length; i++) {
            double multiplier = 0.5 * (1 - Math.cos(2 * Math.PI * i / (frame.length - 1)));
            frame[i] *= multiplier;
        }
    }

    private float[] computePowerSpectrum(float[] frame) {
        // 简化的FFT计算（实际应使用完整FFT实现）
        int n = frame.length;
        float[] spectrum = new float[n / 2 + 1];

        // 这里应该是完整的FFT实现
        for (int i = 0; i < spectrum.length; i++) {
            spectrum[i] = frame[i] * frame[i]; // 简化处理
        }

        return spectrum;
    }

    private float[] applyMelFilterBank(float[] powerSpectrum) {
        float[] melSpectrum = new float[N_MELS];

        // 简化的Mel滤波器组（实际需要预计算的滤波器组）
        for (int i = 0; i < N_MELS; i++) {
            melSpectrum[i] = powerSpectrum[Math.min(i, powerSpectrum.length - 1)];
        }

        return melSpectrum;
    }

    private String runInference(float[][][] melSpectrogram) {
        try {
            // 准备输入输出
            Object[] inputs = {melSpectrogram};
            Map<Integer, Object> outputs = new HashMap<>();

            float[][][] outputTokens = new float[1][448][tokenizer.getVocabSize()];
            outputs.put(0, outputTokens);

            // 运行推理
            long startTime = System.currentTimeMillis();
            tflite.runForMultipleInputsOutputs(inputs, outputs);
            long endTime = System.currentTimeMillis();

            Log.d(TAG, "推理耗时: " + (endTime - startTime) + "ms");

            // 解码输出
            return tokenizer.decode(outputTokens[0]);

        } catch (Exception e) {
            Log.e(TAG, "推理失败: " + e.getMessage());
            return "推理错误";
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
