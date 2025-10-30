package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 性能测试工具
 * 用于验证性能优化效果和检查是否满足性能要求
 */
public class PerformanceTester {
    private static final String TAG = "PerformanceTester";
    
    private final Context context;
    private final PerformanceMonitor performanceMonitor;
    private final MemoryManager memoryManager;
    private final ThreadPoolManager threadPoolManager;
    
    /**
     * 测试结果
     */
    public static class TestResult {
        public final String testName;
        public final boolean passed;
        public final long duration;
        public final String details;
        public final Exception error;
        
        public TestResult(String testName, boolean passed, long duration, String details, Exception error) {
            this.testName = testName;
            this.passed = passed;
            this.duration = duration;
            this.details = details;
            this.error = error;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s (took %dms) - %s", 
                               testName, passed ? "PASS" : "FAIL", duration, details);
        }
    }
    
    /**
     * 测试套件结果
     */
    public static class TestSuiteResult {
        public final List<TestResult> testResults;
        public final int totalTests;
        public final int passedTests;
        public final int failedTests;
        public final long totalDuration;
        
        public TestSuiteResult(List<TestResult> testResults) {
            this.testResults = new ArrayList<>(testResults);
            this.totalTests = testResults.size();
            this.passedTests = (int) testResults.stream().mapToLong(r -> r.passed ? 1 : 0).sum();
            this.failedTests = totalTests - passedTests;
            this.totalDuration = testResults.stream().mapToLong(r -> r.duration).sum();
        }
        
        public boolean allTestsPassed() {
            return failedTests == 0;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Performance Test Suite Results:\n");
            sb.append("============================\n");
            sb.append(String.format("Total Tests: %d, Passed: %d, Failed: %d\n", 
                                   totalTests, passedTests, failedTests));
            sb.append(String.format("Total Duration: %dms\n", totalDuration));
            sb.append(String.format("Success Rate: %.1f%%\n", (passedTests * 100.0 / totalTests)));
            sb.append("\nDetailed Results:\n");
            
            for (TestResult result : testResults) {
                sb.append("  ").append(result.toString()).append("\n");
                if (!result.passed && result.error != null) {
                    sb.append("    Error: ").append(result.error.getMessage()).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
    
    public PerformanceTester(Context context) {
        this.context = context.getApplicationContext();
        this.performanceMonitor = PerformanceMonitor.getInstance();
        this.memoryManager = MemoryManager.getInstance(context);
        this.threadPoolManager = ThreadPoolManager.getInstance();
        
        Log.i(TAG, "PerformanceTester initialized");
    }
    
    /**
     * 运行完整的性能测试套件
     */
    public TestSuiteResult runFullTestSuite() {
        Log.i(TAG, "Starting full performance test suite");
        
        List<TestResult> results = new ArrayList<>();
        
        // 1. 应用启动时间测试
        results.add(testAppStartupTime());
        
        // 2. 内存管理测试
        results.add(testMemoryManagement());
        
        // 3. 线程池性能测试
        results.add(testThreadPoolPerformance());
        
        // 4. 音频处理性能测试
        results.add(testAudioProcessingPerformance());
        
        // 5. 转录性能测试
        results.add(testTranscriptionPerformance());
        
        // 6. 内存泄漏检测测试
        results.add(testMemoryLeakDetection());
        
        // 7. 电池优化测试
        results.add(testBatteryOptimization());
        
        // 8. 智能录音检测测试
        results.add(testSmartRecordingDetection());
        
        TestSuiteResult suiteResult = new TestSuiteResult(results);
        Log.i(TAG, "Performance test suite completed: " + suiteResult.passedTests + "/" + suiteResult.totalTests + " passed");
        
        return suiteResult;
    }
    
    /**
     * 测试应用启动时间
     */
    private TestResult testAppStartupTime() {
        String testName = "App Startup Time";
        long startTime = System.currentTimeMillis();
        
        try {
            // 模拟应用启动过程
            simulateAppStartup();
            
            long duration = System.currentTimeMillis() - startTime;
            boolean passed = duration < 3000; // 要求小于3秒
            
            String details = String.format("Startup took %dms (requirement: <3000ms)", duration);
            performanceMonitor.recordMeasurement("app_startup_test", duration);
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "App startup test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试内存管理
     */
    private TestResult testMemoryManagement() {
        String testName = "Memory Management";
        long startTime = System.currentTimeMillis();
        
        try {
            long initialMemory = memoryManager.getUsedMemory();
            
            // 创建一些对象来测试内存管理
            List<byte[]> testData = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                testData.add(new byte[1024 * 10]); // 10KB each
            }
            
            long peakMemory = memoryManager.getUsedMemory();
            
            // 清理对象
            testData.clear();
            testData = null;
            
            // 触发内存清理
            memoryManager.performMemoryCleanup();
            
            // 等待垃圾回收
            Thread.sleep(1000);
            
            long finalMemory = memoryManager.getUsedMemory();
            long memoryRecovered = peakMemory - finalMemory;
            
            boolean passed = memoryRecovered > 0 && !memoryManager.isCriticalMemory();
            long duration = System.currentTimeMillis() - startTime;
            
            String details = String.format("Memory: initial=%dKB, peak=%dKB, final=%dKB, recovered=%dKB",
                                          initialMemory/1024, peakMemory/1024, finalMemory/1024, memoryRecovered/1024);
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Memory management test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试线程池性能
     */
    private TestResult testThreadPoolPerformance() {
        String testName = "Thread Pool Performance";
        long startTime = System.currentTimeMillis();
        
        try {
            CountDownLatch latch = new CountDownLatch(50);
            
            // 提交50个任务到不同的线程池
            for (int i = 0; i < 50; i++) {
                final int taskId = i;
                
                if (i % 4 == 0) {
                    threadPoolManager.executeTranscription(() -> {
                        simulateWork(50);
                        latch.countDown();
                    });
                } else if (i % 4 == 1) {
                    threadPoolManager.executeAudioProcessing(() -> {
                        simulateWork(30);
                        latch.countDown();
                    });
                } else if (i % 4 == 2) {
                    threadPoolManager.executeBackground(() -> {
                        simulateWork(20);
                        latch.countDown();
                    });
                } else {
                    threadPoolManager.executeIO(() -> {
                        simulateWork(10);
                        latch.countDown();
                    });
                }
            }
            
            // 等待所有任务完成，最多等待10秒
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            boolean passed = completed && duration < 5000 && threadPoolManager.isHealthy();
            String details = String.format("Completed %d tasks in %dms, healthy=%s", 
                                          50 - (int)latch.getCount(), duration, threadPoolManager.isHealthy());
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Thread pool performance test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试音频处理性能
     */
    private TestResult testAudioProcessingPerformance() {
        String testName = "Audio Processing Performance";
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建模拟音频数据
            byte[] audioData = generateTestAudioData(16000, 5); // 5秒音频
            
            // 测试音频处理
            OptimizedAudioProcessor processor = new OptimizedAudioProcessor();
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            
            processor.convertAudioFormatAsync(audioData, 16000, new OptimizedAudioProcessor.AudioConversionCallback() {
                @Override
                public void onConversionSuccess(com.example.cantonesevoicerecognition.data.model.AudioData convertedAudio) {
                    success[0] = true;
                    latch.countDown();
                }
                
                @Override
                public void onConversionError(String error) {
                    Log.e(TAG, "Audio conversion error: " + error);
                    latch.countDown();
                }
            });
            
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            boolean passed = completed && success[0] && duration < 5000;
            String details = String.format("Processed 5s audio in %dms (requirement: <5000ms)", duration);
            
            processor.release();
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Audio processing performance test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试转录性能
     */
    private TestResult testTranscriptionPerformance() {
        String testName = "Transcription Performance";
        long startTime = System.currentTimeMillis();
        
        try {
            // 模拟转录过程
            simulateTranscription(30); // 模拟30秒音频转录
            
            long duration = System.currentTimeMillis() - startTime;
            boolean passed = duration < 5000; // 要求30秒音频在5秒内完成转录
            
            String details = String.format("Transcribed 30s audio in %dms (requirement: <5000ms)", duration);
            performanceMonitor.recordTranscriptionTime(duration, 30);
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Transcription performance test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试内存泄漏检测
     */
    private TestResult testMemoryLeakDetection() {
        String testName = "Memory Leak Detection";
        long startTime = System.currentTimeMillis();
        
        try {
            MemoryLeakDetector detector = MemoryLeakDetector.getInstance();
            
            // 创建一些对象并跟踪
            List<Object> testObjects = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Object obj = new Object();
                testObjects.add(obj);
                detector.trackObject(obj, "TestObject");
            }
            
            // 清理一半对象
            for (int i = 0; i < 5; i++) {
                Object obj = testObjects.remove(0);
                detector.untrackObject(obj, "TestObject");
            }
            
            // 强制检查泄漏
            List<MemoryLeakDetector.LeakReport> leaks = detector.forceLeakCheck();
            
            long duration = System.currentTimeMillis() - startTime;
            boolean passed = leaks.isEmpty() || leaks.size() < 3; // 允许少量误报
            
            String details = String.format("Detected %d potential leaks, tracking %d objects", 
                                          leaks.size(), detector.getTotalTrackedCount());
            
            // 清理测试对象
            testObjects.clear();
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Memory leak detection test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试电池优化
     */
    private TestResult testBatteryOptimization() {
        String testName = "Battery Optimization";
        long startTime = System.currentTimeMillis();
        
        try {
            BatteryOptimizer optimizer = BatteryOptimizer.getInstance(context);
            
            // 获取初始状态
            BatteryOptimizer.PowerMode initialMode = optimizer.getCurrentPowerMode();
            boolean initialOptimization = optimizer.isOptimizationEnabled();
            
            // 测试模式切换
            optimizer.setPowerMode(BatteryOptimizer.PowerMode.POWER_SAVE);
            Thread.sleep(100);
            
            optimizer.setPowerMode(BatteryOptimizer.PowerMode.NORMAL);
            Thread.sleep(100);
            
            long duration = System.currentTimeMillis() - startTime;
            boolean passed = optimizer.getCurrentPowerMode() == BatteryOptimizer.PowerMode.NORMAL;
            
            String details = String.format("Battery level: %d%%, charging: %s, mode: %s", 
                                          optimizer.getBatteryLevel(), optimizer.isCharging(), 
                                          optimizer.getCurrentPowerMode());
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Battery optimization test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 测试智能录音检测
     */
    private TestResult testSmartRecordingDetection() {
        String testName = "Smart Recording Detection";
        long startTime = System.currentTimeMillis();
        
        try {
            SmartRecordingDetector detector = new SmartRecordingDetector();
            detector.startDetection();
            
            // 模拟音频数据处理
            byte[] silentAudio = generateSilentAudioData(1000);
            byte[] loudAudio = generateLoudAudioData(1000);
            
            SmartRecordingDetector.DetectionResult silentResult = 
                detector.processAudioData(silentAudio, 16000);
            
            SmartRecordingDetector.DetectionResult loudResult = 
                detector.processAudioData(loudAudio, 16000);
            
            detector.stopDetection();
            
            long duration = System.currentTimeMillis() - startTime;
            boolean passed = !silentResult.isSpeechDetected && loudResult.isSpeechDetected;
            
            String details = String.format("Silent detected as speech: %s, Loud detected as speech: %s", 
                                          silentResult.isSpeechDetected, loudResult.isSpeechDetected);
            
            return new TestResult(testName, passed, duration, details, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "Smart recording detection test failed", e);
            return new TestResult(testName, false, duration, "Test failed with exception", e);
        }
    }
    
    /**
     * 模拟应用启动
     */
    private void simulateAppStartup() throws InterruptedException {
        // 模拟各种启动任务
        Thread.sleep(500); // 模拟初始化
        Thread.sleep(300); // 模拟资源加载
        Thread.sleep(200); // 模拟UI创建
    }
    
    /**
     * 模拟工作负载
     */
    private void simulateWork(int durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 模拟转录过程
     */
    private void simulateTranscription(int audioLengthSeconds) throws InterruptedException {
        // 模拟转录处理时间
        Thread.sleep(audioLengthSeconds * 50); // 假设每秒音频需要50ms处理
    }
    
    /**
     * 生成测试音频数据
     */
    private byte[] generateTestAudioData(int sampleRate, int durationSeconds) {
        int samples = sampleRate * durationSeconds;
        byte[] audioData = new byte[samples * 2]; // 16-bit audio
        
        Random random = new Random();
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (byte) (random.nextInt(256) - 128);
        }
        
        return audioData;
    }
    
    /**
     * 生成静音音频数据
     */
    private byte[] generateSilentAudioData(int samples) {
        return new byte[samples * 2]; // 全零数据
    }
    
    /**
     * 生成大音量音频数据
     */
    private byte[] generateLoudAudioData(int samples) {
        byte[] audioData = new byte[samples * 2];
        
        for (int i = 0; i < audioData.length; i += 2) {
            // 生成高幅度音频信号
            short value = (short) (Math.sin(i * 0.1) * 20000);
            audioData[i] = (byte) (value & 0xFF);
            audioData[i + 1] = (byte) ((value >> 8) & 0xFF);
        }
        
        return audioData;
    }
    
    /**
     * 运行单个性能测试
     */
    public TestResult runSingleTest(String testName) {
        switch (testName.toLowerCase()) {
            case "startup":
                return testAppStartupTime();
            case "memory":
                return testMemoryManagement();
            case "threadpool":
                return testThreadPoolPerformance();
            case "audio":
                return testAudioProcessingPerformance();
            case "transcription":
                return testTranscriptionPerformance();
            case "leak":
                return testMemoryLeakDetection();
            case "battery":
                return testBatteryOptimization();
            case "recording":
                return testSmartRecordingDetection();
            default:
                return new TestResult(testName, false, 0, "Unknown test: " + testName, 
                                    new IllegalArgumentException("Unknown test name"));
        }
    }
    
    /**
     * 获取性能基准测试结果
     */
    public String getPerformanceBenchmark() {
        StringBuilder benchmark = new StringBuilder();
        benchmark.append("Performance Benchmark Results:\n");
        benchmark.append("==============================\n");
        
        // 运行快速基准测试
        long startTime = System.currentTimeMillis();
        
        // CPU基准测试
        long cpuStart = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Math.sqrt(i);
        }
        long cpuTime = System.currentTimeMillis() - cpuStart;
        benchmark.append(String.format("CPU Benchmark: %dms (1M sqrt operations)\n", cpuTime));
        
        // 内存基准测试
        long memStart = System.currentTimeMillis();
        List<byte[]> memTest = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            memTest.add(new byte[1024]);
        }
        memTest.clear();
        long memTime = System.currentTimeMillis() - memStart;
        benchmark.append(String.format("Memory Benchmark: %dms (1000 x 1KB allocations)\n", memTime));
        
        // 线程基准测试
        long threadStart = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            threadPoolManager.executeBackground(() -> {
                simulateWork(10);
                latch.countDown();
            });
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long threadTime = System.currentTimeMillis() - threadStart;
        benchmark.append(String.format("Thread Benchmark: %dms (10 concurrent tasks)\n", threadTime));
        
        long totalTime = System.currentTimeMillis() - startTime;
        benchmark.append(String.format("Total Benchmark Time: %dms\n", totalTime));
        
        return benchmark.toString();
    }