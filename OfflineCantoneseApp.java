package com.example.offlinecantoneseasr;

import android.app.Application;
import android.util.Log;

// OfflineCantoneseApp.java
public class OfflineCantoneseApp extends Application {
    private static final String TAG = "OfflineCantoneseApp";

    private WhisperCantoneseRecognizer whisperRecognizer;
    private boolean isModelLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        preloadModel();
    }

    private void preloadModel() {
        new Thread(() -> {
            try {
                whisperRecognizer = new WhisperCantoneseRecognizer(this);
                isModelLoaded = true;
                Log.d(TAG, "Whisper模型预加载完成");
            } catch (Exception e) {
                Log.e(TAG, "模型预加载失败: " + e.getMessage());
                isModelLoaded = false;
            }
        }).start();
    }

    public WhisperCantoneseRecognizer getWhisperRecognizer() {
        return whisperRecognizer;
    }

    public boolean isModelLoaded() {
        return isModelLoaded;
    }
}
