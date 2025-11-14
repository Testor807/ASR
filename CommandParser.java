package com.example.offlinecantoneseasr;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CommandParser {
    private static final String TAG = "CommandParser";

    private Context context;
    private TextToSpeech textToSpeech;
    private Map<String, Runnable> commandMap;
    private Map<String, String> commandSynonyms;

    public CommandParser(Context context) {
        this.context = context;
        initializeCommandMaps();
        initializeTextToSpeech();
    }

    public CommandParser() {

    }

    private void initializeCommandMaps() {
        // 主要指令映射
        commandMap = new HashMap<>();

        // 设备控制指令
        commandMap.put("打開燈", this::turnOnLight);
        commandMap.put("關燈", this::turnOffLight);
        commandMap.put("開燈", this::turnOnLight);
        commandMap.put("熄燈", this::turnOffLight);

        // 媒体控制指令
        commandMap.put("播放音樂", this::playMusic);
        commandMap.put("播歌", this::playMusic);
        commandMap.put("暫停音樂", this::pauseMusic);
        commandMap.put("停音樂", this::pauseMusic);
        commandMap.put("停止播放", this::pauseMusic);
        commandMap.put("下一首", this::nextSong);
        commandMap.put("上一首", this::previousSong);
        commandMap.put("大聲啲", this::volumeUp);
        commandMap.put("細聲啲", this::volumeDown);
        commandMap.put("大聲的", this::volumeUp);
        commandMap.put("細聲的", this::volumeDown);

        // 通信指令
        commandMap.put("打電話", this::makeCall);
        commandMap.put("打電話給", this::makeCall);
        commandMap.put("發信息", this::sendMessage);
        commandMap.put("發短信", this::sendMessage);

        // 相机指令
        commandMap.put("影相", this::takePhoto);
        commandMap.put("拍照", this::takePhoto);
        commandMap.put("錄像", this::recordVideo);
        commandMap.put("錄影", this::recordVideo);

        // 系统指令
        commandMap.put("打開設定", this::openSettings);
        commandMap.put("打開設置", this::openSettings);
        commandMap.put("打開藍牙", this::openBluetooth);
        commandMap.put("打開WiFi", this::openWifi);
        commandMap.put("關閉藍牙", this::closeBluetooth);
        commandMap.put("關閉WiFi", this::closeWifi);

        // 应用指令
        commandMap.put("打開相機", this::openCamera);
        commandMap.put("打開電話", this::openDialer);
        commandMap.put("打開信息", this::openMessages);

        // 帮助指令
        commandMap.put("幫助", this::showHelp);
        commandMap.put("幫手", this::showHelp);
        commandMap.put("指令列表", this::showCommands);

        // 初始化同义词映射
        commandSynonyms = new HashMap<>();
        commandSynonyms.put("開燈", "打開燈");
        commandSynonyms.put("熄燈", "關燈");
        commandSynonyms.put("播歌", "播放音樂");
        commandSynonyms.put("停歌", "暫停音樂");
        commandSynonyms.put("停音樂", "暫停音樂");
        commandSynonyms.put("大聲的", "大聲啲");
        commandSynonyms.put("細聲的", "細聲啲");
        commandSynonyms.put("影相", "拍照");
        commandSynonyms.put("錄影", "錄像");
        commandSynonyms.put("打開設置", "打開設定");
        commandSynonyms.put("幫手", "幫助");
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // 设置粤语语音（如果支持）
                    int result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "粤语TTS不支持，使用默认语音");
                    }
                } else {
                    Log.e(TAG, "TTS初始化失败");
                }
            }
        });
    }

    public void parseAndExecute(String speechText) {
        if (speechText == null || speechText.trim().isEmpty()) {
            Log.d(TAG, "输入文本为空");
            return;
        }

        Log.d(TAG, "解析指令: " + speechText);

        // 预处理文本
        String processedText = preprocessText(speechText);

        // 精确匹配
        for (Map.Entry<String, Runnable> entry : commandMap.entrySet()) {
            if (processedText.contains(entry.getKey())) {
                Log.d(TAG, "找到匹配指令: " + entry.getKey());
                executeCommandWithFeedback(entry.getKey(), entry.getValue());
                return;
            }
        }

        // 同义词匹配
        String normalizedText = normalizeText(processedText);
        for (Map.Entry<String, Runnable> entry : commandMap.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                Log.d(TAG, "通过同义词找到指令: " + entry.getKey());
                executeCommandWithFeedback(entry.getKey(), entry.getValue());
                return;
            }
        }

        // 模糊匹配
        fuzzyMatchAndExecute(processedText);
    }

    private String preprocessText(String text) {
        // 移除标点符号和多余空格
        String cleaned = text.replaceAll("[^\\w\\u4e00-\\u9fff]", " ").trim();
        // 合并多个空格
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private String normalizeText(String text) {
        String normalized = text;
        // 替换同义词
        for (Map.Entry<String, String> entry : commandSynonyms.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized;
    }

    private void fuzzyMatchAndExecute(String text) {
        String bestMatch = null;
        double bestScore = 0.7; // 相似度阈值

        for (String command : commandMap.keySet()) {
            double similarity = calculateSimilarity(text, command);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = command;
            }
        }

        if (bestMatch != null) {
            Log.d(TAG, "模糊匹配到指令: " + bestMatch + " (相似度: " + bestScore + ")");
            executeCommandWithFeedback(bestMatch, commandMap.get(bestMatch));
        } else {
            handleUnknownCommand(text);
        }
    }

    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) return 1.0;

        int distance = computeLevenshteinDistance(text1, text2);
        return 1.0 - (double) distance / maxLength;
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + cost,
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    private void executeCommandWithFeedback(String command, Runnable action) {
        // 语音反馈
        speakFeedback("執行 " + command + " 指令");

        // 显示Toast反馈
        showToast("執行: " + command);

        // 执行指令
        try {
            action.run();
        } catch (Exception e) {
            Log.e(TAG, "执行指令失败: " + e.getMessage());
            speakFeedback("指令執行失敗");
            showToast("指令執行失敗");
        }
    }

    private void handleUnknownCommand(String command) {
        Log.d(TAG, "未知指令: " + command);
        String response = getRandomUnknownCommandResponse();
        speakFeedback(response);
        showToast("無法識別指令: " + command);
    }

    private String getRandomUnknownCommandResponse() {
        List<String> responses = Arrays.asList(
                "對唔住，我聽唔明呢個指令",
                "唔好意思，我未學識呢個指令",
                "呢個指令我仲未識處理",
                "可唔可以講多次？我聽唔清楚"
        );
        Random random = new Random();
        return responses.get(random.nextInt(responses.size()));
    }

    // ========== 指令执行方法 ==========

    private void turnOnLight() {
        Log.d(TAG, "执行开灯指令");
        // 这里可以调用智能家居API
        showToast("正在打開燈光...");
    }

    private void turnOffLight() {
        Log.d(TAG, "执行关灯指令");
        // 这里可以调用智能家居API
        showToast("正在關閉燈光...");
    }

    private void playMusic() {
        Log.d(TAG, "执行播放音乐指令");
        // 这里可以调用音乐播放API
        showToast("正在播放音樂...");
    }

    private void pauseMusic() {
        Log.d(TAG, "执行暂停音乐指令");
        // 这里可以调用音乐播放API
        showToast("暫停播放音樂");
    }

    private void nextSong() {
        Log.d(TAG, "执行下一首指令");
        // 这里可以调用音乐播放API
        showToast("下一首歌曲");
    }

    private void previousSong() {
        Log.d(TAG, "执行上一首指令");
        // 这里可以调用音乐播放API
        showToast("上一首歌曲");
    }

    private void volumeUp() {
        Log.d(TAG, "执行音量增加指令");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            showToast("音量增加");
        }
    }

    private void volumeDown() {
        Log.d(TAG, "执行音量减少指令");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            showToast("音量減少");
        }
    }

    private void makeCall() {
        Log.d(TAG, "执行打电话指令");
        // 打开拨号界面
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開電話撥號");
    }

    private void sendMessage() {
        Log.d(TAG, "执行发信息指令");
        // 打开短信界面
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開信息應用");
    }

    private void takePhoto() {
        Log.d(TAG, "执行拍照指令");
        // 打开相机应用
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            showToast("打開相機拍照");
        } else {
            showToast("找不到相機應用");
        }
    }

    private void recordVideo() {
        Log.d(TAG, "执行录像指令");
        // 打开录像功能
        Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            showToast("開始錄像");
        } else {
            showToast("找不到錄像應用");
        }
    }

    private void openSettings() {
        Log.d(TAG, "执行打开设置指令");
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開系統設定");
    }

    private void openBluetooth() {
        Log.d(TAG, "执行打开蓝牙指令");
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開藍牙設定");
    }

    private void openWifi() {
        Log.d(TAG, "执行打开WiFi指令");
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開WiFi設定");
    }

    private void closeBluetooth() {
        Log.d(TAG, "执行关闭蓝牙指令");
        // 这里需要特殊权限才能直接关闭蓝牙
        showToast("請在手動關閉藍牙");
    }

    private void closeWifi() {
        Log.d(TAG, "执行关闭WiFi指令");
        // 这里需要特殊权限才能直接关闭WiFi
        showToast("請在手動關閉WiFi");
    }

    private void openCamera() {
        Log.d(TAG, "执行打开相机指令");
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            showToast("打開相機");
        } else {
            showToast("找不到相機應用");
        }
    }

    private void openDialer() {
        Log.d(TAG, "执行打开电话指令");
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開電話");
    }

    private void openMessages() {
        Log.d(TAG, "执行打开信息指令");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("打開信息");
    }

    private void showHelp() {
        Log.d(TAG, "执行帮助指令");
        StringBuilder helpText = new StringBuilder();
        helpText.append("支持嘅指令包括：\n");

        List<String> commands = new ArrayList<>(commandMap.keySet());
        for (int i = 0; i < Math.min(commands.size(), 10); i++) {
            helpText.append("• ").append(commands.get(i)).append("\n");
        }

        if (commands.size() > 10) {
            helpText.append("... 仲有更多指令");
        }

        showToast(helpText.toString());
        speakFeedback("我識得處理開燈關燈播放音樂同其他指令");
    }

    private void showCommands() {
        Log.d(TAG, "显示指令列表");
        List<String> commands = new ArrayList<>(commandMap.keySet());
        String commandList = "可用指令: " + String.join(", ", commands);
        showToast(commandList);
    }

    // ========== 工具方法 ==========

    private void speakFeedback(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public List<String> getAvailableCommands() {
        return new ArrayList<>(commandMap.keySet());
    }
}
