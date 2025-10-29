package com.example.cantonesevoicerecognition.native;

/**
 * JNI interface for Whisper speech recognition engine
 */
public class WhisperJNI {
    static {
        System.loadLibrary("cantonese_voice");
    }
    
    /**
     * Initialize the Whisper model
     * @param modelPath Path to the ONNX model file
     * @return true if initialization successful, false otherwise
     */
    public native boolean initializeModel(String modelPath);
    
    /**
     * Transcribe audio data to text
     * @param audioData Raw audio data in 16kHz, 16-bit, mono format
     * @return Transcribed text
     */
    public native String transcribeAudio(byte[] audioData);
    
    /**
     * Release the loaded model and free resources
     */
    public native void releaseModel();
}