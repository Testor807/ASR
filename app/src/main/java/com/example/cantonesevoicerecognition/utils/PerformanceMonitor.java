package com.example.cantonesevoicerecognition.utils;

import android.os.SystemClock;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 * 用于监控和分析应用性能指标
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    private static PerformanceMonitor instance;
    private final Map<String, PerformanceMetric> metrics;
    private final Map<String, Long> startTimes;
    private boolean isEnabled = true;
    
    /**
     * 性能指标类
     */
    public static class PerformanceMetric {
        private final String name;
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final List<Long> recentTimes = new ArrayList<>();
        private final Object lock = new Object();
        
        public PerformanceMetric(String name) {
            this.name = name;
        }
        
        public void addMeasurement(long duration) {
            totalTime.addAndGet(duration);
            callCount.incrementAndGet();
            
            // 更新最小值
            long currentMin = minTime.get();
            while (duration < currentMin && !minTime.compareAndSet(currentMin, duration)) {
                currentMin = minTime.get();
            }
            
            // 更新最大值
            long currentMax = maxTime.get();
            while (duration > currentMax && !maxTime.compareAndSet(currentMax, duration)) {
                currentMax = maxTime.get();
            }
            
            // 保存最近的测量值（用于计算标准差等）
            synchronized (lock) {
                recentTimes.add(duration);
                if (recentTimes.size() > 100) { // 只保留最近100次测量
                    recentTimes.remove(0);
                }
            }
        }
        
        public double getAverageTime() {
            long count = callCount.get();
            return count > 0 ? (double) totalTime.get() / count : 0.0;
        }
        
        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public long getMaxTime() {
            return maxTime.get();
        }
        
        public long getCallCount() {
            return callCount.get();
        }
        
        public long getTotalTime() {
            return totalTime.get();
        }
        
        public double getStandardDeviation() {
            synchronized (lock) {
                if (recentTimes.size() < 2) {
                    return 0.0;
                }
                
                double mean = recentTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double variance = recentTimes.stream()
                    .mapToDouble(time -> Math.pow(time - mean, 2))
                    .average().orElse(0.0);
                
                return Math.sqrt(variance);
            }
        }
        
        public String getFormattedStats() {
            return String.format("%s: avg=%.2fms, min=%dms, max=%dms, count=%d, std=%.2fms",
                               name, getAverageTime(), getMinTime(), getMaxTime(), 
                               getCallCount(), getStandardDeviation());
        }
        
        public void reset() {
            totalTime.set(0);
            callCount.set(0);
            minTime.set(Long.MAX_VALUE);
            maxTime.set(0);
            synchronized (lock) {
                recentTimes.clear();
            }
        }
    }
    
    private PerformanceMonitor() {
        this.metrics = new ConcurrentHashMap<>();
        this.startTimes = new ConcurrentHashMap<>();
        Log.i(TAG, "PerformanceMonitor initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * 启用性能监控
     */
    public void enable() {
        isEnabled = true;
        Log.i(TAG, "Performance monitoring enabled");
    }
    
    /**
     * 禁用性能监控
     */
    public void disable() {
        isEnabled = false;
        Log.i(TAG, "Performance monitoring disabled");
    }
    
    /**
     * 开始测量
     */
    public void startMeasurement(String operationName) {
        if (!isEnabled) {
            return;
        }
        
        long startTime = SystemClock.elapsedRealtime();
        startTimes.put(operationName, startTime);
        
        Log.d(TAG, "Started measuring: " + operationName);
    }
    
    /**
     * 结束测量
     */
    public void endMeasurement(String operationName) {
        if (!isEnabled) {
            return;
        }
        
        long endTime = SystemClock.elapsedRealtime();
        Long startTime = startTimes.remove(operationName);
        
        if (startTime != null) {
            long duration = endTime - startTime;
            recordMeasurement(operationName, duration);
            
            Log.d(TAG, String.format("Completed measuring: %s (took %dms)", operationName, duration));
        } else {
            Log.w(TAG, "No start time found for operation: " + operationName);
        }
    }
    
    /**
     * 记录测量结果
     */
    public void recordMeasurement(String operationName, long duration) {
        if (!isEnabled) {
            return;
        }
        
        PerformanceMetric metric = metrics.computeIfAbsent(operationName, PerformanceMetric::new);
        metric.addMeasurement(duration);
    }
    
    /**
     * 测量代码块执行时间
     */
    public void measureBlock(String operationName, Runnable block) {
        if (!isEnabled) {
            block.run();
            return;
        }
        
        long startTime = SystemClock.elapsedRealtime();
        try {
            block.run();
        } finally {
            long duration = SystemClock.elapsedRealtime() - startTime;
            recordMeasurement(operationName, duration);
        }
    }
    
    /**
     * 获取性能指标
     */
    public PerformanceMetric getMetric(String operationName) {
        return metrics.get(operationName);
    }
    
    /**
     * 获取所有性能指标
     */
    public Map<String, PerformanceMetric> getAllMetrics() {
        return new HashMap<>(metrics);
    }
    
    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        if (metrics.isEmpty()) {
            return "No performance data available";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Performance Report:\n");
        report.append("==================\n");
        
        for (PerformanceMetric metric : metrics.values()) {
            report.append(metric.getFormattedStats()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 获取关键性能指标摘要
     */
    public String getKPISummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Key Performance Indicators:\n");
        
        // 转录相关指标
        PerformanceMetric transcription = metrics.get("audio_transcription");
        if (transcription != null) {
            summary.append(String.format("Audio Transcription: avg=%.2fms, calls=%d\n",
                                        transcription.getAverageTime(), transcription.getCallCount()));
        }
        
        // 音频处理指标
        PerformanceMetric audioProcessing = metrics.get("audio_processing");
        if (audioProcessing != null) {
            summary.append(String.format("Audio Processing: avg=%.2fms, calls=%d\n",
                                        audioProcessing.getAverageTime(), audioProcessing.getCallCount()));
        }
        
        // 实时转录指标
        PerformanceMetric realTime = metrics.get("real_time_transcription");
        if (realTime != null) {
            summary.append(String.format("Real-time Transcription: avg=%.2fms, calls=%d\n",
                                        realTime.getAverageTime(), realTime.getCallCount()));
        }
        
        // 模型加载指标
        PerformanceMetric modelLoading = metrics.get("model_loading");
        if (modelLoading != null) {
            summary.append(String.format("Model Loading: avg=%.2fms, calls=%d\n",
                                        modelLoading.getAverageTime(), modelLoading.getCallCount()));
        }
        
        return summary.toString();
    }
    
    /**
     * 检查性能是否符合要求
     */
    public boolean checkPerformanceRequirements() {
        boolean meetsRequirements = true;
        
        // 检查转录响应时间 < 5秒
        PerformanceMetric transcription = metrics.get("audio_transcription");
        if (transcription != null && transcription.getAverageTime() > 5000) {
            Log.w(TAG, "Transcription performance below requirements: " + transcription.getAverageTime() + "ms");
            meetsRequirements = false;
        }
        
        // 检查应用启动时间 < 3秒
        PerformanceMetric appStartup = metrics.get("app_startup");
        if (appStartup != null && appStartup.getAverageTime() > 3000) {
            Log.w(TAG, "App startup performance below requirements: " + appStartup.getAverageTime() + "ms");
            meetsRequirements = false;
        }
        
        // 检查实时转录延迟 < 2秒
        PerformanceMetric realTime = metrics.get("real_time_transcription");
        if (realTime != null && realTime.getAverageTime() > 2000) {
            Log.w(TAG, "Real-time transcription performance below requirements: " + realTime.getAverageTime() + "ms");
            meetsRequirements = false;
        }
        
        return meetsRequirements;
    }
    
    /**
     * 获取性能警告
     */
    public List<String> getPerformanceWarnings() {
        List<String> warnings = new ArrayList<>();
        
        for (PerformanceMetric metric : metrics.values()) {
            // 检查平均时间是否过长
            if (metric.getAverageTime() > 1000) { // 超过1秒
                warnings.add(String.format("Operation '%s' is slow: avg=%.2fms", 
                                          metric.name, metric.getAverageTime()));
            }
            
            // 检查标准差是否过大（性能不稳定）
            if (metric.getStandardDeviation() > metric.getAverageTime() * 0.5) {
                warnings.add(String.format("Operation '%s' has unstable performance: std=%.2fms", 
                                          metric.name, metric.getStandardDeviation()));
            }
            
            // 检查最大时间是否异常
            if (metric.getMaxTime() > metric.getAverageTime() * 3) {
                warnings.add(String.format("Operation '%s' has performance spikes: max=%dms", 
                                          metric.name, metric.getMaxTime()));
            }
        }
        
        return warnings;
    }
    
    /**
     * 重置所有指标
     */
    public void resetAllMetrics() {
        for (PerformanceMetric metric : metrics.values()) {
            metric.reset();
        }
        startTimes.clear();
        Log.i(TAG, "All performance metrics reset");
    }
    
    /**
     * 重置特定指标
     */
    public void resetMetric(String operationName) {
        PerformanceMetric metric = metrics.get(operationName);
        if (metric != null) {
            metric.reset();
            Log.i(TAG, "Performance metric reset: " + operationName);
        }
    }
    
    /**
     * 导出性能数据
     */
    public Map<String, Object> exportPerformanceData() {
        Map<String, Object> data = new HashMap<>();
        
        for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
            String name = entry.getKey();
            PerformanceMetric metric = entry.getValue();
            
            Map<String, Object> metricData = new HashMap<>();
            metricData.put("averageTime", metric.getAverageTime());
            metricData.put("minTime", metric.getMinTime());
            metricData.put("maxTime", metric.getMaxTime());
            metricData.put("callCount", metric.getCallCount());
            metricData.put("totalTime", metric.getTotalTime());
            metricData.put("standardDeviation", metric.getStandardDeviation());
            
            data.put(name, metricData);
        }
        
        return data;
    }
    
    /**
     * 记录应用启动时间
     */
    public void recordAppStartupTime(long startupTime) {
        recordMeasurement("app_startup", startupTime);
        Log.i(TAG, "App startup time recorded: " + startupTime + "ms");
    }
    
    /**
     * 记录转录时间
     */
    public void recordTranscriptionTime(long transcriptionTime, int audioLengthSeconds) {
        recordMeasurement("audio_transcription", transcriptionTime);
        
        // 计算转录效率（实时倍数）
        if (audioLengthSeconds > 0) {
            double efficiency = (double) audioLengthSeconds * 1000 / transcriptionTime;
            recordMeasurement("transcription_efficiency", (long) (efficiency * 100)); // 存储为百分比
        }
        
        Log.i(TAG, String.format("Transcription time recorded: %dms for %ds audio", 
                                transcriptionTime, audioLengthSeconds));
    }
    
    /**
     * 获取转录效率统计
     */
    public String getTranscriptionEfficiencyStats() {
        PerformanceMetric efficiency = metrics.get("transcription_efficiency");
        if (efficiency == null) {
            return "No transcription efficiency data available";
        }
        
        double avgEfficiency = efficiency.getAverageTime() / 100.0; // 转换回倍数
        return String.format("Transcription Efficiency: %.2fx real-time (avg), %.2fx (min), %.2fx (max)",
                           avgEfficiency, efficiency.getMinTime() / 100.0, efficiency.getMaxTime() / 100.0);
    }
}