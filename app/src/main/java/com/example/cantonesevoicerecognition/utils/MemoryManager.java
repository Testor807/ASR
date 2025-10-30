package com.example.cantonesevoicerecognition.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存管理器
 * 负责监控和优化应用内存使用
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";
    
    // 内存阈值常量
    private static final long LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB
    private static final int MEMORY_CHECK_INTERVAL = 30; // 30秒
    
    private static MemoryManager instance;
    private final Context context;
    private final ActivityManager activityManager;
    private final List<WeakReference<MemoryListener>> listeners;
    private final ScheduledExecutorService scheduler;
    
    private boolean isMonitoring = false;
    private long lastGcTime = 0;
    private final Object lock = new Object();
    
    /**
     * 内存状态监听器
     */
    public interface MemoryListener {
        void onLowMemory(long availableMemory);
        void onCriticalMemory(long availableMemory);
        void onMemoryRecovered(long availableMemory);
    }
    
    private MemoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.listeners = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        Log.i(TAG, "MemoryManager initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized MemoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new MemoryManager(context);
        }
        return instance;
    }
    
    /**
     * 开始内存监控
     */
    public void startMonitoring() {
        synchronized (lock) {
            if (isMonitoring) {
                Log.w(TAG, "Memory monitoring already started");
                return;
            }
            
            isMonitoring = true;
            scheduler.scheduleAtFixedRate(this::checkMemoryStatus, 
                                        0, MEMORY_CHECK_INTERVAL, TimeUnit.SECONDS);
            
            Log.i(TAG, "Memory monitoring started");
        }
    }
    
    /**
     * 停止内存监控
     */
    public void stopMonitoring() {
        synchronized (lock) {
            if (!isMonitoring) {
                return;
            }
            
            isMonitoring = false;
            Log.i(TAG, "Memory monitoring stopped");
        }
    }
    
    /**
     * 添加内存监听器
     */
    public void addMemoryListener(MemoryListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.add(new WeakReference<>(listener));
            }
        }
    }
    
    /**
     * 移除内存监听器
     */
    public void removeMemoryListener(MemoryListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.removeIf(ref -> {
                    MemoryListener l = ref.get();
                    return l == null || l == listener;
                });
            }
        }
    }
    
    /**
     * 检查内存状态
     */
    private void checkMemoryStatus() {
        try {
            long availableMemory = getAvailableMemory();
            long usedMemory = getUsedMemory();
            
            Log.d(TAG, String.format("Memory status - Available: %d MB, Used: %d MB", 
                                   availableMemory / (1024 * 1024), usedMemory / (1024 * 1024)));
            
            // 检查内存状态并通知监听器
            if (availableMemory <= CRITICAL_MEMORY_THRESHOLD) {
                Log.w(TAG, "Critical memory situation detected");
                notifyListeners(listener -> listener.onCriticalMemory(availableMemory));
                performEmergencyCleanup();
                
            } else if (availableMemory <= LOW_MEMORY_THRESHOLD) {
                Log.w(TAG, "Low memory situation detected");
                notifyListeners(listener -> listener.onLowMemory(availableMemory));
                performMemoryCleanup();
                
            } else if (availableMemory > LOW_MEMORY_THRESHOLD * 1.5) {
                // 内存恢复正常
                notifyListeners(listener -> listener.onMemoryRecovered(availableMemory));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking memory status", e);
        }
    }
    
    /**
     * 获取可用内存
     */
    public long getAvailableMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }
    
    /**
     * 获取已使用内存
     */
    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 获取最大可用内存
     */
    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }
    
    /**
     * 获取内存使用率
     */
    public float getMemoryUsagePercentage() {
        long used = getUsedMemory();
        long max = getMaxMemory();
        return (float) used / max * 100;
    }
    
    /**
     * 执行内存清理
     */
    public void performMemoryCleanup() {
        Log.i(TAG, "Performing memory cleanup");
        
        try {
            // 建议垃圾回收
            suggestGarbageCollection();
            
            // 清理弱引用
            cleanupWeakReferences();
            
            // 通知应用组件进行清理
            notifyComponentsToCleanup();
            
            Log.i(TAG, "Memory cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during memory cleanup", e);
        }
    }
    
    /**
     * 执行紧急内存清理
     */
    public void performEmergencyCleanup() {
        Log.w(TAG, "Performing emergency memory cleanup");
        
        try {
            // 执行常规清理
            performMemoryCleanup();
            
            // 强制垃圾回收
            forceGarbageCollection();
            
            // 清理所有可清理的缓存
            clearAllCaches();
            
            Log.w(TAG, "Emergency memory cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during emergency cleanup", e);
        }
    }
    
    /**
     * 建议垃圾回收
     */
    private void suggestGarbageCollection() {
        long currentTime = System.currentTimeMillis();
        
        // 避免频繁GC，至少间隔10秒
        if (currentTime - lastGcTime > 10000) {
            System.gc();
            lastGcTime = currentTime;
            Log.d(TAG, "Garbage collection suggested");
        }
    }
    
    /**
     * 强制垃圾回收
     */
    private void forceGarbageCollection() {
        System.gc();
        System.runFinalization();
        System.gc();
        lastGcTime = System.currentTimeMillis();
        Log.d(TAG, "Forced garbage collection");
    }
    
    /**
     * 清理弱引用
     */
    private void cleanupWeakReferences() {
        synchronized (listeners) {
            listeners.removeIf(ref -> ref.get() == null);
        }
    }
    
    /**
     * 通知组件进行清理
     */
    private void notifyComponentsToCleanup() {
        // 发送内存清理广播
        context.sendBroadcast(new android.content.Intent("com.example.cantonesevoicerecognition.MEMORY_CLEANUP"));
    }
    
    /**
     * 清理所有缓存
     */
    private void clearAllCaches() {
        // 这里可以添加具体的缓存清理逻辑
        Log.d(TAG, "Clearing all caches");
    }
    
    /**
     * 通知监听器
     */
    private void notifyListeners(ListenerAction action) {
        synchronized (listeners) {
            List<WeakReference<MemoryListener>> toRemove = new ArrayList<>();
            
            for (WeakReference<MemoryListener> ref : listeners) {
                MemoryListener listener = ref.get();
                if (listener == null) {
                    toRemove.add(ref);
                } else {
                    try {
                        action.execute(listener);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying memory listener", e);
                    }
                }
            }
            
            // 清理无效引用
            listeners.removeAll(toRemove);
        }
    }
    
    /**
     * 监听器动作接口
     */
    private interface ListenerAction {
        void execute(MemoryListener listener);
    }
    
    /**
     * 获取内存状态信息
     */
    public String getMemoryStatusInfo() {
        long available = getAvailableMemory();
        long used = getUsedMemory();
        long max = getMaxMemory();
        float usage = getMemoryUsagePercentage();
        
        return String.format("Memory Status - Available: %d MB, Used: %d MB, Max: %d MB, Usage: %.1f%%",
                           available / (1024 * 1024),
                           used / (1024 * 1024),
                           max / (1024 * 1024),
                           usage);
    }
    
    /**
     * 检查是否处于低内存状态
     */
    public boolean isLowMemory() {
        return getAvailableMemory() <= LOW_MEMORY_THRESHOLD;
    }
    
    /**
     * 检查是否处于临界内存状态
     */
    public boolean isCriticalMemory() {
        return getAvailableMemory() <= CRITICAL_MEMORY_THRESHOLD;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopMonitoring();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        synchronized (listeners) {
            listeners.clear();
        }
        
        Log.i(TAG, "MemoryManager released");
    }
}