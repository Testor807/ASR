package com.example.offlinecantoneseasr;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

// MainActivity.java
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AudioRecorderManager audioRecorder;
    private WhisperCantoneseRecognizer whisperRecognizer;
    private PermissionManager permissionManager;
    private CommandParser commandParser;

    private Button recordButton;
    private Button processButton;
    private TextView statusTextView;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private TextView timerTextView;

    private short[] currentAudioData;
    private File currentAudioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeComponents();
        checkPermissions();
    }

    private void initializeViews() {
        recordButton = findViewById(R.id.record_button);
        processButton = findViewById(R.id.process_button);
        statusTextView = findViewById(R.id.status_text);
        resultTextView = findViewById(R.id.result_text);
        progressBar = findViewById(R.id.progress_bar);
        timerTextView = findViewById(R.id.timer_text);

        recordButton.setOnClickListener(v -> toggleRecording());
        processButton.setOnClickListener(v -> processRecording());

        updateUIState(false, false);
    }

    private void initializeComponents() {
        audioRecorder = new AudioRecorderManager(this);
        permissionManager = new PermissionManager(this);
        commandParser = new CommandParser();

        // 获取预加载的识别器
        OfflineCantoneseApp app = (OfflineCantoneseApp) getApplication();
        whisperRecognizer = app.getWhisperRecognizer();

        // 检查模型状态
        checkModelStatus();
    }

    private void checkModelStatus() {
        OfflineCantoneseApp app = (OfflineCantoneseApp) getApplication();
        if (app.isModelLoaded()) {
            statusTextView.setText("离线语音识别就绪");
            recordButton.setEnabled(true);
        } else {
            statusTextView.setText("模型加载中...");
            recordButton.setEnabled(false);

            // 延迟检查模型状态
            new Handler().postDelayed(this::checkModelStatus, 1000);
        }
    }

    private void checkPermissions() {
        if (!permissionManager.hasAudioPermission()) {
            permissionManager.requestAudioPermission();
        }
    }

    private void toggleRecording() {
        if (audioRecorder.isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (!permissionManager.hasAudioPermission()) {
            permissionManager.requestAudioPermission();
            return;
        }

        audioRecorder.startRecording();
        updateRecordingUI(true);
        startTimer();
        statusTextView.setText("录音中...");

    }

    private void stopRecording() {
        audioRecorder.stopRecording();
        updateRecordingUI(false);
        stopTimer();
        statusTextView.setText("录音完成");

        currentAudioData = audioRecorder.getRecordedAudio();
        audioRecorder.saveRecordingToFile();
        currentAudioFile = audioRecorder.getOutputFile();

        processButton.setEnabled(true);
        statusTextView.setText("录音完成，点击处理按钮进行识别");

    }

    private void processRecording() {
        if (currentAudioData == null || currentAudioData.length == 0) {
            statusTextView.setText("没有可处理的录音数据");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        processButton.setEnabled(false);
        statusTextView.setText("正在识别粤语...");

        new Thread(() -> {
            final String result = whisperRecognizer.recognizeAudio(currentAudioData);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                resultTextView.setText(result);
                statusTextView.setText("识别完成");

                // 解析并执行指令
                parseAndExecuteCommand(result);
            });
        }).start();
    }

    private void parseAndExecuteCommand(String text) {
        if (text != null && !text.isEmpty() && !text.startsWith("识别失败")) {
            commandParser.parseAndExecute(text);

            // 语音反馈（可选）
            speakResult("已识别: " + text);
        }
    }

    private void speakResult(String text) {
        // 简单的文本转语音反馈
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void startTimer() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (audioRecorder.isRecording()) {
                    seconds++;
                    timerTextView.setText(String.format(" %02d:%02d", seconds / 60, seconds % 60));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void stopTimer() {
        timerTextView.setText(" 00:00");
    }

    private void updateRecordingUI(boolean recording) {
        if (recording) {
            recordButton.setText("停止录音");
            recordButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
            timerTextView.setVisibility(View.VISIBLE);
        } else {
            recordButton.setText("开始录音");
            recordButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null));
            timerTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void updateUIState(boolean modelReady, boolean hasRecording) {
        recordButton.setEnabled(modelReady);
        processButton.setEnabled(hasRecording);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (whisperRecognizer != null) {
            whisperRecognizer.close();
        }
    }
}