package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.util.Log;

/**
 * 性能优化集成示例
 * 展示如何在应用中正确使用性能优化功能
 */
public class PerformanceIntegrationExample {
    private static final String TAG = "PerformanceIntegration";
    
    private final Context context;
    private PerformanceOptimizer performanceOptimizer;
    private PerformanceTestRunner testRunner;
    
    public PerformanceIntegrationExample(Context context) {
        this.context = context.getApplicationContext();
        initializePerformanceComponents();
    }
    
    /**
     * 初始化性能组件
     */
    private void initializePerformanceComponents() {
        Log.i(TAG, "初始化性能优化组件");
        
        // 1. 初始化性能优化器
        performanceOptimizer = PerformanceOptimizer.getInstance(context);
        performanceOptimizer.startOptimization();
        
        // 2. 设置优化级别（根据设备性能和用户偏好）
        performanceOptimizer.setOptimizationLevel(PerformanceOptimizer.OptimizationLevel.BALANCED);
        
        // 3. 初始化测试运行器
        testRunner = new PerformanceTestRunner(context);
        
        Log.i(TAG, "性能优化组件初始化完成");
    }
    
    /**
     * 演示如何在应用启动时进行性能优化
     */
    public void demonstrateAppStartupOptimization() {
        Log.i(TAG, "演示应用启动优化");
        
        long startTime = System.currentTimeMillis();
        
        // 1. 记录启动开始时间
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.startMeasurement("app_startup_demo");
        
        try {
            // 2. 模拟应用初始化过程
            simulateAppInitialization();
            
            // 3. 记录启动完成时间
            monitor.endMeasurement("app_startup_demo");
            
            long startupTime = System.currentTimeMillis() - startTime;
            Log.i(TAG, "应用启动完成，耗时: " + startupTime + "ms");
            
            // 4. 检查是否满足性能要求
            if (startupTime > 3000) {
                Log.w(TAG, "应用启动时间超过要求，建议优化");
                performanceOptimizer.setOptimizationLevel(PerformanceOptimizer.OptimizationLevel.PERFORMANCE);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "应用启动优化演示失败", e);
        }
    }
    
    /**
     * 演示如何在音频处理时进行性能优化
     */
    public void demonstrateAudioProcessingOptimization() {
        Log.i(TAG, "演示音频处理优化");
        
        // 1. 使用优化的音频处理器
        OptimizedAudioProcessor audioProcessor = new OptimizedAudioProcessor();
        
        // 2. 创建测试音频数据
        byte[] testAudioData = generateTestAudioData();
        
        // 3. 异步处理音频，避免阻塞UI线程
        audioProcessor.convertAudioFormatAsync(testAudioData, 16000, 
            new OptimizedAudioProcessor.AudioConversionCallback() {
                @Override
                public void onConversionSuccess(com.example.cantonesevoicerecognition.data.model.AudioData convertedAudio) {
                    Log.i(TAG, "音频转换成功");
                    
                    // 4. 记录处理性能
                    PerformanceMonitor.getInstance().recordMeasurement("audio_conversion_demo", 
                        System.currentTimeMillis());
                }
                
                @Override
                public void onConversionError(String error) {
                    Log.e(TAG, "音频转换失败: " + error);
                }
            });
        
        // 5. 清理资源
        audioProcessor.release();
    }
    
    /**
     * 演示如何进行内存优化
     */
    public void demonstrateMemoryOptimization() {
        Log.i(TAG, "演示内存优化");
        
        MemoryManager memoryManager = MemoryManager.getInstance(context);
        
        // 1. 检查当前内存状态
        Log.i(TAG, "当前内存状态: " + memoryManager.getMemoryStatusInfo());
        
        // 2. 模拟内存密集型操作
        simulateMemoryIntensiveOperation();
        
        // 3. 检查内存使用情况
        if (memoryManager.isLowMemory()) {
            Log.w(TAG, "检测到低内存状态，执行内存清理");
            memoryManager.performMemoryCleanup();
        }
        
        // 4. 再次检查内存状态
        Log.i(TAG, "优化后内存状态: " + memoryManager.getMemoryStatusInfo());
    }
    
    /**
     * 演示如何进行电池优化
     */
    public void demonstrateBatteryOptimization() {
        Log.i(TAG, "演示电池优化");
        
        BatteryOptimizer batteryOptimizer = BatteryOptimizer.getInstance(context);
        
        // 1. 检查当前电池状态
        Log.i(TAG, "当前电池状态: " + batteryOptimizer.getBatteryStatusInfo());
        
        // 2. 根据电池状态调整性能策略
        if (batteryOptimizer.getBatteryLevel() < 20) {
            Log.i(TAG, "电量较低，切换到电池优化模式");
            performanceOptimizer.setOptimizationLevel(PerformanceOptimizer.OptimizationLevel.BATTERY);
        }
        
        // 3. 获取省电建议
        java.util.List<String> tips = batteryOptimizer.getPowerSavingTips();
        for (String tip : tips) {
            Log.i(TAG, "省电建议: " + tip);
        }
    }
    
