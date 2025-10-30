package com.example.cantonesevoicerecognition;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cantonesevoicerecognition.engine.TranscriptionCallback;
import com.example.cantonesevoicerecognition.engine.TranscriptionError;
import com.example.cantonesevoicerecognition.engine.WhisperEngineFactory;
import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;
import com.example.cantonesevoicerecognition.service.TranscriptionService;
import com.example.cantonesevoicerecognition.utils.Constants;
import com.example.cantonesevoicerecognition.utils.PermissionUtils;
import com.example.cantonesevoicerecognition.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/**
 * 主界面Activity - 提供录音控制和转录结果显示功能
 */
public class MainActivity extends AppCompatActivity implements TranscriptionCallback, PermissionManager.PermissionManagerCallback {
    
    private static final String TAG = "MainActivity";
    
    // UI组件
    private FloatingActionButton fabRecord;
    private MaterialButton btnRealtimeMode;
    private MaterialButton btnFileMode;
    private MaterialButton btnHistory;
    private ImageButton btnSettings;
    private TextView tvTranscriptionResult;
    private TextView tvRealtimeStatus;
    private TextView tvRecordingStatus;
    private TextView tvRecordingTime;
    private TextView tvOfflineStatus;
    private TextView tvEngineStatus;
    private LinearLayout llRealtimeStatus;
    private LinearLayout llRecordingIndicator;
    private ProgressBar progressRealtime;
    
    // 服务和引擎
    private TranscriptionService transcriptionService;
    private boolean isServiceBound = false;
    private WhisperEngineFactory engineFactory;
    
    // 权限管理
    private PermissionManager permissionManager;
    
    // 状态管理
    private boolean isRecording = false;
    private boolean isRealTimeMode = true;
    private long recordingStartTime = 0;
    private Handler uiHandler;
    private Runnable timeUpdateRunnable;
    
    // 广播接收器
    private BroadcastReceiver transcriptionReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        initializeViews();
        
        // 初始化处理器
        uiHandler = new Handler(Looper.getMainLooper());
        
        // 初始化引擎工厂
        engineFactory = new WhisperEngineFactory();
        
        // 初始化权限管理器
        permissionManager = new PermissionManager(this);
        permissionManager.setCallback(this);
        
        // 设置点击监听器
        setupClickListeners();
        
        // 检查权限
        permissionManager.checkAndRequestPermissions();
        
        // 初始化广播接收器
        setupBroadcastReceiver();
        
        // 绑定转录服务
        bindTranscriptionService();
        
        // 更新UI状态
        updateUIState();
        
