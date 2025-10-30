package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 性能测试运行器
 * 提供便捷的性能测试执行和结果处理功能
 */
public class PerformanceTestRunner {
    private static final String TAG = "PerformanceTestRunner";
    
    private final Context context;
    private final PerformanceTester tester;
    private final ExecutorService executor;
    
    /**
     * 测试结果监听器
     */
    public interface TestResultListener {
        void onTestStarted(String testName);
        void onTestCompleted(PerformanceTester.TestResult result);
        void onTestSuiteCompleted(PerformanceTester.TestSuiteResult suiteResult);
        void onTestError(String testName, Exception error);
    }
    
    public PerformanceTestRunner(Context context) {
        this.context = context.getApplicationContext();
        this.tester = new PerformanceTester(context);
        this.executor = Executors.newSingleThreadExecutor();
        
        Log.i(TAG, "PerformanceTestRunner initialized");
    }
    
    /**
     * 异步运行完整测试套件
     */
    public CompletableFuture<PerformanceTester.TestSuiteResult> runFullTestSuiteAsync(TestResultListener listener) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.i(TAG, "Starting full performance test suite");
                
                if (listener != null) {
                    listener.onTestStarted("Full Test Suite");
                }
                
                PerformanceTester.TestSuiteResult result = tester.runFullTestSuite();
                
                if (listener != null) {
                    listener.onTestSuiteCompleted(result);
                }
                
                Log.i(TAG, "Performance test suite completed: " + result.passedTests + "/" + result.totalTests + " passed");
                return result;
                
            } catch (Exception e) {
                Log.e(TAG, "Error running performance test suite", e);
                if (listener != null) {
                    listener.onTestError("Full Test Suite", e);
                }
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * 异步运行单个测试
     */
    public CompletableFuture<PerformanceTester.TestResult> runSingleTestAsync(String testName, TestResultListener listener) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.i(TAG, "Starting test: " + testName);
                
                if (listener != null) {
                    listener.onTestStarted(testName);
                }
                
                PerformanceTester.TestResult result = tester.runSingleTest(testName);
                
                if (listener != null) {
                    listener.onTestCompleted(result);
                }
                
                Log.i(TAG, "Test completed: " + result.toString());
                return result;
                
            } catch (Exception e) {
                Log.e(TAG, "Error running test: " + testName, e);
                if (listener != null) {
                    listener.onTestError(testName, e);
                }
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * 运行性能基准测试
     */
    public CompletableFuture<String> runBenchmarkAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.i(TAG, "Starting performance benchmark");
                String benchmark = tester.getPerformanceBenchmark();
                Log.i(TAG, "Performance benchmark completed");
                return benchmark;
                
            } catch (Exception e) {
                Log.e(TAG, "Error running performance benchmark", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * 检查应用是否满足性能要求
     */
    public CompletableFuture<Boolean> checkPerformanceRequirementsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.i(TAG, "Checking performance requirements");
                
                // 运行关键性能测试
                PerformanceTester.TestResult startupTest = tester.runSingleTest("startup");
                PerformanceTester.TestResult transcriptionTest = tester.runSingleTest("transcription");
                PerformanceTester.TestResult memoryTest = tester.runSingleTest("memory");
                
                boolean meetsRequirements = startupTest.passed && 
                                          transcriptionTest.passed && 
                                          memoryTest.passed;
                
                Log.i(TAG, "Performance requirements check completed: " + meetsRequirements);
                return meetsRequirements;
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking performance requirements", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("粤语语音识别应用性能报告\n");
        report.append("========================\n\n");
        
        // 获取性能监控数据
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        report.append("性能监控数据:\n");
        report.append(monitor.getKPISummary()).append("\n");
        
        // 获取内存状态
        MemoryManager memoryManager = MemoryManager.getInstance(context);
        report.append("内存状态:\n");
        report.append(memoryManager.getMemoryStatusInfo()).append("\n\n");
        
        // 获取线程池状态
        ThreadPoolManager threadManager = ThreadPoolManager.getInstance();
        report.append("线程池状态:\n");
        report.append(threadManager.getAllPoolsStatus()).append("\n");
        
        // 获取电池优化状态
        BatteryOptimizer batteryOptimizer = BatteryOptimizer.getInstance(context);
        report.append("电池优化状态:\n");
        report.append(batteryOptimizer.getBatteryStatusInfo()).append("\n\n");
        
        // 获取性能优化摘要
        PerformanceOptimizer optimizer = PerformanceOptimizer.getInstance(context);
        report.append("性能优化摘要:\n");
        report.append(optimizer.getOptimizationSummary()).append("\n");
        
        return report.toString();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        Log.i(TAG, "PerformanceTestRunner released");
    }
}