    /**
     * 演示如何使用智能录音检测
     */
    public void demonstrateSmartRecordingDetection() {
        Log.i(TAG, "演示智能录音检测");
        
        SmartRecordingDetector detector = new SmartRecordingDetector();
        
        // 1. 设置检测监听器
        detector.setDetectionListener(new SmartRecordingDetector.DetectionListener() {
            @Override
            public void onSpeechStarted(long timestamp) {
                Log.i(TAG, "检测到语音开始: " + timestamp);
            }
            
            @Override
            public void onSpeechEnded(long timestamp, long duration) {
                Log.i(TAG, "检测到语音结束: " + timestamp + ", 持续时间: " + duration + "ms");
            }
            
            @Override
            public void onSilenceDetected(long duration) {
                Log.i(TAG, "检测到静音: " + duration + "ms");
            }
            
            @Override
            public void onBackgroundNoiseUpdated(float noiseLevel) {
                Log.i(TAG, "背景噪音更新: " + noiseLevel);
            }
        });
        
        // 2. 开始检测
        detector.startDetection();
        
        // 3. 模拟音频数据处理
        byte[] silentAudio = new byte[1600]; // 静音数据
        byte[] speechAudio = generateSpeechAudioData(); // 语音数据
        
        SmartRecordingDetector.DetectionResult silentResult = 
            detector.processAudioData(silentAudio, 16000);
        SmartRecordingDetector.DetectionResult speechResult = 
            detector.processAudioData(speechAudio, 16000);
        
        Log.i(TAG, "静音检测结果: " + silentResult.toString());
        Log.i(TAG, "语音检测结果: " + speechResult.toString());
        
        // 4. 停止检测
        detector.stopDetection();
    }
    
    /**
     * 演示如何运行性能测试
     */
    public void demonstratePerformanceTesting() {
        Log.i(TAG, "演示性能测试");
        
        // 1. 运行单个测试
        testRunner.runSingleTestAsync("startup", new PerformanceTestRunner.TestResultListener() {
            @Override
            public void onTestStarted(String testName) {
                Log.i(TAG, "开始测试: " + testName);
            }
            
            @Override
            public void onTestCompleted(PerformanceTester.TestResult result) {
                Log.i(TAG, "测试完成: " + result.toString());
            }
            
            @Override
            public void onTestSuiteCompleted(PerformanceTester.TestSuiteResult suiteResult) {
                // 单个测试不会触发此回调
            }
            
            @Override
            public void onTestError(String testName, Exception error) {
                Log.e(TAG, "测试失败: " + testName, error);
            }
        });
        
        // 2. 检查性能要求
        testRunner.checkPerformanceRequirementsAsync().thenAccept(meetsRequirements -> {
            if (meetsRequirements) {
                Log.i(TAG, "应用满足性能要求");
            } else {
                Log.w(TAG, "应用不满足性能要求，需要优化");
                performanceOptimizer.performFullOptimization();
            }
        });
    }
    
    /**
     * 演示如何生成性能报告
     */
    public void demonstratePerformanceReporting() {
        Log.i(TAG, "演示性能报告生成");
        
        // 1. 生成详细的性能报告
        String performanceReport = testRunner.generatePerformanceReport();
        Log.i(TAG, "性能报告:\n" + performanceReport);
        
        // 2. 获取优化摘要
        String optimizationSummary = performanceOptimizer.getOptimizationSummary();
        Log.i(TAG, "优化摘要:\n" + optimizationSummary);
        
        // 3. 运行基准测试
        testRunner.runBenchmarkAsync().thenAccept(benchmark -> {
            Log.i(TAG, "基准测试结果:\n" + benchmark);
        });
    }
    
    /**
     * 模拟应用初始化
     */
    private void simulateAppInitialization() {
        try {
            // 模拟各种初始化任务
            Thread.sleep(500); // 数据库初始化
            Thread.sleep(300); // 网络配置
            Thread.sleep(200); // UI组件初始化
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 模拟内存密集型操作
     */
    private void simulateMemoryIntensiveOperation() {
        // 创建一些大对象来模拟内存使用
        java.util.List<byte[]> memoryConsumers = new java.util.ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            memoryConsumers.add(new byte[1024 * 100]); // 100KB each
        }
        
        // 模拟处理时间
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 清理内存
        memoryConsumers.clear();
    }
    
    /**
     * 生成测试音频数据
     */
    private byte[] generateTestAudioData() {
        // 生成5秒的16kHz单声道音频数据
        int sampleRate = 16000;
        int duration = 5; // 秒
        int samples = sampleRate * duration;
        byte[] audioData = new byte[samples * 2]; // 16-bit
        
        // 填充随机数据模拟音频
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (byte) (random.nextInt(256) - 128);
        }
        
        return audioData;
    }
    
    /**
     * 生成语音音频数据
     */
    private byte[] generateSpeechAudioData() {
        byte[] audioData = new byte[3200]; // 0.1秒的音频
        
        // 生成高能量音频信号模拟语音
        for (int i = 0; i < audioData.length; i += 2) {
            short value = (short) (Math.sin(i * 0.1) * 15000);
            audioData[i] = (byte) (value & 0xFF);
            audioData[i + 1] = (byte) ((value >> 8) & 0xFF);
        }
        
        return audioData;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (testRunner != null) {
            testRunner.release();
        }
        
        if (performanceOptimizer != null) {
            performanceOptimizer.release();
        }
        
        Log.i(TAG, "PerformanceIntegrationExample released");
    }
}