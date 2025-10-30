package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.cantonesevoicerecognition.engine.TranscriptionError;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日志管理器
 * 提供分级日志记录、文件日志和错误统计功能
 */
public class LogManager {
    
    private static final String TAG = "LogManager";
    private static LogManager instance;
    
    // 日志级别
    public enum LogLevel {
        VERBOSE(2, "V"),
        DEBUG(3, "D"),
        INFO(4, "I"),
        WARN(5, "W"),
        ERROR(6, "E");
        
        private final int priority;
        private final String shortName;
        
        LogLevel(int priority, String shortName) {
            this.priority = priority;
            this.shortName = shortName;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getShortName() {
            return shortName;
        }
    }
    
    private final Context context;
    private final ExecutorService logExecutor;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat fileNameFormat;
    private final Map<TranscriptionError, Integer> errorStatistics;
    
    // 配置参数
    private LogLevel minLogLevel = LogLevel.DEBUG;
    private boolean enableFileLogging = true;
    private boolean enableConsoleLogging = true;
    private long maxLogFileSize = 5 * 1024 * 1024; // 5MB
    private int maxLogFiles = 5;
    
    private LogManager(Context context) {
        this.context = context.getApplicationContext();
        this.logExecutor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        this.fileNameFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.errorStatistics = new HashMap<>();
        
        // 创建日志目录
        createLogDirectory();
        
        // 清理旧日志文件
        cleanupOldLogFiles();
    }
    
    /**
     * 获取LogManager单例实例
     */
    public static synchronized LogManager getInstance(Context context) {
        if (instance == null) {
            instance = new LogManager(context);
        }
        return instance;
    }
    
    /**
     * 设置最小日志级别
     */
    public void setMinLogLevel(LogLevel level) {
        this.minLogLevel = level;
    }
    
    /**
     * 设置是否启用文件日志
     */
    public void setFileLoggingEnabled(boolean enabled) {
        this.enableFileLogging = enabled;
    }
    
    /**
     * 设置是否启用控制台日志
     */
    public void setConsoleLoggingEnabled(boolean enabled) {
        this.enableConsoleLogging = enabled;
    }
    
    /**
     * 记录VERBOSE级别日志
     */
    public void v(String tag, String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }
    
    /**
     * 记录DEBUG级别日志
     */
    public void d(String tag, String message) {
        log(LogLevel.DEBUG, tag, message, null);
    }
    
    /**
     * 记录INFO级别日志
     */
    public void i(String tag, String message) {
        log(LogLevel.INFO, tag, message, null);
    }
    
    /**
     * 记录WARN级别日志
     */
    public void w(String tag, String message) {
        log(LogLevel.WARN, tag, message, null);
    }
    
    /**
     * 记录WARN级别日志（带异常）
     */
    public void w(String tag, String message, Throwable throwable) {
        log(LogLevel.WARN, tag, message, throwable);
    }
    
    /**
     * 记录ERROR级别日志
     */
    public void e(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }
    
    /**
     * 记录ERROR级别日志（带异常）
     */
    public void e(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }
    
    /**
     * 记录日志的核心方法
     */
    private void log(LogLevel level, String tag, String message, Throwable throwable) {
        // 检查日志级别
        if (level.getPriority() < minLogLevel.getPriority()) {
            return;
        }
        
        // 控制台日志
        if (enableConsoleLogging) {
            logToConsole(level, tag, message, throwable);
        }
        
        // 文件日志（异步）
        if (enableFileLogging) {
            logExecutor.execute(() -> logToFile(level, tag, message, throwable));
        }
    }
    
    /**
     * 输出到控制台
     */
    private void logToConsole(LogLevel level, String tag, String message, Throwable throwable) {
        switch (level) {
            case VERBOSE:
                if (throwable != null) {
                    Log.v(tag, message, throwable);
                } else {
                    Log.v(tag, message);
                }
                break;
            case DEBUG:
                if (throwable != null) {
                    Log.d(tag, message, throwable);
                } else {
                    Log.d(tag, message);
                }
                break;
            case INFO:
                if (throwable != null) {
                    Log.i(tag, message, throwable);
                } else {
                    Log.i(tag, message);
                }
                break;
            case WARN:
                if (throwable != null) {
                    Log.w(tag, message, throwable);
                } else {
                    Log.w(tag, message);
                }
                break;
            case ERROR:
                if (throwable != null) {
                    Log.e(tag, message, throwable);
                } else {
                    Log.e(tag, message);
                }
                break;
        }
    }
    
