package com.example.cantonesevoicerecognition.engine;

import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;

/**
 * Callback interface for transcription operations
 */
public interface TranscriptionCallback {
    /**
     * Called when transcription starts
     */
    void onTranscriptionStarted();
    
    /**
     * Called when partial transcription result is available (for real-time mode)
     * @param partialText Partial transcription text
     */
    void onPartialResult(String partialText);
    
    /**
     * Called when transcription is completed
     * @param result Complete transcription result
     */
    void onTranscriptionCompleted(TranscriptionResult result);
    
    /**
     * Called when transcription error occurs
     * @param error Error information
     */
    void onTranscriptionError(TranscriptionError error);
}