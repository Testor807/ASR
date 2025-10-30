package com.example.cantonesevoicerecognition.engine;

import android.content.Context;
import android.util.Log;
import com.example.cantonesevoicerecognition.audio.AudioRecorderManager;
import com.example.cantonesevoicerecognition.audio.AudioStream;
import java.io.File;

/**
 * WhisperEngine工厂类
 * 提供便捷的引擎创建和配置方法
 */
public class WhisperEngineFactory {
    private static final String TAG = "WhisperEngineFactory";
    
    private static WhisperEngine singletonEngine;
    private static final Object lock = new Object();
    
    /**
     * 创建新的WhisperEngine实例
     * @return WhisperEngine实例
     */
    public static WhisperEngine createEngine() {
        return new WhisperEngine();
    }
    
    /**
     * 获取单例WhisperEngine实例
     * @return 单例WhisperEngine实例
     */
    public static WhisperEngine getSingletonEngine() {
        if (singletonEngine == null) {
            synchronized (lock) {
                if (singletonEngine == null) {
                    singletonEngine = new WhisperEngine();
                    Log.i(TAG, "Created singleton WhisperEngine instance");
                }
            }
        }
        return singletonEngine;
    }
    
    /**
     * 创建配置好的WhisperEngine实例
     * @param context 应用上下文
     * @param modelPath 模型文件路径
     * @return 配置好的WhisperEngine实例，如果配置失败返回null
     */
    public static WhisperEngine createConfiguredEngine(Context context, String modelPath) {
        WhisperEngine engine = createEngine();
        
        if (engine.initializeModel(modelPath)) {
            Log.i(TAG, "Created and configured WhisperEngine with model: " + modelPath);
            return engine;
        } else {
            Log.e(TAG, "Failed to configure WhisperEngine with model: " + modelPath);
            engine.releaseModel();
            return null;
        }
    }
    
    /**
     * 创建用于实时转录的完整配置
     * @param context 应用上下文
     * @param modelPath 模型文件路径
     * @return 实时转录配置对象
     */
    public static RealTimeTranscriptionConfig createRealTimeConfig(Context context, String modelPath) {
        WhisperEngine engine = createConfiguredEngine(context, modelPath);
        if (engine == null) {
            return null;
        }
        
        AudioRecorderManager recorderManager = new AudioRecorderManager(context);
        AudioStream audioStream = new AudioStream(recorderManager);
        
        return new RealTimeTranscriptionConfig(engine, audioStream, recorderManager);
    }
    
    /**
     * 创建离线模式的WhisperEngine实例
     * @param context 应用上下文
     * @return 配置好的离线WhisperEngine实例，如果离线模式不可用返回null
     */
    public static WhisperEngine createOfflineEngine(Context context) {
        return OfflineModeHelper.createOfflineEngine(context);
    }
    
    /**
     * 创建自动选择模式的WhisperEngine实例
     * 优先使用离线模式，如果不可用则尝试指定的模型路径
     * @param context 应用上下文
     * @param fallbackModelPath 备用模型路径
     * @return 配置好的WhisperEngine实例
     */
    public static WhisperEngine createAutoEngine(Context context, String fallbackModelPath) {
        // 首先尝试离线模式
        WhisperEngine engine = createOfflineEngine(context);
        if (engine != null) {
            Log.i(TAG, "Created WhisperEngine using offline mode");
            return engine;
        }
        
        // 离线模式不可用，尝试备用模型路径
        if (fallbackModelPath != null && validateModelFile(fallbackModelPath)) {
            engine = createConfiguredEngine(context, fallbackModelPath);
            if (engine != null) {
                Log.i(TAG, "Created WhisperEngine using fallback model: " + fallbackModelPath);
                return engine;
            }
        }
        
        Log.w(TAG, "Failed to create WhisperEngine with any available method");
        return null;
    }
    