    /**
     * 输出到文件
     */
    private void logToFile(LogLevel level, String tag, String message, Throwable throwable) {
        try {
            File logFile = getCurrentLogFile();
            if (logFile == null) {
                return;
            }
            
            // 检查文件大小，如果超过限制则轮转
            if (logFile.length() > maxLogFileSize) {
                rotateLogFile();
                logFile = getCurrentLogFile();
                if (logFile == null) {
                    return;
                }
            }
            
            // 构建日志消息
            String timestamp = dateFormat.format(new Date());
            String logMessage = String.format("%s %s/%s: %s%n", 
                timestamp, level.getShortName(), tag, message);
            
            // 写入文件
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logMessage);
                
                // 如果有异常，写入异常信息
                if (throwable != null) {
                    writer.write("异常信息: " + throwable.getMessage() + "\n");
                    for (StackTraceElement element : throwable.getStackTrace()) {
                        writer.write("    at " + element.toString() + "\n");
                    }
                }
                
                writer.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "写入日志文件失败", e);
        }
    }
    
    /**
     * 创建日志目录
     */
    private void createLogDirectory() {
        File logDir = getLogDirectory();
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                Log.w(TAG, "创建日志目录失败: " + logDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 获取日志目录
     */
    private File getLogDirectory() {
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir != null) {
            return new File(externalDir, "logs");
        } else {
            return new File(context.getFilesDir(), "logs");
        }
    }
    
    /**
     * 获取当前日志文件
     */
    private File getCurrentLogFile() {
        File logDir = getLogDirectory();
        if (!logDir.exists()) {
            createLogDirectory();
        }
        
        String fileName = "app_" + fileNameFormat.format(new Date()) + ".log";
        return new File(logDir, fileName);
    }
    
    /**
     * 轮转日志文件
     */
    private void rotateLogFile() {
        try {
            File logDir = getLogDirectory();
            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            
            if (logFiles != null && logFiles.length >= maxLogFiles) {
                // 删除最老的日志文件
                File oldestFile = null;
                long oldestTime = Long.MAX_VALUE;
                
                for (File file : logFiles) {
                    if (file.lastModified() < oldestTime) {
                        oldestTime = file.lastModified();
                        oldestFile = file;
                    }
                }
                
                if (oldestFile != null) {
                    boolean deleted = oldestFile.delete();
                    if (deleted) {
                        Log.d(TAG, "删除旧日志文件: " + oldestFile.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "轮转日志文件失败", e);
        }
    }
    
    /**
     * 清理旧日志文件
     */
    private void cleanupOldLogFiles() {
        logExecutor.execute(() -> {
            try {
                File logDir = getLogDirectory();
                File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
                
                if (logFiles != null) {
                    long currentTime = System.currentTimeMillis();
                    long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7天
                    
                    for (File file : logFiles) {
                        if (currentTime - file.lastModified() > maxAge) {
                            boolean deleted = file.delete();
                            if (deleted) {
                                Log.d(TAG, "清理过期日志文件: " + file.getName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "清理旧日志文件失败", e);
            }
        });
    }
    
    /**
     * 记录错误统计
     */
    public void recordErrorStatistics(TranscriptionError error) {
        synchronized (errorStatistics) {
            int count = errorStatistics.getOrDefault(error, 0);
            errorStatistics.put(error, count + 1);
        }
        
        // 记录统计日志
        i(TAG, String.format("错误统计更新: %s (总计: %d次)", 
            error.name(), errorStatistics.get(error)));
    }
    
    /**
     * 获取错误统计
     */
    public Map<TranscriptionError, Integer> getErrorStatistics() {
        synchronized (errorStatistics) {
            return new HashMap<>(errorStatistics);
        }
    }
    
    /**
     * 清除错误统计
     */
    public void clearErrorStatistics() {
        synchronized (errorStatistics) {
            errorStatistics.clear();
        }
        i(TAG, "错误统计已清除");
    }
    
    /**
     * 获取日志文件列表
     */
    public File[] getLogFiles() {
        File logDir = getLogDirectory();
        return logDir.listFiles((dir, name) -> name.endsWith(".log"));
    }
    
    /**
     * 获取日志目录大小
     */
    public long getLogDirectorySize() {
        File logDir = getLogDirectory();
        File[] files = logDir.listFiles();
        long totalSize = 0;
        
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        
        return totalSize;
    }
    
    /**
     * 清除所有日志文件
     */
    public void clearAllLogs() {
        logExecutor.execute(() -> {
            try {
                File logDir = getLogDirectory();
                File[] files = logDir.listFiles();
                
                if (files != null) {
                    for (File file : files) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            Log.d(TAG, "删除日志文件: " + file.getName());
                        }
                    }
                }
                
                i(TAG, "所有日志文件已清除");
            } catch (Exception e) {
                Log.e(TAG, "清除日志文件失败", e);
            }
        });
    }
    
    /**
     * 关闭日志管理器
     */
    public void shutdown() {
        if (logExecutor != null && !logExecutor.isShutdown()) {
            logExecutor.shutdown();
        }
    }
}