        Log.d(TAG, "MainActivity created successfully");
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeViews() {
        fabRecord = findViewById(R.id.fab_record);
        btnRealtimeMode = findViewById(R.id.btn_realtime_mode);
        btnFileMode = findViewById(R.id.btn_file_mode);
        btnHistory = findViewById(R.id.btn_history);
        btnSettings = findViewById(R.id.btn_settings);
        tvTranscriptionResult = findViewById(R.id.tv_transcription_result);
        tvRealtimeStatus = findViewById(R.id.tv_realtime_status);
        tvRecordingStatus = findViewById(R.id.tv_recording_status);
        tvRecordingTime = findViewById(R.id.tv_recording_time);
        tvOfflineStatus = findViewById(R.id.tv_offline_status);
        tvEngineStatus = findViewById(R.id.tv_engine_status);
        llRealtimeStatus = findViewById(R.id.ll_realtime_status);
        llRecordingIndicator = findViewById(R.id.ll_recording_indicator);
        progressRealtime = findViewById(R.id.progress_realtime);
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 录音按钮
        fabRecord.setOnClickListener(v -> toggleRecording());
        
        // 实时模式按钮
        btnRealtimeMode.setOnClickListener(v -> switchToRealtimeMode());
        
        // 文件模式按钮
        btnFileMode.setOnClickListener(v -> switchToFileMode());
        
        // 历史记录按钮
        btnHistory.setOnClickListener(v -> openHistory());
        
        // 设置按钮
        btnSettings.setOnClickListener(v -> openSettings());
    }
    

    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // 使用权限管理器处理结果
        if (permissionManager != null) {
            permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
        }
    }
    

    
    /**
     * 设置广播接收器
     */
    private void setupBroadcastReceiver() {
        transcriptionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.example.cantonesevoicerecognition.TRANSCRIPTION_RESULT".equals(action)) {
                    String text = intent.getStringExtra("text");
                    boolean isComplete = intent.getBooleanExtra("isComplete", false);
                    updateTranscriptionResult(text, isComplete);
                } else if ("com.example.cantonesevoicerecognition.TRANSCRIPTION_ERROR".equals(action)) {
                    String error = intent.getStringExtra("error");
                    Toast.makeText(MainActivity.this, "转录错误: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.cantonesevoicerecognition.TRANSCRIPTION_RESULT");
        filter.addAction("com.example.cantonesevoicerecognition.TRANSCRIPTION_ERROR");
        registerReceiver(transcriptionReceiver, filter);
    }
    
    /**
     * 绑定转录服务
     */
    private void bindTranscriptionService() {
        Intent serviceIntent = new Intent(this, TranscriptionService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * 服务连接回调
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TranscriptionService.TranscriptionBinder binder = (TranscriptionService.TranscriptionBinder) service;
            transcriptionService = binder.getService();
            isServiceBound = true;
            transcriptionService.setTranscriptionCallback(MainActivity.this);
            updateEngineStatus();
            Log.d(TAG, "TranscriptionService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            transcriptionService = null;
            isServiceBound = false;
            Log.d(TAG, "TranscriptionService disconnected");
        }
    };
    
    /**
     * 切换录音状态
     */
    private void toggleRecording() {
        if (!PermissionUtils.hasAllRequiredPermissions(this)) {
            permissionManager.checkAndRequestPermissions();
            return;
        }
        
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }
    
    /**
     * 开始录音
     */
    private void startRecording() {
        if (!isServiceBound || transcriptionService == null) {
            Toast.makeText(this, "服务未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            if (isRealTimeMode) {
                transcriptionService.startRealTimeTranscription();
            }
            
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            updateRecordingUI(true);
            startTimeUpdate();
            
            Log.d(TAG, "Recording started in " + (isRealTimeMode ? "real-time" : "file") + " mode");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            Toast.makeText(this, "录音启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 停止录音
     */
    private void stopRecording() {
        if (!isServiceBound || transcriptionService == null) {
            return;
        }
        
        try {
            if (isRealTimeMode) {
                transcriptionService.stopRealTimeTranscription();
            }
            
            isRecording = false;
            updateRecordingUI(false);
            stopTimeUpdate();
            
            Log.d(TAG, "Recording stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
            Toast.makeText(this, "录音停止失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 切换到实时模式
     */
    private void switchToRealtimeMode() {
        if (isRecording) {
            Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isRealTimeMode = true;
        updateModeButtons();
        Log.d(TAG, "Switched to real-time mode");
    }
    
    /**
     * 切换到文件模式
     */
    private void switchToFileMode() {
        if (isRecording) {
            Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isRealTimeMode = false;
        updateModeButtons();
        Log.d(TAG, "Switched to file mode");
    }
    
    /**
     * 打开历史记录
     */
    private void openHistory() {
        Intent historyIntent = new Intent(this, com.example.cantonesevoicerecognition.ui.history.HistoryActivity.class);
        startActivity(historyIntent);
    }
    
    /**
     * 打开设置
     */
    private void openSettings() {
        Intent settingsIntent = new Intent(this, com.example.cantonesevoicerecognition.ui.settings.SettingsActivity.class);
        startActivity(settingsIntent);
    }
    
    /**
     * 更新录音UI状态
     */
    private void updateRecordingUI(boolean recording) {
        if (recording) {
            fabRecord.setImageResource(android.R.drawable.ic_media_pause);
            fabRecord.setContentDescription("停止录音");
            llRecordingIndicator.setVisibility(View.VISIBLE);
            
            if (isRealTimeMode) {
                llRealtimeStatus.setVisibility(View.VISIBLE);
                tvRealtimeStatus.setText("实时转录中...");
            }
        } else {
            fabRecord.setImageResource(R.drawable.ic_mic);
            fabRecord.setContentDescription("开始录音");
            llRecordingIndicator.setVisibility(View.GONE);
            llRealtimeStatus.setVisibility(View.GONE);
            tvRecordingTime.setText("00:00");
        }
    }
    
    /**
     * 更新模式按钮状态
     */
    private void updateModeButtons() {
        if (isRealTimeMode) {
            btnRealtimeMode.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color));
            btnRealtimeMode.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnFileMode.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btnFileMode.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        } else {
            btnFileMode.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color));
            btnFileMode.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnRealtimeMode.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btnRealtimeMode.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }
    
    /**
     * 开始时间更新
     */
    private void startTimeUpdate() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    updateRecordingTime(elapsed);
                    uiHandler.postDelayed(this, 1000);
                }
            }
        };
        uiHandler.post(timeUpdateRunnable);
    }
    
    /**
     * 停止时间更新
     */
    private void stopTimeUpdate() {
        if (timeUpdateRunnable != null) {
            uiHandler.removeCallbacks(timeUpdateRunnable);
            timeUpdateRunnable = null;
        }
    }
    
    /**
     * 更新录音时间显示
     */
    private void updateRecordingTime(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        
        String timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvRecordingTime.setText(timeText);
    }
    
    /**
     * 更新转录结果显示
     */
    private void updateTranscriptionResult(String text, boolean isComplete) {
        runOnUiThread(() -> {
            if (text != null && !text.trim().isEmpty()) {
                String currentText = tvTranscriptionResult.getText().toString();
                if (isComplete) {
                    // 完整结果，添加时间戳
                    String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    String newText = currentText + "\n[" + timestamp + "] " + text;
                    tvTranscriptionResult.setText(newText);
                } else {
                    // 部分结果，实时更新
                    tvTranscriptionResult.setText(text);
                }
                
                // 滚动到底部
                tvTranscriptionResult.post(() -> {
                    int scrollAmount = tvTranscriptionResult.getLayout().getLineTop(
                            tvTranscriptionResult.getLineCount()) - tvTranscriptionResult.getHeight();
                    if (scrollAmount > 0) {
                        tvTranscriptionResult.scrollTo(0, scrollAmount);
                    } else {
                        tvTranscriptionResult.scrollTo(0, 0);
                    }
                });
            }
        });
    }
    
    /**
     * 更新引擎状态
     */
    private void updateEngineStatus() {
        if (engineFactory != null) {
            String status = engineFactory.getStatusInfo();
            tvEngineStatus.setText("引擎: " + status);
        }
    }
    
    /**
     * 更新UI状态
     */
    private void updateUIState() {
        updateModeButtons();
        updateEngineStatus();
        
        // 更新离线状态
        tvOfflineStatus.setText("离线模式: 未启用");
        
        // 检查权限状态
        boolean hasPermissions = PermissionUtils.hasAllRequiredPermissions(this);
        fabRecord.setEnabled(hasPermissions);
        
        if (!hasPermissions) {
            tvTranscriptionResult.setText("请授权录音和存储权限后使用");
        }
    }
    
    // TranscriptionCallback 实现
    @Override
    public void onTranscriptionStarted() {
        runOnUiThread(() -> {
            Log.d(TAG, "Transcription started");
        });
    }
    
    @Override
    public void onPartialResult(String partialText) {
        updateTranscriptionResult(partialText, false);
    }
    
    @Override
    public void onTranscriptionCompleted(TranscriptionResult result) {
        runOnUiThread(() -> {
            if (result != null && result.getText() != null) {
                updateTranscriptionResult(result.getText(), true);
                Log.d(TAG, "Transcription completed: " + result.getText());
            }
        });
    }
    
    @Override
    public void onTranscriptionError(TranscriptionError error) {
        runOnUiThread(() -> {
            String errorMsg = "转录错误: " + (error != null ? error.getMessage() : "未知错误");
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            Log.e(TAG, errorMsg);
            
            // 停止录音
            if (isRecording) {
                stopRecording();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUIState();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停录音但不停止服务
        if (isRecording) {
            // 可以选择暂停或继续在后台录音
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止录音
        if (isRecording) {
            stopRecording();
        }
        
        // 停止时间更新
        stopTimeUpdate();
        
        // 解绑服务
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        
        // 注销广播接收器
        if (transcriptionReceiver != null) {
            unregisterReceiver(transcriptionReceiver);
        }
        
        // 释放引擎工厂
        if (engineFactory != null) {
            engineFactory.release();
        }
        
        // 释放权限管理器
        if (permissionManager != null) {
            permissionManager.release();
        }
        
        Log.d(TAG, "MainActivity destroyed");
    }
    
    // PermissionManagerCallback 实现
    @Override
    public void onAllPermissionsGranted() {
        Log.d(TAG, "All permissions granted");
        Toast.makeText(this, getString(R.string.toast_all_permissions_granted), Toast.LENGTH_SHORT).show();
        updateUIState();
    }
    
    @Override
    public void onPermissionDenied() {
        Log.w(TAG, "Permission denied");
        Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show();
        updateUIState();
    }
    
    @Override
    public void onPermissionPermanentlyDenied() {
        Log.w(TAG, "Permission permanently denied");
        Toast.makeText(this, getString(R.string.permission_permanently_denied_message), Toast.LENGTH_LONG).show();
        // 可以选择关闭应用或保持当前状态
        updateUIState();
    }
}