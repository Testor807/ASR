package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 性能优化集成管理器
 * 统一管理内存优化、性能监控、电池优化等功能
 */
public class PerformanceOptimizer implements 
    MemoryManager.MemoryListener, 
    BatteryOptimizer.BatteryListener {
    
    private static final String TAG = "PerformanceOptimizer";
    
    private static PerformanceOptimizer instance;
    private final Context context;
    
    // 优化组件
    private MemoryManager memoryManager;
    private PerformanceMonitor performanceMonitor;
    private BatteryOptimizer batteryOptimizer;
    private ThreadPoolManager threadPoolManager;
    private MemoryLeakDetector leakDetector;
    private SmartRecordingDetector recordingDetector;
    
    // 优化配置
    private boolean isOptimizationEnabled = true;
    private boolean isAggressiveOptimization = false;
    private OptimizationLevel currentLevel = OptimizationLevel.BALANCED;
    
    // 定时优化任务
    private final ScheduledExecutorService optimizationScheduler;
    private static final int OPTIMIZATION_INTERVAL_MINUTES = 5;
    
    /**
     * 优化级别
     */
    public enum OptimizationLevel {
        PERFORMANCE,  // 性能优先
        BALANCED,     // 平衡模式
        BATTERY       // 电池优先
    }
    
    /**
     * 优化报告
     */
    public static class OptimizationReport {
        public final long timestamp;
        public final String memoryStatus;
        public final String batteryStatus;
        public final String performanceStatus;
        public final List<String> optimizationActions;
        public final List<String> recommendations;
        
        public OptimizationReport(long timestamp, String memoryStatus, String batteryStatus,
                                String performanceStatus, List<String> optimizationActions,
                                List<String> recommendations) {
            this.timestamp = timestamp;
            this.memoryStatus = memoryStatus;
            this.batteryStatus = batteryStatus;
            this.performanceStatus = performanceStatus;
            this.optimizationActions = new ArrayList<>(optimizationActions);
            this.recommendations = new ArrayList<>(recommendations);
        }
    }
    
    private PerformanceOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.optimizationScheduler = Executors.newSingleThreadScheduledExecutor();
        
        initializeOptimizationComponents();
        Log.i(TAG, "PerformanceOptimizer initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized PerformanceOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceOptimizer(context);
        }
        return instance;
    }
    
    /**
     * 初始化优化组件
     */
    private void initializeOptimizationComponents() {
        try {
            // 初始化内存管理器
            memoryManager = MemoryManager.getInstance(context);
            memoryManager.addMemoryListener(this);
            
            // 初始化性能监控器
            performanceMonitor = PerformanceMonitor.getInstance();
            performanceMonitor.enable();
            
            // 初始化电池优化器
            batteryOptimizer = BatteryOptimizer.getInstance(context);
            batteryOptimizer.addBatteryListener(this);
            
            // 初始化线程池管理器
            threadPoolManager = ThreadPoolManager.getInstance();
            
            // 初始化内存泄漏检测器
            leakDetector = MemoryLeakDetector.getInstance();
            
            // 初始化智能录音检测器
            recordingDetector = new SmartRecordingDetector();
            
            Log.i(TAG, "All optimization components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing optimization components", e);
        }
    }
    
    /**
     * 启动性能优化
     */
    public void startOptimization() {
        if (!isOptimizationEnabled) {
            Log.w(TAG, "Optimization is disabled");
            return;
        }
        
        Log.i(TAG, "Starting performance optimization");
        
        // 启动各个组件的监控
        memoryManager.startMonitoring();
        batteryOptimizer.startMonitoring();
        leakDetector.startDetection();
        
        // 启动定时优化任务
        optimizationScheduler.scheduleAtFixedRate(
            this::performPeriodicOptimization,
            OPTIMIZATION_INTERVAL_MINUTES,
            OPTIMIZATION_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        // 记录启动时间
        performanceMonitor.recordAppStartupTime(System.currentTimeMillis());
        
        Log.i(TAG, "Performance optimization started");
    }
    
    /**
     * 停止性能优化
     */
    public void stopOptimization() {
        Log.i(TAG, "Stopping performance optimization");
        
        // 停止各个组件的监控
        if (memoryManager != null) {
            memoryManager.stopMonitoring();
        }
        
        if (batteryOptimizer != null) {
            batteryOptimizer.stopMonitoring();
        }
        
        if (leakDetector != null) {
            leakDetector.stopDetection();
        }
        
        Log.i(TAG, "Performance optimization stopped");
    }
    
    /**
     * 设置优化级别
     */
    public void setOptimizationLevel(OptimizationLevel level) {
        if (level == currentLevel) {
            return;
        }
        
        OptimizationLevel oldLevel = currentLevel;
        currentLevel = level;
        
        Log.i(TAG, "Optimization level changed: " + oldLevel + " -> " + currentLevel);
        
        // 应用新的优化配置
        applyOptimizationLevel();
    }
    
    /**
     * 应用优化级别配置
     */
    private void applyOptimizationLevel() {
        switch (currentLevel) {
            case PERFORMANCE:
                applyPerformanceOptimizations();
                break;
            case BALANCED:
                applyBalancedOptimizations();
                break;
            case BATTERY:
                applyBatteryOptimizations();
                break;
        }
    }
    
    /**
     * 应用性能优先优化
     */
    private void applyPerformanceOptimizations() {
        Log.i(TAG, "Applying performance-first optimizations");
        
        // 禁用激进的电池优化
        isAggressiveOptimization = false;
        
        // 优化线程池配置以提高性能
        threadPoolManager.optimizeThreadPools();
        
        // 减少内存监控频率以节省CPU
        // 可以在这里添加更多性能优化配置
    }
    
    /**
     * 应用平衡优化
     */
    private void applyBalancedOptimizations() {
        Log.i(TAG, "Applying balanced optimizations");
        
        // 平衡性能和电池使用
        isAggressiveOptimization = false;
        
        // 使用默认的优化配置
        threadPoolManager.optimizeThreadPools();
    }
    
    /**
     * 应用电池优先优化
     */
    private void applyBatteryOptimizations() {
        Log.i(TAG, "Applying battery-first optimizations");
        
        // 启用激进的电池优化
        isAggressiveOptimization = true;
        
        // 优化线程池以节省电池
        threadPoolManager.optimizeThreadPools();
        
        // 启用更频繁的内存清理
        if (memoryManager != null) {
            memoryManager.performMemoryCleanup();
        }
    }
    
    /**
     * 执行定期优化
     */
    private void performPeriodicOptimization() {
        try {
            Log.d(TAG, "Performing periodic optimization");
            
            List<String> actions = new ArrayList<>();
            
            // 检查内存状态
            if (memoryManager != null && memoryManager.isLowMemory()) {
                memoryManager.performMemoryCleanup();
                actions.add("Memory cleanup performed");
            }
            
            // 检查线程池健康状态
            if (threadPoolManager != null && !threadPoolManager.isHealthy()) {
                threadPoolManager.optimizeThreadPools();
                actions.add("Thread pools optimized");
            }
            
            // 清理空闲线程
            if (threadPoolManager != null) {
                threadPoolManager.purgeIdleThreads();
                actions.add("Idle threads purged");
            }
            
            // 检查内存泄漏
            if (leakDetector != null) {
                List<MemoryLeakDetector.LeakReport> leaks = leakDetector.forceLeakCheck();
                if (!leaks.isEmpty()) {
                    actions.add("Memory leaks detected: " + leaks.size());
                    
                    // 记录泄漏信息
                    for (MemoryLeakDetector.LeakReport leak : leaks) {
                        LogManager.getInstance().logWarning(TAG, "Memory leak: " + leak.toString());
                    }
                }
            }
            
            // 根据电池状态调整优化策略
            if (batteryOptimizer != null) {
                BatteryOptimizer.PowerMode powerMode = batteryOptimizer.getCurrentPowerMode();
                if (powerMode == BatteryOptimizer.PowerMode.POWER_SAVE && currentLevel != OptimizationLevel.BATTERY) {
                    setOptimizationLevel(OptimizationLevel.BATTERY);
                    actions.add("Switched to battery optimization mode");
                } else if (powerMode == BatteryOptimizer.PowerMode.NORMAL && currentLevel == OptimizationLevel.BATTERY) {
                    setOptimizationLevel(OptimizationLevel.BALANCED);
                    actions.add("Switched to balanced optimization mode");
                }
            }
            
            if (!actions.isEmpty()) {
                Log.i(TAG, "Periodic optimization completed: " + actions);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during periodic optimization", e);
        }
    }
    
    /**
     * 执行全面优化
     */
    public OptimizationReport performFullOptimization() {
        Log.i(TAG, "Performing full optimization");
        
        long startTime = System.currentTimeMillis();
        List<String> actions = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        try {
            // 内存优化
            if (memoryManager != null) {
                if (memoryManager.isLowMemory()) {
                    memoryManager.performEmergencyCleanup();
                    actions.add("Emergency memory cleanup performed");
                } else {
                    memoryManager.performMemoryCleanup();
                    actions.add("Regular memory cleanup performed");
                }
            }
            
            // 线程池优化
            if (threadPoolManager != null) {
                threadPoolManager.optimizeThreadPools();
                threadPoolManager.purgeIdleThreads();
                actions.add("Thread pools optimized and idle threads purged");
            }
            
            // 检查性能要求
            if (performanceMonitor != null) {
                if (!performanceMonitor.checkPerformanceRequirements()) {
                    recommendations.add("Performance below requirements - consider optimization");
                }
                
                List<String> warnings = performanceMonitor.getPerformanceWarnings();
                recommendations.addAll(warnings);
            }
            
            // 电池优化建议
            if (batteryOptimizer != null) {
                List<String> batteryTips = batteryOptimizer.getPowerSavingTips();
                recommendations.addAll(batteryTips);
            }
            
            // 内存泄漏检查
            if (leakDetector != null) {
                List<MemoryLeakDetector.LeakReport> leaks = leakDetector.forceLeakCheck();
                if (!leaks.isEmpty()) {
                    actions.add("Memory leaks detected and reported: " + leaks.size());
                    recommendations.add("Fix memory leaks to improve performance");
                }
            }
            
            long optimizationTime = System.currentTimeMillis() - startTime;
            performanceMonitor.recordMeasurement("full_optimization", optimizationTime);
            
            Log.i(TAG, "Full optimization completed in " + optimizationTime + "ms");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during full optimization", e);
            actions.add("Optimization error: " + e.getMessage());
        }
        
        // 生成优化报告
        return generateOptimizationReport(actions, recommendations);
    }
    
    /**
     * 生成优化报告
     */
    private OptimizationReport generateOptimizationReport(List<String> actions, List<String> recommendations) {
        String memoryStatus = memoryManager != null ? memoryManager.getMemoryStatusInfo() : "Memory manager not available";
        String batteryStatus = batteryOptimizer != null ? batteryOptimizer.getBatteryStatusInfo() : "Battery optimizer not available";
        String performanceStatus = performanceMonitor != null ? performanceMonitor.getKPISummary() : "Performance monitor not available";
        
        return new OptimizationReport(
            System.currentTimeMillis(),
            memoryStatus,
            batteryStatus,
            performanceStatus,
            actions,
            recommendations
        );
    }
    
    /**
     * 获取优化状态摘要
     */
    public String getOptimizationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Optimization Summary:\n");
        summary.append("================================\n");
        summary.append("Optimization Level: ").append(currentLevel).append("\n");
        summary.append("Aggressive Mode: ").append(isAggressiveOptimization).append("\n\n");
        
        if (memoryManager != null) {
            summary.append(memoryManager.getMemoryStatusInfo()).append("\n\n");
        }
        
        if (batteryOptimizer != null) {
            summary.append(batteryOptimizer.getBatteryStatusInfo()).append("\n\n");
        }
        
        if (performanceMonitor != null) {
            summary.append(performanceMonitor.getKPISummary()).append("\n\n");
        }
        
        if (threadPoolManager != null) {
            summary.append(threadPoolManager.getAllPoolsStatus()).append("\n");
        }
        
        return summary.toString();
    }
    
    // MemoryManager.MemoryListener 实现
    
    @Override
    public void onLowMemory(long availableMemory) {
        Log.w(TAG, "Low memory detected: " + availableMemory + " bytes");
        
        if (currentLevel != OptimizationLevel.BATTERY) {
            // 临时切换到电池优化模式以节省内存
            applyBatteryOptimizations();
        }
    }
    
    @Override
    public void onCriticalMemory(long availableMemory) {
        Log.e(TAG, "Critical memory situation: " + availableMemory + " bytes");
        
        // 执行紧急优化
        if (memoryManager != null) {
            memoryManager.performEmergencyCleanup();
        }
        
        if (threadPoolManager != null) {
            threadPoolManager.optimizeThreadPools();
        }
    }
    
    @Override
    public void onMemoryRecovered(long availableMemory) {
        Log.i(TAG, "Memory recovered: " + availableMemory + " bytes");
        
        // 恢复正常优化级别
        applyOptimizationLevel();
    }
    
    // BatteryOptimizer.BatteryListener 实现
    
    @Override
    public void onBatteryLevelChanged(int level, boolean isCharging) {
        Log.d(TAG, "Battery level changed: " + level + "%, charging: " + isCharging);
        
        // 根据电池状态调整优化策略
        if (!isCharging && level <= 20 && currentLevel != OptimizationLevel.BATTERY) {
            setOptimizationLevel(OptimizationLevel.BATTERY);
        } else if (isCharging && level > 50 && currentLevel == OptimizationLevel.BATTERY) {
            setOptimizationLevel(OptimizationLevel.BALANCED);
        }
    }
    
    @Override
    public void onLowBattery(int level) {
        Log.w(TAG, "Low battery: " + level + "%");
        
        // 强制切换到电池优化模式
        if (currentLevel != OptimizationLevel.BATTERY) {
            setOptimizationLevel(OptimizationLevel.BATTERY);
        }
    }
    
    @Override
    public void onCriticalBattery(int level) {
        Log.e(TAG, "Critical battery: " + level + "%");
        
        // 启用最激进的电池优化
        isAggressiveOptimization = true;
        applyBatteryOptimizations();
    }
    
    @Override
    public void onPowerModeChanged(BatteryOptimizer.PowerMode mode) {
        Log.i(TAG, "Power mode changed: " + mode);
        
        // 根据电源模式调整优化级别
        switch (mode) {
            case NORMAL:
                if (currentLevel == OptimizationLevel.BATTERY) {
                    setOptimizationLevel(OptimizationLevel.BALANCED);
                }
                break;
            case POWER_SAVE:
                setOptimizationLevel(OptimizationLevel.BATTERY);
                break;
            case ULTRA_SAVE:
                setOptimizationLevel(OptimizationLevel.BATTERY);
                isAggressiveOptimization = true;
                break;
        }
    }
    
    /**
     * 获取智能录音检测器
     */
    public SmartRecordingDetector getSmartRecordingDetector() {
        return recordingDetector;
    }
    
    /**
     * 启用/禁用优化
     */
    public void setOptimizationEnabled(boolean enabled) {
        isOptimizationEnabled = enabled;
        Log.i(TAG, "Optimization " + (enabled ? "enabled" : "disabled"));
        
        if (enabled) {
            startOptimization();
        } else {
            stopOptimization();
        }
    }
    
    /**
     * 检查优化是否启用
     */
    public boolean isOptimizationEnabled() {
        return isOptimizationEnabled;
    }
    
    /**
     * 获取当前优化级别
     */
    public OptimizationLevel getCurrentOptimizationLevel() {
        return currentLevel;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.i(TAG, "Releasing PerformanceOptimizer resources");
        
        stopOptimization();
        
        if (optimizationScheduler != null && !optimizationScheduler.isShutdown()) {
            optimizationScheduler.shutdown();
            try {
                if (!optimizationScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    optimizationScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                optimizationScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 释放各个组件
        if (memoryManager != null) {
            memoryManager.removeMemoryListener(this);
            memoryManager.release();
        }
        
        if (batteryOptimizer != null) {
            batteryOptimizer.removeBatteryListener(this);
            batteryOptimizer.release();
        }
        
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }
        
        if (leakDetector != null) {
            leakDetector.release();
        }
        
        Log.i(TAG, "PerformanceOptimizer resources released");
    }
}