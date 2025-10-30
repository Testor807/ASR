package com.example.cantonesevoicerecognition.utils;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 * 提供优化的多线程处理能力
 */
public class ThreadPoolManager {
    private static final String TAG = "ThreadPoolManager";
    
    private static ThreadPoolManager instance;
    
    // 线程池配置
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE_TIME = 30L; // 30秒
    
    // 不同类型的线程池
    private ThreadPoolExecutor transcriptionPool;
    private ThreadPoolExecutor audioProcessingPool;
    private ThreadPoolExecutor backgroundTaskPool;
    private ThreadPoolExecutor ioPool;
    
    private ThreadPoolManager() {
        initializeThreadPools();
        Log.i(TAG, "ThreadPoolManager initialized with CPU count: " + CPU_COUNT);
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }
    
    /**
     * 初始化线程池
     */
    private void initializeThreadPools() {
        // 转录专用线程池 - 高优先级，较少线程数
        transcriptionPool = createThreadPool(
            "Transcription",
            1, // 单线程处理转录任务，避免资源竞争
            2,
            Thread.NORM_PRIORITY + 1
        );
        
        // 音频处理线程池 - 中等优先级，适中线程数
        audioProcessingPool = createThreadPool(
            "AudioProcessing",
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            Thread.NORM_PRIORITY
        );
        
        // 后台任务线程池 - 低优先级，较多线程数
        backgroundTaskPool = createThreadPool(
            "Background",
            CORE_POOL_SIZE,
            MAX_POOL_SIZE * 2,
            Thread.NORM_PRIORITY - 1
        );
        
        // IO操作线程池 - 专门处理文件和网络IO
        ioPool = createThreadPool(
            "IO",
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            Thread.NORM_PRIORITY
        );
        
        Log.i(TAG, String.format("Thread pools initialized - Transcription: %d-%d, Audio: %d-%d, Background: %d-%d, IO: %d-%d",
                                1, 2, CORE_POOL_SIZE, MAX_POOL_SIZE, 
                                CORE_POOL_SIZE, MAX_POOL_SIZE * 2, CORE_POOL_SIZE, MAX_POOL_SIZE));
    }
    
