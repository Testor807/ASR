package com.example.cantonesevoicerecognition.utils;

import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存泄漏检测器
 * 用于检测和报告潜在的内存泄漏
 */
public class MemoryLeakDetector {
    private static final String TAG = "MemoryLeakDetector";
    
    // 检测配置
    private static final int CHECK_INTERVAL_SECONDS = 60; // 检测间隔
    private static final int LEAK_THRESHOLD_COUNT = 5; // 泄漏阈值
    private static final long OBJECT_LIFETIME_THRESHOLD = 5 * 60 * 1000; // 5分钟
    
    private static MemoryLeakDetector instance;
    private final Map<String, List<ObjectTracker>> trackedObjects;
    private final ScheduledExecutorService scheduler;
    private boolean isDetectionEnabled = false;
    
    /**
     * 对象跟踪器
     */
    private static class ObjectTracker {
        final WeakReference<Object> objectRef;
        final long creationTime;
        final String stackTrace;
        
        ObjectTracker(Object object, String stackTrace) {
            this.objectRef = new WeakReference<>(object);
            this.creationTime = System.currentTimeMillis();
            this.stackTrace = stackTrace;
        }
        
        boolean isAlive() {
            return objectRef.get() != null;
        }
        
        long getAge() {
            return System.currentTimeMillis() - creationTime;
        }
    }
    
    /**
     * 泄漏报告
     */
    public static class LeakReport {
        public final String className;
        public final int leakedCount;
        public final long averageAge;
        public final List<String> stackTraces;
        
        LeakReport(String className, int leakedCount, long averageAge, List<String> stackTraces) {
            this.className = className;
            this.leakedCount = leakedCount;
            this.averageAge = averageAge;
            this.stackTraces = new ArrayList<>(stackTraces);
        }
        
        @Override
        public String toString() {
            return String.format("LeakReport{class=%s, count=%d, avgAge=%dms}", 
                               className, leakedCount, averageAge);
        }
    }
    
