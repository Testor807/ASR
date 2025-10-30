package com.example.cantonesevoicerecognition;

import android.app.Application;

import com.example.cantonesevoicerecognition.utils.ErrorHandler;
import com.example.cantonesevoicerecognition.utils.LogManager;
import com.example.cantonesevoicerecognition.utils.ErrorReporter;
import com.example.cantonesevoicerecognition.utils.PerformanceOptimizer;
import com.example.cantonesevoicerecognition.utils.PerformanceMonitor;

/**
 * 应用程序主类
 * 负责初始化全局组件和错误处理系统
 */
public class CantoneseVoiceApplication extends Application {
    
    private static final String TAG = "CantoneseVoiceApp";
    
    private LogManager logManager;
    private ErrorHandler errorHandler;
    private ErrorReporter errorReporter;
    private PerformanceOptimizer performanceOptimizer;
    private long appStartTime;
    
    @Override
    public void onCreate() {
        appStartTime = System.currentTimeMillis();
        super.onCreate();
        
        // 初始化日志管理器
        initializeLogManager();
        
        // 初始化错误处理器
        initializeErrorHandler();
        
        // 初始化错误报告器
        initializeErrorReporter();
        
        // 初始化性能优化
        initializePerformanceOptimization();
        
        // 设置全局异常处理器
        setupGlobalExceptionHandler();
        
        // 记录应用启动时间
        long startupTime = System.currentTimeMillis() - appStartTime;
        PerformanceMonitor.getInstance().recordAppStartupTime(startupTime);
        
        logManager.i(TAG, "粤语语音识别应用启动完成，耗时: " + startupTime + "ms");
    }
    
    /**
     * 初始化日志管理器
     */
    private void initializeLogManager() {
        logManager = LogManager.getInstance(this);
        
        // 根据构建类型配置日志级别
        if (BuildConfig.DEBUG) {
            logManager.setMinLogLevel(LogManager.LogLevel.DEBUG);
            logManager.setConsoleLoggingEnabled(true);
            logManager.setFileLoggingEnabled(true);
        } else {
            logManager.setMinLogLevel(LogManager.LogLevel.INFO);
            logManager.setConsoleLoggingEnabled(false);
            logManager.setFileLoggingEnabled(true);
        }
        
        logManager.i(TAG, "日志管理器初始化完成");
    }
    
    /**
     * 初始化错误处理器
     */
    private void initializeErrorHandler() {
        errorHandler = ErrorHandler.getInstance(this);
        logManager.i(TAG, "错误处理器初始化完成");
    }
    
    /**
     * 初始化错误报告器
     */
    private void initializeErrorReporter() {
        errorReporter = ErrorReporter.getInstance(this);
        logManager.i(TAG, "错误报告器初始化完成");
    }
    
    /**
     * 初始化性能优化
     */
    private void initializePerformanceOptimization() {
        try {
            performanceOptimizer = PerformanceOptimizer.getInstance(this);
            performanceOptimizer.startOptimization();
            logManager.i(TAG, "性能优化器初始化完成");
        } catch (Exception e) {
            logManager.e(TAG, "性能优化器初始化失败", e);
        }
    }
    
    /**
     * 设置全局异常处理器
     */
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // 记录未捕获的异常
                logManager.e(TAG, "未捕获的异常", throwable);
                
                // 生成错误报告
                String additionalInfo = String.format(
                    "线程: %s\n线程ID: %d\n线程状态: %s",
                    thread.getName(),
                    thread.getId(),
                    thread.getState().name()
                );
                
                errorReporter.saveErrorReport(
                    com.example.cantonesevoicerecognition.engine.TranscriptionError.UNKNOWN_ERROR,
                    throwable,
                    additionalInfo
                );
                
                // 调用系统默认处理器
                System.exit(1);
            }
        });
        
        logManager.i(TAG, "全局异常处理器设置完成");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // 清理资源
        if (performanceOptimizer != null) {
            performanceOptimizer.release();
        }
        
        if (logManager != null) {
            logManager.i(TAG, "应用程序终止");
            logManager.shutdown();
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        if (logManager != null) {
            logManager.w(TAG, "系统内存不足警告");
        }
        
        // 执行内存优化
        if (performanceOptimizer != null) {
            performanceOptimizer.performFullOptimization();
        }
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        if (logManager != null) {
            String levelName = getTrimMemoryLevelName(level);
            logManager.w(TAG, "系统请求释放内存: " + levelName);
        }
        
        // 根据内存压力级别执行相应优化
        if (performanceOptimizer != null) {
            if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
                performanceOptimizer.setOptimizationLevel(PerformanceOptimizer.OptimizationLevel.BATTERY);
            }
            performanceOptimizer.performFullOptimization();
        }
    }
    
    /**
     * 获取内存修剪级别名称
     */
    private String getTrimMemoryLevelName(int level) {
        switch (level) {
            case TRIM_MEMORY_COMPLETE:
                return "COMPLETE";
            case TRIM_MEMORY_MODERATE:
                return "MODERATE";
            case TRIM_MEMORY_BACKGROUND:
                return "BACKGROUND";
            case TRIM_MEMORY_UI_HIDDEN:
                return "UI_HIDDEN";
            case TRIM_MEMORY_RUNNING_CRITICAL:
                return "RUNNING_CRITICAL";
            case TRIM_MEMORY_RUNNING_LOW:
                return "RUNNING_LOW";
            case TRIM_MEMORY_RUNNING_MODERATE:
                return "RUNNING_MODERATE";
            default:
                return "UNKNOWN(" + level + ")";
        }
    }
    
    /**
     * 获取性能优化器
     */
    public PerformanceOptimizer getPerformanceOptimizer() {
        return performanceOptimizer;
    }
    
    /**
     * 获取应用性能报告
     */
    public String getPerformanceReport() {
        if (performanceOptimizer != null) {
            return performanceOptimizer.getOptimizationSummary();
        }
        return "性能优化器不可用";
    }
    
    /**
     * 执行全面性能优化
     */
    public PerformanceOptimizer.OptimizationReport performFullOptimization() {
        if (performanceOptimizer != null) {
            return performanceOptimizer.performFullOptimization();
        }
        return null;
    }
    
    /**
     * 设置性能优化级别
     */
    public void setPerformanceOptimizationLevel(PerformanceOptimizer.OptimizationLevel level) {
        if (performanceOptimizer != null) {
            performanceOptimizer.setOptimizationLevel(level);
        }
    }
    
    /**
     * 检查应用是否满足性能要求
     */
    public boolean checkPerformanceRequirements() {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        return monitor.checkPerformanceRequirements();
    }
}