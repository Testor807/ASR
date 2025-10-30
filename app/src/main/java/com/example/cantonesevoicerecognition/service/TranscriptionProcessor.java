package com.example.cantonesevoicerecognition.service;

import android.util.Log;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import com.example.cantonesevoicerecognition.data.model.TranscriptionResult;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 轉錄結果處理器
 * 負責轉錄結果的格式化、後處理和優化
 */
public class TranscriptionProcessor {
    private static final String TAG = "TranscriptionProcessor";
    
    // 文本處理模式
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_SPACES = Pattern.compile("\\s+([。！？，、；：])");
    private static final Pattern ENGLISH_CHINESE = Pattern.compile("([a-zA-Z])([\\u4e00-\\u9fff])");
    private static final Pattern CHINESE_ENGLISH = Pattern.compile("([\\u4e00-\\u9fff])([a-zA-Z])");
    
    /**
     * 創建轉錄記錄
     * @param result 轉錄結果
     * @param isRealTime 是否為實時轉錄
     * @param audioFilePath 音頻文件路徑（可選）
     * @return 轉錄記錄
     */
    public static TranscriptionRecord createTranscriptionRecord(
            TranscriptionResult result, boolean isRealTime, String audioFilePath) {
        
        if (result == null) {
            Log.w(TAG, "TranscriptionResult is null");
            return null;
        }
        
        TranscriptionRecord record = new TranscriptionRecord();
        record.setOriginalText(result.getText());
        record.setEditedText(formatTranscriptionText(result.getText()));
        record.setTimestamp(System.currentTimeMillis());
        record.setConfidence(result.getConfidence());
        record.setRealTime(isRealTime);
        record.setAudioFilePath(audioFilePath);
        record.setDuration((int) result.getProcessingTime());
        
        return record;
    }
    
    /**
     * 格式化轉錄文本
     * @param rawText 原始文本
     * @return 格式化後的文本
     */
    public static String formatTranscriptionText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        
        String formatted = rawText.trim();
        
        // 1. 移除多餘的空格
        formatted = MULTIPLE_SPACES.matcher(formatted).replaceAll(" ");
        
        // 2. 修正標點符號前的空格
        formatted = PUNCTUATION_SPACES.matcher(formatted).replaceAll("$1");
        
        // 3. 在中英文之間添加空格
        formatted = ENGLISH_CHINESE.matcher(formatted).replaceAll("$1 $2");
        formatted = CHINESE_ENGLISH.matcher(formatted).replaceAll("$1 $2");
        
        // 4. 確保句子以標點符號結尾
        if (!formatted.matches(".*[。！？.!?]$")) {
            // 根據內容判斷應該添加什麼標點
            if (formatted.matches(".*[？?].*")) {
                formatted += "？";
            } else if (formatted.matches(".*[！!].*")) {
                formatted += "！";
            } else {
                formatted += "。";
            }
        }
        
        // 5. 首字母大寫（如果包含英文）
        formatted = capitalizeFirstLetter(formatted);
        
        // 6. 修正常見的識別錯誤
        formatted = fixCommonErrors(formatted);
        