    /**
     * 创建离线模式的实时转录配置
     * @param context 应用上下文
     * @return 离线实时转录配置对象
     */
    public static RealTimeTranscriptionConfig createOfflineRealTimeConfig(Context context) {
        WhisperEngine engine = createOfflineEngine(context);
        if (engine == null) {
            return null;
        }
        
        AudioRecorderManager recorderManager = new AudioRecorderManager(context);
        AudioStream audioStream = new AudioStream(recorderManager);
        
        return new RealTimeTranscriptionConfig(engine, audioStream, recorderManager);
    }
    
    /**
     * 验证模型文件
     * @param modelPath 模型文件路径
     * @return 是否有效
     */
    public static boolean validateModelFile(String modelPath) {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            return false;
        }
        
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            Log.w(TAG, "Model file does not exist: " + modelPath);
            return false;
        }
        
        if (!modelFile.canRead()) {
            Log.w(TAG, "Cannot read model file: " + modelPath);
            return false;
        }
        
        // 检查文件大小（模型文件应该大于1MB）
        if (modelFile.length() < 1024 * 1024) {
            Log.w(TAG, "Model file seems too small: " + modelFile.length() + " bytes");
            return false;
        }
        
        // 检查文件扩展名
        String fileName = modelFile.getName().toLowerCase();
        if (!fileName.endsWith(".onnx") && !fileName.endsWith(".bin")) {
            Log.w(TAG, "Unexpected model file extension: " + fileName);
            // 不返回false，因为可能有其他格式的模型文件
        }
        
        return true;
    }
    
    /**
     * 释放单例引擎
     */
    public static void releaseSingletonEngine() {
        synchronized (lock) {
            if (singletonEngine != null) {
                singletonEngine.releaseModel();
                singletonEngine = null;
                Log.i(TAG, "Released singleton WhisperEngine instance");
            }
        }
    }
    
    /**
     * 实时转录配置类
     */
    public static class RealTimeTranscriptionConfig {
        private final WhisperEngine engine;
        private final AudioStream audioStream;
        private final AudioRecorderManager recorderManager;
        
        public RealTimeTranscriptionConfig(WhisperEngine engine, AudioStream audioStream, 
                                         AudioRecorderManager recorderManager) {
            this.engine = engine;
            this.audioStream = audioStream;
            this.recorderManager = recorderManager;
        }
        
        public WhisperEngine getEngine() {
            return engine;
        }
        
        public AudioStream getAudioStream() {
            return audioStream;
        }
        
        public AudioRecorderManager getRecorderManager() {
            return recorderManager;
        }
        
        /**
         * 开始实时转录
         * @param callback 转录回调
         * @return 是否成功开始
         */
        public boolean startRealTimeTranscription(TranscriptionCallback callback) {
            if (engine == null || audioStream == null) {
                return false;
            }
            
            try {
                engine.startRealTimeTranscription(audioStream, callback);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to start real-time transcription", e);
                return false;
            }
        }
        
        /**
         * 停止实时转录
         */
        public void stopRealTimeTranscription() {
            if (engine != null) {
                engine.stopRealTimeTranscription();
            }
            if (audioStream != null) {
                audioStream.stop();
            }
        }
        
        /**
         * 释放所有资源
         */
        public void release() {
            stopRealTimeTranscription();
            
            if (audioStream != null) {
                audioStream.release();
            }
            
            if (engine != null) {
                engine.releaseModel();
            }
        }
        
        /**
         * 获取配置状态信息
         * @return 状态信息
         */
        public String getStatusInfo() {
            StringBuilder sb = new StringBuilder();
            sb.append("RealTimeConfig Status:\n");
            
            if (engine != null) {
                sb.append("- Engine: ").append(engine.getEngineStatus()).append("\n");
            } else {
                sb.append("- Engine: null\n");
            }
            
            if (audioStream != null) {
                sb.append("- AudioStream: Active=").append(audioStream.isActive())
                  .append(", Duration=").append(audioStream.getDuration()).append("ms\n");
            } else {
                sb.append("- AudioStream: null\n");
            }
            
            if (recorderManager != null) {
                sb.append("- Recorder: Recording=").append(recorderManager.isRecording())
                  .append(", Paused=").append(recorderManager.isPaused());
            } else {
                sb.append("- Recorder: null");
            }
            
            return sb.toString();
        }
    }
}