    private MemoryLeakDetector() {
        this.trackedObjects = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        Log.i(TAG, "MemoryLeakDetector initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized MemoryLeakDetector getInstance() {
        if (instance == null) {
            instance = new MemoryLeakDetector();
        }
        return instance;
    }
    
    /**
     * 开始泄漏检测
     */
    public void startDetection() {
        if (isDetectionEnabled) {
            Log.w(TAG, "Leak detection already enabled");
            return;
        }
        
        isDetectionEnabled = true;
        scheduler.scheduleAtFixedRate(this::performLeakCheck, 
                                    CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        Log.i(TAG, "Memory leak detection started");
    }
    
    /**
     * 停止泄漏检测
     */
    public void stopDetection() {
        if (!isDetectionEnabled) {
            return;
        }
        
        isDetectionEnabled = false;
        Log.i(TAG, "Memory leak detection stopped");
    }
    
    /**
     * 跟踪对象
     */
    public void trackObject(Object object, String tag) {
        if (!isDetectionEnabled || object == null) {
            return;
        }
        
        String className = object.getClass().getSimpleName();
        String key = tag != null ? tag : className;
        
        // 获取调用栈信息
        String stackTrace = getSimplifiedStackTrace();
        
        ObjectTracker tracker = new ObjectTracker(object, stackTrace);
        
        trackedObjects.computeIfAbsent(key, k -> new ArrayList<>()).add(tracker);
        
        Log.d(TAG, "Tracking object: " + key);
    }
    
    /**
     * 跟踪对象（使用类名作为标签）
     */
    public void trackObject(Object object) {
        trackObject(object, null);
    }
    
    /**
     * 停止跟踪对象
     */
    public void untrackObject(Object object, String tag) {
        if (object == null) {
            return;
        }
        
        String className = object.getClass().getSimpleName();
        String key = tag != null ? tag : className;
        
        List<ObjectTracker> trackers = trackedObjects.get(key);
        if (trackers != null) {
            trackers.removeIf(tracker -> tracker.objectRef.get() == object);
            
            if (trackers.isEmpty()) {
                trackedObjects.remove(key);
            }
        }
        
        Log.d(TAG, "Untracking object: " + key);
    }
    
    /**
     * 停止跟踪对象（使用类名作为标签）
     */
    public void untrackObject(Object object) {
        untrackObject(object, null);
    }
    
    /**
     * 执行泄漏检查
     */
    private void performLeakCheck() {
        try {
            Log.d(TAG, "Performing leak check...");
            
            List<LeakReport> leakReports = new ArrayList<>();
            
            for (Map.Entry<String, List<ObjectTracker>> entry : trackedObjects.entrySet()) {
                String className = entry.getKey();
                List<ObjectTracker> trackers = entry.getValue();
                
                // 清理已被回收的对象
                Iterator<ObjectTracker> iterator = trackers.iterator();
                List<ObjectTracker> leakedObjects = new ArrayList<>();
                
                while (iterator.hasNext()) {
                    ObjectTracker tracker = iterator.next();
                    
                    if (!tracker.isAlive()) {
                        // 对象已被回收，移除跟踪
                        iterator.remove();
                    } else if (tracker.getAge() > OBJECT_LIFETIME_THRESHOLD) {
                        // 对象存活时间过长，可能泄漏
                        leakedObjects.add(tracker);
                    }
                }
                
                // 检查是否存在泄漏
                if (leakedObjects.size() >= LEAK_THRESHOLD_COUNT) {
                    long totalAge = leakedObjects.stream().mapToLong(ObjectTracker::getAge).sum();
                    long averageAge = totalAge / leakedObjects.size();
                    
                    List<String> stackTraces = new ArrayList<>();
                    for (ObjectTracker tracker : leakedObjects) {
                        stackTraces.add(tracker.stackTrace);
                    }
                    
                    LeakReport report = new LeakReport(className, leakedObjects.size(), averageAge, stackTraces);
                    leakReports.add(report);
                }
                
                // 清理空的跟踪列表
                if (trackers.isEmpty()) {
                    trackedObjects.remove(className);
                }
            }
            
            // 报告泄漏
            if (!leakReports.isEmpty()) {
                reportLeaks(leakReports);
            }
            
            Log.d(TAG, "Leak check completed. Tracking " + getTotalTrackedCount() + " objects");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during leak check", e);
        }
    }
    
    /**
     * 报告内存泄漏
     */
    private void reportLeaks(List<LeakReport> leakReports) {
        Log.w(TAG, "Memory leaks detected:");
        
        for (LeakReport report : leakReports) {
            Log.w(TAG, "LEAK: " + report.toString());
            
            // 记录详细信息到日志
            LogManager.getInstance().logWarning(TAG, "Memory leak detected: " + report.toString());
            
            // 可以在这里添加更多的泄漏处理逻辑，比如：
            // - 发送崩溃报告
            // - 通知开发者
            // - 尝试自动清理
        }
    }
    
    /**
     * 获取简化的调用栈信息
     */
    private String getSimplifiedStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        // 跳过前几个系统调用，获取有用的调用信息
        for (int i = 4; i < Math.min(stackTrace.length, 8); i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().contains("com.example.cantonesevoicerecognition")) {
                sb.append(element.getClassName()).append(".").append(element.getMethodName())
                  .append("(").append(element.getFileName()).append(":").append(element.getLineNumber()).append(")\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取当前跟踪的对象总数
     */
    public int getTotalTrackedCount() {
        return trackedObjects.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * 获取跟踪统计信息
     */
    public String getTrackingStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Leak Detection Stats:\n");
        sb.append("Total tracked objects: ").append(getTotalTrackedCount()).append("\n");
        sb.append("Tracked classes: ").append(trackedObjects.size()).append("\n");
        
        for (Map.Entry<String, List<ObjectTracker>> entry : trackedObjects.entrySet()) {
            String className = entry.getKey();
            List<ObjectTracker> trackers = entry.getValue();
            
            long aliveCount = trackers.stream().mapToLong(t -> t.isAlive() ? 1 : 0).sum();
            long averageAge = trackers.stream().mapToLong(ObjectTracker::getAge).sum() / Math.max(1, trackers.size());
            
            sb.append("  ").append(className).append(": ").append(aliveCount)
              .append(" alive, avg age: ").append(averageAge).append("ms\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 强制执行泄漏检查
     */
    public List<LeakReport> forceLeakCheck() {
        List<LeakReport> leakReports = new ArrayList<>();
        
        for (Map.Entry<String, List<ObjectTracker>> entry : trackedObjects.entrySet()) {
            String className = entry.getKey();
            List<ObjectTracker> trackers = entry.getValue();
            
            List<ObjectTracker> suspiciousObjects = new ArrayList<>();
            
            for (ObjectTracker tracker : trackers) {
                if (tracker.isAlive() && tracker.getAge() > OBJECT_LIFETIME_THRESHOLD / 2) {
                    suspiciousObjects.add(tracker);
                }
            }
            
            if (suspiciousObjects.size() >= LEAK_THRESHOLD_COUNT / 2) {
                long totalAge = suspiciousObjects.stream().mapToLong(ObjectTracker::getAge).sum();
                long averageAge = totalAge / suspiciousObjects.size();
                
                List<String> stackTraces = new ArrayList<>();
                for (ObjectTracker tracker : suspiciousObjects) {
                    stackTraces.add(tracker.stackTrace);
                }
                
                LeakReport report = new LeakReport(className, suspiciousObjects.size(), averageAge, stackTraces);
                leakReports.add(report);
            }
        }
        
        return leakReports;
    }
    
    /**
     * 清理所有跟踪信息
     */
    public void clearAllTracking() {
        trackedObjects.clear();
        Log.i(TAG, "All tracking information cleared");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopDetection();
        
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
        
        clearAllTracking();
        Log.i(TAG, "MemoryLeakDetector released");
    }
}