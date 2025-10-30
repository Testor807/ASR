package com.example.cantonesevoicerecognition.ui.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cantonesevoicerecognition.R;
import com.example.cantonesevoicerecognition.engine.OfflineModeHelper;
import com.example.cantonesevoicerecognition.engine.OfflineModeManager;
import com.example.cantonesevoicerecognition.engine.ModelDownloadCallback;
import com.example.cantonesevoicerecognition.utils.SettingsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 应用设置Fragment
 */
public class SettingsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String TAG = "SettingsFragment";
    
    // UI组件
    private TextView tvOfflineStatus;
    private SwitchMaterial switchOfflineMode;
    private TextView tvModelStatus;
    private MaterialButton btnDownloadModel;
    private Slider sliderConfidence;
    private TextView tvConfidenceValue;
    private SwitchMaterial switchAutoSave;
    private RadioGroup rgAudioQuality;
    private SwitchMaterial switchNoiseReduction;
    private SwitchMaterial switchVad;
    private LinearLayout llLanguageSetting;
    private TextView tvLanguageSummary;
    private TextView tvThemeSummary;
    private LinearLayout llClearCache;
    private TextView tvCacheSize;
    private LinearLayout llVersionInfo;
    private TextView tvVersionName;
    private LinearLayout llPrivacyPolicy;
    private LinearLayout llOpenSource;
    
    // 管理器
    private SettingsManager settingsManager;
    private OfflineModeManager offlineModeManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeManagers();
        setupListeners();
        loadSettings();
        updateUI();
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeViews(View view) {
        // 转录设置
        tvOfflineStatus = view.findViewById(R.id.tv_offline_status);
        switchOfflineMode = view.findViewById(R.id.switch_offline_mode);
        tvModelStatus = view.findViewById(R.id.tv_model_status);
        btnDownloadModel = view.findViewById(R.id.btn_download_model);
        sliderConfidence = view.findViewById(R.id.slider_confidence);
        tvConfidenceValue = view.findViewById(R.id.tv_confidence_value);
        switchAutoSave = view.findViewById(R.id.switch_auto_save);
        
        // 音频设置
        rgAudioQuality = view.findViewById(R.id.rg_audio_quality);
        switchNoiseReduction = view.findViewById(R.id.switch_noise_reduction);
        switchVad = view.findViewById(R.id.switch_vad);
        
        // 应用设置
        llLanguageSetting = view.findViewById(R.id.ll_language_setting);
        tvLanguageSummary = view.findViewById(R.id.tv_language_summary);
        tvThemeSummary = view.findViewById(R.id.tv_theme_summary);
        llClearCache = view.findViewById(R.id.ll_clear_cache);
        tvCacheSize = view.findViewById(R.id.tv_cache_size);
        
        // 关于
        llVersionInfo = view.findViewById(R.id.ll_version_info);
        tvVersionName = view.findViewById(R.id.tv_version_name);
        llPrivacyPolicy = view.findViewById(R.id.ll_privacy_policy);
        llOpenSource = view.findViewById(R.id.ll_open_source);
    }
    
    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        settingsManager = SettingsManager.getInstance(requireContext());
        offlineModeManager = new OfflineModeManager(requireContext());
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 离线模式开关
        switchOfflineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !offlineModeManager.isOfflineModeAvailable()) {
                // 需要先下载模型
                switchOfflineMode.setChecked(false);
                showDownloadModelDialog();
            } else {
                settingsManager.setOfflineModeEnabled(isChecked);
                updateOfflineModeStatus();
            }
        });
        
        // 下载模型按钮
        btnDownloadModel.setOnClickListener(v -> downloadModel());
        
        // 置信度滑块
        sliderConfidence.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                settingsManager.setConfidenceThreshold(value);
                updateConfidenceValue();
            }
        });
        
        // 自动保存开关
        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> 
                settingsManager.setAutoSaveEnabled(isChecked));
        
        // 音频质量选择
        rgAudioQuality.setOnCheckedChangeListener((group, checkedId) -> {
            String quality;
            if (checkedId == R.id.rb_quality_high) {
                quality = SettingsManager.AUDIO_QUALITY_HIGH;
            } else if (checkedId == R.id.rb_quality_low) {
                quality = SettingsManager.AUDIO_QUALITY_LOW;
            } else {
                quality = SettingsManager.AUDIO_QUALITY_STANDARD;
            }
            settingsManager.setAudioQuality(quality);
        });
        
        // 噪声抑制开关
        switchNoiseReduction.setOnCheckedChangeListener((buttonView, isChecked) -> 
                settingsManager.setNoiseReductionEnabled(isChecked));
        
        // 语音活动检测开关
        switchVad.setOnCheckedChangeListener((buttonView, isChecked) -> 
                settingsManager.setVadEnabled(isChecked));
        
        // 语言设置
        llLanguageSetting.setOnClickListener(v -> showLanguageDialog());
        
        // 清除缓存
        llClearCache.setOnClickListener(v -> showClearCacheDialog());
        
        // 版本信息
        llVersionInfo.setOnClickListener(v -> showVersionDialog());
        
        // 隐私政策
        llPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        
        // 开源许可
        llOpenSource.setOnClickListener(v -> showOpenSourceLicenses());
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        // 转录设置
        switchOfflineMode.setChecked(settingsManager.isOfflineModeEnabled());
        sliderConfidence.setValue(settingsManager.getConfidenceThreshold());
        switchAutoSave.setChecked(settingsManager.isAutoSaveEnabled());
        
        // 音频设置
        String audioQuality = settingsManager.getAudioQuality();
        switch (audioQuality) {
            case SettingsManager.AUDIO_QUALITY_HIGH:
                rgAudioQuality.check(R.id.rb_quality_high);
                break;
            case SettingsManager.AUDIO_QUALITY_LOW:
                rgAudioQuality.check(R.id.rb_quality_low);
                break;
            default:
                rgAudioQuality.check(R.id.rb_quality_standard);
                break;
        }
        
        switchNoiseReduction.setChecked(settingsManager.isNoiseReductionEnabled());
        switchVad.setChecked(settingsManager.isVadEnabled());
        
        // 应用设置
        tvLanguageSummary.setText(settingsManager.getLanguageDisplayName());
        tvThemeSummary.setText(settingsManager.getThemeDisplayName());
    }
    
    /**
     * 更新UI状态
     */
    private void updateUI() {
        updateOfflineModeStatus();
        updateModelStatus();
        updateConfidenceValue();
        updateCacheSize();
        updateVersionInfo();
    }
    
    /**
     * 更新离线模式状态
     */
    private void updateOfflineModeStatus() {
        boolean isAvailable = offlineModeManager.isOfflineModeAvailable();
        boolean isEnabled = settingsManager.isOfflineModeEnabled();
        
        switchOfflineMode.setEnabled(isAvailable);
        
        if (isAvailable) {
            if (isEnabled) {
                tvOfflineStatus.setText("已启用 - 使用本地模型");
            } else {
                tvOfflineStatus.setText("可用 - 点击启用");
            }
        } else {
            tvOfflineStatus.setText("需要下载模型文件");
        }
    }
    
    /**
     * 更新模型状态
     */
    private void updateModelStatus() {
        if (offlineModeManager.isOfflineModeAvailable()) {
            tvModelStatus.setText("已下载");
            btnDownloadModel.setText("重新下载");
        } else if (offlineModeManager.isDownloading()) {
            tvModelStatus.setText("下载中...");
            btnDownloadModel.setText("取消");
            btnDownloadModel.setEnabled(false);
        } else {
            tvModelStatus.setText("未下载");
            btnDownloadModel.setText("下载");
        }
    }
    
    /**
     * 更新置信度值显示
     */
    private void updateConfidenceValue() {
        float confidence = settingsManager.getConfidenceThreshold();
        int percent = Math.round(confidence * 100);
        tvConfidenceValue.setText(percent + "%");
    }
    
    /**
     * 更新缓存大小
     */
    private void updateCacheSize() {
        // 计算缓存大小
        long cacheSize = calculateCacheSize();
        tvCacheSize.setText(formatFileSize(cacheSize));
    }
    
    /**
     * 更新版本信息
     */
    private void updateVersionInfo() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            tvVersionName.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersionName.setText("未知");
        }
    }
    
    /**
     * 显示下载模型对话框
     */
    private void showDownloadModelDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("下载离线模型")
                .setMessage("启用离线模式需要下载语音识别模型文件（约100MB）。是否现在下载？")
                .setPositiveButton("下载", (dialog, which) -> downloadModel())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 下载模型
     */
    private void downloadModel() {
        if (offlineModeManager.isDownloading()) {
            offlineModeManager.cancelDownload();
            return;
        }
        
        offlineModeManager.downloadModel(new ModelDownloadCallback() {
            @Override
            public void onDownloadStarted() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateModelStatus();
                        Toast.makeText(requireContext(), "开始下载模型", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onDownloadProgress(float progress, long downloadedBytes, long totalBytes) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        int percent = Math.round(progress * 100);
                        tvModelStatus.setText("下载中... " + percent + "%");
                    });
                }
            }
            
            @Override
            public void onDownloadCompleted(String modelPath) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateModelStatus();
                        updateOfflineModeStatus();
                        Toast.makeText(requireContext(), "模型下载完成", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onDownloadError(Exception error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateModelStatus();
                        Toast.makeText(requireContext(), "下载失败: " + error.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * 显示语言选择对话框
     */
    private void showLanguageDialog() {
        String[] languages = {"简体中文", "English"};
        String[] languageCodes = {SettingsManager.LANGUAGE_CHINESE, SettingsManager.LANGUAGE_ENGLISH};
        
        String currentLanguage = settingsManager.getLanguage();
        int currentIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                currentIndex = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择语言")
                .setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
                    settingsManager.setLanguage(languageCodes[which]);
                    tvLanguageSummary.setText(languages[which]);
                    dialog.dismiss();
                    
                    // 提示重启应用
                    Toast.makeText(requireContext(), "语言设置将在重启应用后生效", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示清除缓存对话框
     */
    private void showClearCacheDialog() {
        long cacheSize = calculateCacheSize();
        String sizeText = formatFileSize(cacheSize);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("清除缓存")
                .setMessage("将清除 " + sizeText + " 的缓存数据，包括临时音频文件和处理缓存。")
                .setPositiveButton("清除", (dialog, which) -> clearCache())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示版本信息对话框
     */
    private void showVersionDialog() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            
            String versionInfo = "版本名称: " + packageInfo.versionName + "\n" +
                               "版本代码: " + packageInfo.versionCode + "\n" +
                               "包名: " + packageInfo.packageName;
            
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("版本信息")
                    .setMessage(versionInfo)
                    .setPositiveButton("确定", null)
                    .show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(requireContext(), "无法获取版本信息", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示隐私政策
     */
    private void showPrivacyPolicy() {
        // TODO: 实现隐私政策显示
        Toast.makeText(requireContext(), "隐私政策功能开发中", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示开源许可
     */
    private void showOpenSourceLicenses() {
        // TODO: 实现开源许可显示
        Toast.makeText(requireContext(), "开源许可功能开发中", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 计算缓存大小
     */
    private long calculateCacheSize() {
        long totalSize = 0;
        
        // 计算应用缓存目录大小
        File cacheDir = requireContext().getCacheDir();
        if (cacheDir != null && cacheDir.exists()) {
            totalSize += calculateDirectorySize(cacheDir);
        }
        
        // 计算外部缓存目录大小
        File externalCacheDir = requireContext().getExternalCacheDir();
        if (externalCacheDir != null && externalCacheDir.exists()) {
            totalSize += calculateDirectorySize(externalCacheDir);
        }
        
        return totalSize;
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
    
    /**
     * 清除缓存
     */
    private void clearCache() {
        try {
            // 清除应用缓存目录
            File cacheDir = requireContext().getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }
            
            // 清除外部缓存目录
            File externalCacheDir = requireContext().getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDirectory(externalCacheDir);
            }
            
            updateCacheSize();
            Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "清除缓存失败: " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.#").format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return new DecimalFormat("#.#").format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return new DecimalFormat("#.#").format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // 处理设置变更
        switch (key) {
            case SettingsManager.KEY_OFFLINE_MODE:
                updateOfflineModeStatus();
                break;
            case SettingsManager.KEY_CONFIDENCE_THRESHOLD:
                updateConfidenceValue();
                break;
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        settingsManager.registerOnSharedPreferenceChangeListener(this);
        updateUI();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        settingsManager.unregisterOnSharedPreferenceChangeListener(this);
    }
}