    /**
     * 创建线程池
     */
    private ThreadPoolExecutor createThreadPool(String name, int coreSize, int maxSize, int priority) {
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(128);
        
        ThreadFactory threadFactory = new CustomThreadFactory(name, priority);
        RejectedExecutionHandler rejectedHandler = new CustomRejectedExecutionHandler(name);
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreSize,
            maxSize,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            rejectedHandler
        );
        
        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);
        
        return executor;
    }
    
    /**
     * 自定义线程工厂
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final int priority;
        
        CustomThreadFactory(String poolName, int priority) {
            this.namePrefix = "CVR-" + poolName + "-";
            this.priority = priority;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(priority);
            return thread;
        }
    }
    
    /**
     * 自定义拒绝执行处理器
     */
    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        private final String poolName;
        
        CustomRejectedExecutionHandler(String poolName) {
            this.poolName = poolName;
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.w(TAG, "Task rejected by " + poolName + " pool. Queue size: " + executor.getQueue().size());
            
            // 尝试在调用线程中执行
            if (!executor.isShutdown()) {
                try {
                    r.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error executing rejected task in caller thread", e);
                }
            }
        }
    }
    
    /**
     * 提交转录任务
     */
    public void executeTranscription(Runnable task) {
        if (transcriptionPool != null && !transcriptionPool.isShutdown()) {
            transcriptionPool.execute(wrapTask(task, "Transcription"));
        } else {
            Log.e(TAG, "Transcription pool is not available");
        }
    }
    
    /**
     * 提交音频处理任务
     */
    public void executeAudioProcessing(Runnable task) {
        if (audioProcessingPool != null && !audioProcessingPool.isShutdown()) {
            audioProcessingPool.execute(wrapTask(task, "AudioProcessing"));
        } else {
            Log.e(TAG, "Audio processing pool is not available");
        }
    }
    
    /**
     * 提交后台任务
     */
    public void executeBackground(Runnable task) {
        if (backgroundTaskPool != null && !backgroundTaskPool.isShutdown()) {
            backgroundTaskPool.execute(wrapTask(task, "Background"));
        } else {
            Log.e(TAG, "Background pool is not available");
        }
    }
    
    /**
     * 提交IO任务
     */
    public void executeIO(Runnable task) {
        if (ioPool != null && !ioPool.isShutdown()) {
            ioPool.execute(wrapTask(task, "IO"));
        } else {
            Log.e(TAG, "IO pool is not available");
        }
    }
    
    /**
     * 包装任务，添加性能监控和错误处理
     */
    private Runnable wrapTask(Runnable task, String taskType) {
        return () -> {
            String taskName = taskType + "_task";
            PerformanceMonitor monitor = PerformanceMonitor.getInstance();
            
            monitor.startMeasurement(taskName);
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing " + taskType + " task", e);
                
                // 记录错误到日志系统
                LogManager.getInstance().logError(TAG, "Task execution error: " + taskType, e);
            } finally {
                monitor.endMeasurement(taskName);
            }
        };
    }
    
    /**
     * 获取转录线程池状态
     */
    public String getTranscriptionPoolStatus() {
        return getPoolStatus("Transcription", transcriptionPool);
    }
    
    /**
     * 获取音频处理线程池状态
     */
    public String getAudioProcessingPoolStatus() {
        return getPoolStatus("AudioProcessing", audioProcessingPool);
    }
    
    /**
     * 获取后台任务线程池状态
     */
    public String getBackgroundPoolStatus() {
        return getPoolStatus("Background", backgroundTaskPool);
    }
    
    /**
     * 获取IO线程池状态
     */
    public String getIOPoolStatus() {
        return getPoolStatus("IO", ioPool);
    }
    
    /**
     * 获取线程池状态信息
     */
    private String getPoolStatus(String name, ThreadPoolExecutor pool) {
        if (pool == null) {
            return name + ": Not initialized";
        }
        
        return String.format("%s: active=%d, pool=%d/%d, queue=%d, completed=%d",
                           name,
                           pool.getActiveCount(),
                           pool.getPoolSize(),
                           pool.getMaximumPoolSize(),
                           pool.getQueue().size(),
                           pool.getCompletedTaskCount());
    }
    
    /**
     * 获取所有线程池状态
     */
    public String getAllPoolsStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Thread Pool Status:\n");
        status.append("==================\n");
        status.append(getTranscriptionPoolStatus()).append("\n");
        status.append(getAudioProcessingPoolStatus()).append("\n");
        status.append(getBackgroundPoolStatus()).append("\n");
        status.append(getIOPoolStatus()).append("\n");
        
        return status.toString();
    }
    
    /**
     * 检查线程池健康状态
     */
    public boolean isHealthy() {
        return isPoolHealthy(transcriptionPool) &&
               isPoolHealthy(audioProcessingPool) &&
               isPoolHealthy(backgroundTaskPool) &&
               isPoolHealthy(ioPool);
    }
    
    /**
     * 检查单个线程池健康状态
     */
    private boolean isPoolHealthy(ThreadPoolExecutor pool) {
        if (pool == null || pool.isShutdown()) {
            return false;
        }
        
        // 检查队列是否过满
        int queueSize = pool.getQueue().size();
        int maxQueueSize = 100; // 假设的最大健康队列大小
        
        if (queueSize > maxQueueSize) {
            Log.w(TAG, "Thread pool queue is too full: " + queueSize);
            return false;
        }
        
        return true;
    }
    
    /**
     * 优化线程池配置
     */
    public void optimizeThreadPools() {
        Log.i(TAG, "Optimizing thread pool configurations");
        
        // 根据当前系统负载调整线程池大小
        MemoryManager memoryManager = MemoryManager.getInstance(null);
        
        if (memoryManager != null && memoryManager.isLowMemory()) {
            // 内存不足时减少线程数
            Log.i(TAG, "Low memory detected, reducing thread pool sizes");
            adjustPoolSize(audioProcessingPool, Math.max(1, CORE_POOL_SIZE / 2));
            adjustPoolSize(backgroundTaskPool, Math.max(1, CORE_POOL_SIZE / 2));
        } else {
            // 内存充足时恢复正常线程数
            adjustPoolSize(audioProcessingPool, CORE_POOL_SIZE);
            adjustPoolSize(backgroundTaskPool, CORE_POOL_SIZE);
        }
    }
    
    /**
     * 调整线程池大小
     */
    private void adjustPoolSize(ThreadPoolExecutor pool, int newCoreSize) {
        if (pool != null && !pool.isShutdown()) {
            pool.setCorePoolSize(newCoreSize);
            Log.d(TAG, "Adjusted pool core size to: " + newCoreSize);
        }
    }
    
    /**
     * 清理空闲线程
     */
    public void purgeIdleThreads() {
        Log.i(TAG, "Purging idle threads");
        
        if (transcriptionPool != null) transcriptionPool.purge();
        if (audioProcessingPool != null) audioProcessingPool.purge();
        if (backgroundTaskPool != null) backgroundTaskPool.purge();
        if (ioPool != null) ioPool.purge();
    }
    
    /**
     * 等待所有任务完成
     */
    public void awaitTermination(long timeout, TimeUnit unit) {
        try {
            if (transcriptionPool != null) {
                transcriptionPool.awaitTermination(timeout, unit);
            }
            if (audioProcessingPool != null) {
                audioProcessingPool.awaitTermination(timeout, unit);
            }
            if (backgroundTaskPool != null) {
                backgroundTaskPool.awaitTermination(timeout, unit);
            }
            if (ioPool != null) {
                ioPool.awaitTermination(timeout, unit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Thread interrupted while waiting for termination");
        }
    }
    
    /**
     * 关闭所有线程池
     */
    public void shutdown() {
        Log.i(TAG, "Shutting down all thread pools");
        
        shutdownPool("Transcription", transcriptionPool);
        shutdownPool("AudioProcessing", audioProcessingPool);
        shutdownPool("Background", backgroundTaskPool);
        shutdownPool("IO", ioPool);
        
        Log.i(TAG, "All thread pools shut down");
    }
    
    /**
     * 关闭单个线程池
     */
    private void shutdownPool(String name, ThreadPoolExecutor pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.w(TAG, name + " pool did not terminate gracefully, forcing shutdown");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}