        return formatted;
    }
    
    /**
     * 首字母大寫
     */
    private static String capitalizeFirstLetter(String text) {
        if (text.length() == 0) {
            return text;
        }
        
        char firstChar = text.charAt(0);
        if (Character.isLetter(firstChar) && Character.isLowerCase(firstChar)) {
            return Character.toUpperCase(firstChar) + text.substring(1);
        }
        
        return text;
    }
    
    /**
     * 修正常見的識別錯誤
     */
    private static String fixCommonErrors(String text) {
        // 常見的粵語識別錯誤修正
        text = text.replace("係咪", "係唔係");
        text = text.replace("唔係咪", "唔係");
        text = text.replace("咁樣", "咁");
        text = text.replace("呢個", "呢個");
        text = text.replace("嗰個", "嗰個");
        
        // 數字格式化
        text = formatNumbers(text);
        
        // 時間格式化
        text = formatTime(text);
        
        return text;
    }
    
    /**
     * 格式化數字
     */
    private static String formatNumbers(String text) {
        // 將中文數字轉換為阿拉伯數字（簡單實現）
        text = text.replace("一", "1");
        text = text.replace("二", "2");
        text = text.replace("三", "3");
        text = text.replace("四", "4");
        text = text.replace("五", "5");
        text = text.replace("六", "6");
        text = text.replace("七", "7");
        text = text.replace("八", "8");
        text = text.replace("九", "9");
        text = text.replace("十", "10");
        
        return text;
    }
    
    /**
     * 格式化時間
     */
    private static String formatTime(String text) {
        // 簡單的時間格式化
        text = text.replaceAll("(\\d+)點(\\d+)分", "$1:$2");
        text = text.replaceAll("(\\d+)點", "$1:00");
        
        return text;
    }
    
    /**
     * 後處理轉錄記錄
     * @param record 轉錄記錄
     * @param callback 處理完成回調
     */
    public static void postProcessTranscription(TranscriptionRecord record, 
                                              PostProcessCallback callback) {
        
        if (record == null) {
            if (callback != null) {
                callback.onPostProcessError(new IllegalArgumentException("TranscriptionRecord is null"));
            }
            return;
        }
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // 格式化文本
                String formattedText = formatTranscriptionText(record.getOriginalText());
                record.setEditedText(formattedText);
                
                // 計算文本統計信息
                TextStatistics stats = calculateTextStatistics(formattedText);
                
                // 分析文本質量
                float qualityScore = analyzeTextQuality(formattedText, record.getConfidence());
                
                // 檢測語言
                String detectedLanguage = detectLanguage(formattedText);
                
                Log.i(TAG, "Post-processing completed: " + stats + 
                          ", quality=" + qualityScore + ", language=" + detectedLanguage);
                
                if (callback != null) {
                    callback.onPostProcessCompleted(record, stats, qualityScore, detectedLanguage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during post-processing", e);
                if (callback != null) {
                    callback.onPostProcessError(e);
                }
            } finally {
                executor.shutdown();
            }
        });
    }
    
    /**
     * 計算文本統計信息
     */
    private static TextStatistics calculateTextStatistics(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new TextStatistics();
        }
        
        TextStatistics stats = new TextStatistics();
        
        // 字符數（包括空格和標點）
        stats.totalCharacters = text.length();
        
        // 中文字符數
        stats.chineseCharacters = (int) text.chars()
                .filter(c -> c >= 0x4e00 && c <= 0x9fff)
                .count();
        
        // 英文字符數
        stats.englishCharacters = (int) text.chars()
                .filter(Character::isLetter)
                .filter(c -> c < 0x4e00 || c > 0x9fff)
                .count();
        
        // 數字字符數
        stats.digitCharacters = (int) text.chars()
                .filter(Character::isDigit)
                .count();
        
        // 標點符號數
        stats.punctuationCount = (int) text.chars()
                .filter(c -> "。！？，、；：.!?,:;".indexOf(c) >= 0)
                .count();
        
        // 詞數估算（中文按字符計算，英文按單詞計算）
        stats.estimatedWords = stats.chineseCharacters + 
                              text.split("\\s+").length - 1; // 減去中文部分重複計算
        
        // 句子數
        stats.sentenceCount = (int) text.chars()
                .filter(c -> "。！？.!?".indexOf(c) >= 0)
                .count();
        
        if (stats.sentenceCount == 0 && stats.totalCharacters > 0) {
            stats.sentenceCount = 1; // 至少有一個句子
        }
        
        return stats;
    }
    
    /**
     * 分析文本質量
     */
    private static float analyzeTextQuality(String text, float originalConfidence) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0f;
        }
        
        float qualityScore = originalConfidence;
        
        // 長度因子
        int length = text.length();
        if (length > 10) {
            qualityScore += 0.1f;
        }
        if (length > 50) {
            qualityScore += 0.1f;
        }
        
        // 標點符號因子
        if (text.matches(".*[。！？.!?].*")) {
            qualityScore += 0.1f;
        }
        
        // 中英文混合因子
        boolean hasChinese = text.matches(".*[\\u4e00-\\u9fff].*");
        boolean hasEnglish = text.matches(".*[a-zA-Z].*");
        if (hasChinese && hasEnglish) {
            qualityScore += 0.05f;
        }
        
        // 重複字符懲罰
        if (text.matches(".*(.)\\1{3,}.*")) { // 4個或更多重複字符
            qualityScore -= 0.2f;
        }
        
        // 確保分數在0-1範圍內
        return Math.max(0.0f, Math.min(1.0f, qualityScore));
    }
    
    /**
     * 檢測語言
     */
    private static String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }
        
        int chineseCount = (int) text.chars()
                .filter(c -> c >= 0x4e00 && c <= 0x9fff)
                .count();
        
        int englishCount = (int) text.chars()
                .filter(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                .count();
        
        if (chineseCount > englishCount * 2) {
            return "zh-yue"; // 粵語
        } else if (englishCount > chineseCount * 2) {
            return "en";
        } else if (chineseCount > 0 && englishCount > 0) {
            return "zh-yue-en"; // 中英混合
        } else if (chineseCount > 0) {
            return "zh-yue";
        } else if (englishCount > 0) {
            return "en";
        } else {
            return "unknown";
        }
    }
    
    /**
     * 文本統計信息類
     */
    public static class TextStatistics {
        public int totalCharacters = 0;
        public int chineseCharacters = 0;
        public int englishCharacters = 0;
        public int digitCharacters = 0;
        public int punctuationCount = 0;
        public int estimatedWords = 0;
        public int sentenceCount = 0;
        
        @Override
        public String toString() {
            return String.format("TextStats{total=%d, zh=%d, en=%d, digits=%d, punct=%d, words=%d, sentences=%d}",
                               totalCharacters, chineseCharacters, englishCharacters, 
                               digitCharacters, punctuationCount, estimatedWords, sentenceCount);
        }
    }
    
    /**
     * 後處理回調接口
     */
    public interface PostProcessCallback {
        void onPostProcessCompleted(TranscriptionRecord record, TextStatistics stats, 
                                  float qualityScore, String detectedLanguage);
        void onPostProcessError(Exception error);
    }
}