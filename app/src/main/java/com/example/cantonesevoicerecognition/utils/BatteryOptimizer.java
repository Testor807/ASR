package com.example.cantonesevoicerecognition.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 电池优化管理器
 * 监控电池状态并优化应用的电池使用
 */
public class BatteryOptimizer {
    private static final String TAG = "BatteryOptimizer";
    
    // 电池阈值
    private static final int LOW_BATTERY_THRESHOLD = 20; // 20%
    private static final int CRITICAL_BATTERY_THRESHOLD = 10; // 10%
    
    // 优化模式
    public enum PowerMode {
        NORMAL,      // 正常模式
        POWER_SAVE,  // 省电模式
        ULTRA_SAVE   // 超级省电模式
    }
    
    private static BatteryOptimizer instance;
    private final Context context;
    private final PowerManager powerManager;
    private final List<BatteryListener> listeners;
    private final ScheduledExecutorService scheduler;
    
    private BatteryReceiver batteryReceiver;
    private PowerMode currentPowerMode = PowerMode.NORMAL;
    private boolean isOptimizationEnabled = true;
    private boolean isMonitoring = false;
    
    // 电池状态
    private int batteryLevel = 100;
    private boolean isCharging = false;
    private boolean isPowerSaveMode = false;
    
    /**
     * 电池状态监听器
     */
    public interface BatteryListener {
        void onBatteryLevelChanged(int level, boolean isCharging);
        void onLowBattery(int level);
        void onCriticalBattery(int level);
        void onPowerModeChanged(PowerMode mode);
    }
    
    /**
     * 电池状态广播接收器
     */
    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                handleBatteryChanged(intent);
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                handlePowerConnected();
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                handlePowerDisconnected();
            } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(action)) {
                handlePowerSaveModeChanged();
            }
        }
    }
    
    private BatteryOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.listeners = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        initializeBatteryStatus();
        Log.i(TAG, "BatteryOptimizer initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized BatteryOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryOptimizer(context);
        }
        return instance;
    }
    
    /**
     * 初始化电池状态
     */
    private void initializeBatteryStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            
            if (level >= 0 && scale > 0) {
                batteryLevel = (int) ((level / (float) scale) * 100);
            }
            
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
        }
        
        // 检查系统省电模式
        if (powerManager != null) {
            isPowerSaveMode = powerManager.isPowerSaveMode();
        }
        
        Log.i(TAG, String.format("Initial battery status - Level: %d%%, Charging: %s, PowerSave: %s",
                                batteryLevel, isCharging, isPowerSaveMode));
    }
    
    /**
     * 开始电池监控
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Battery monitoring already started");
            return;
        }
        
        // 注册电池状态广播接收器
        batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        
        context.registerReceiver(batteryReceiver, filter);
        
        // 启动定期检查
        scheduler.scheduleAtFixedRate(this::performBatteryOptimization, 
                                    30, 30, TimeUnit.SECONDS);
        
        isMonitoring = true;
        Log.i(TAG, "Battery monitoring started");
    }
    
    /**
     * 停止电池监控
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Battery receiver was not registered");
            }
            batteryReceiver = null;
        }
        
        isMonitoring = false;
        Log.i(TAG, "Battery monitoring stopped");
    }
    
    /**
     * 添加电池监听器
     */
    public void addBatteryListener(BatteryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除电池监听器
     */
    public void removeBatteryListener(BatteryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 处理电池状态变化
     */
    private void handleBatteryChanged(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        
        if (level >= 0 && scale > 0) {
            int newBatteryLevel = (int) ((level / (float) scale) * 100);
            
            if (newBatteryLevel != batteryLevel) {
                batteryLevel = newBatteryLevel;
                
                Log.d(TAG, "Battery level changed: " + batteryLevel + "%");
                
                // 通知监听器
                for (BatteryListener listener : listeners) {
                    try {
                        listener.onBatteryLevelChanged(batteryLevel, isCharging);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying battery listener", e);
                    }
                }
                
                // 检查是否需要切换省电模式
                checkAndUpdatePowerMode();
            }
        }
    }
    
    /**
     * 处理电源连接
     */
    private void handlePowerConnected() {
        isCharging = true;
        Log.i(TAG, "Power connected - charging started");
        
        // 充电时可以恢复正常模式
        if (currentPowerMode != PowerMode.NORMAL) {
            setPowerMode(PowerMode.NORMAL);
        }
    }
    
    /**
     * 处理电源断开
     */
    private void handlePowerDisconnected() {
        isCharging = false;
        Log.i(TAG, "Power disconnected - running on battery");
        
        // 断开充电时检查是否需要省电
        checkAndUpdatePowerMode();
    }
    
    /**
     * 处理系统省电模式变化
     */
    private void handlePowerSaveModeChanged() {
        if (powerManager != null) {
            boolean newPowerSaveMode = powerManager.isPowerSaveMode();
            
            if (newPowerSaveMode != isPowerSaveMode) {
                isPowerSaveMode = newPowerSaveMode;
                Log.i(TAG, "System power save mode changed: " + isPowerSaveMode);
                
                // 根据系统省电模式调整应用省电策略
                if (isPowerSaveMode && currentPowerMode == PowerMode.NORMAL) {
                    setPowerMode(PowerMode.POWER_SAVE);
                }
            }
        }
    }
    
    /**
     * 检查并更新电源模式
     */
    private void checkAndUpdatePowerMode() {
        if (!isOptimizationEnabled) {
            return;
        }
        
        PowerMode newMode = currentPowerMode;
        
        if (isCharging) {
            // 充电时使用正常模式
            newMode = PowerMode.NORMAL;
        } else if (batteryLevel <= CRITICAL_BATTERY_THRESHOLD) {
            // 电量极低时使用超级省电模式
            newMode = PowerMode.ULTRA_SAVE;
            
            // 通知监听器电量危急
            for (BatteryListener listener : listeners) {
                try {
                    listener.onCriticalBattery(batteryLevel);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying critical battery listener", e);
                }
            }
        } else if (batteryLevel <= LOW_BATTERY_THRESHOLD || isPowerSaveMode) {
            // 电量较低或系统省电模式时使用省电模式
            newMode = PowerMode.POWER_SAVE;
            
            // 通知监听器电量较低
            for (BatteryListener listener : listeners) {
                try {
                    listener.onLowBattery(batteryLevel);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying low battery listener", e);
                }
            }
        } else if (batteryLevel > LOW_BATTERY_THRESHOLD + 10) {
            // 电量恢复正常
            newMode = PowerMode.NORMAL;
        }
        
        if (newMode != currentPowerMode) {
            setPowerMode(newMode);
        }
    }
    
    /**
     * 设置电源模式
     */
    public void setPowerMode(PowerMode mode) {
        if (mode == currentPowerMode) {
            return;
        }
        
        PowerMode oldMode = currentPowerMode;
        currentPowerMode = mode;
        
        Log.i(TAG, "Power mode changed: " + oldMode + " -> " + currentPowerMode);
        
        // 应用相应的优化策略
        applyPowerModeOptimizations();
        
        // 通知监听器
        for (BatteryListener listener : listeners) {
            try {
                listener.onPowerModeChanged(currentPowerMode);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying power mode listener", e);
            }
        }
    }
    
    /**
     * 应用电源模式优化
     */
    private void applyPowerModeOptimizations() {
        switch (currentPowerMode) {
            case NORMAL:
                applyNormalModeOptimizations();
                break;
            case POWER_SAVE:
                applyPowerSaveModeOptimizations();
                break;
            case ULTRA_SAVE:
                applyUltraSaveModeOptimizations();
                break;
        }
    }
    
    /**
     * 应用正常模式优化
     */
    private void applyNormalModeOptimizations() {
        Log.i(TAG, "Applying normal mode optimizations");
        
        // 恢复正常的线程池配置
        ThreadPoolManager threadManager = ThreadPoolManager.getInstance();
        threadManager.optimizeThreadPools();
        
        // 恢复正常的内存监控频率
        // 可以在这里添加其他正常模式的配置
    }
    
    /**
     * 应用省电模式优化
     */
    private void applyPowerSaveModeOptimizations() {
        Log.i(TAG, "Applying power save mode optimizations");
        
        // 减少后台任务频率
        // 降低音频处理质量
        // 减少内存监控频率
        // 限制网络请求
        
        // 通知其他组件进入省电模式
        Intent intent = new Intent("com.example.cantonesevoicerecognition.POWER_SAVE_MODE");
        intent.putExtra("enabled", true);
        context.sendBroadcast(intent);
    }
    
    /**
     * 应用超级省电模式优化
     */
    private void applyUltraSaveModeOptimizations() {
        Log.i(TAG, "Applying ultra save mode optimizations");
        
        // 暂停非关键后台任务
        // 最小化音频处理
        // 停止内存监控
        // 禁用网络请求
        // 降低屏幕亮度建议
        
        // 通知其他组件进入超级省电模式
        Intent intent = new Intent("com.example.cantonesevoicerecognition.ULTRA_SAVE_MODE");
        intent.putExtra("enabled", true);
        context.sendBroadcast(intent);
    }
    
    /**
     * 执行电池优化
     */
    private void performBatteryOptimization() {
        try {
            // 检查电池状态
            checkAndUpdatePowerMode();
            
            // 根据当前模式执行相应优化
            switch (currentPowerMode) {
                case POWER_SAVE:
                    optimizeForPowerSave();
                    break;
                case ULTRA_SAVE:
                    optimizeForUltraSave();
                    break;
                default:
                    // 正常模式不需要特殊优化
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during battery optimization", e);
        }
    }
    
    /**
     * 省电模式优化
     */
    private void optimizeForPowerSave() {
        // 清理内存
        MemoryManager memoryManager = MemoryManager.getInstance(context);
        if (memoryManager != null) {
            memoryManager.performMemoryCleanup();
        }
        
        // 清理线程池空闲线程
        ThreadPoolManager.getInstance().purgeIdleThreads();
    }
    
    /**
     * 超级省电模式优化
     */
    private void optimizeForUltraSave() {
        // 执行更激进的优化
        optimizeForPowerSave();
        
        // 强制垃圾回收
        System.gc();
        
        // 可以在这里添加更多超级省电优化
    }
    
    /**
     * 获取当前电池状态
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }
    
    /**
     * 检查是否正在充电
     */
    public boolean isCharging() {
        return isCharging;
    }
    
    /**
     * 获取当前电源模式
     */
    public PowerMode getCurrentPowerMode() {
        return currentPowerMode;
    }
    
    /**
     * 检查是否启用了优化
     */
    public boolean isOptimizationEnabled() {
        return isOptimizationEnabled;
    }
    
    /**
     * 启用/禁用电池优化
     */
    public void setOptimizationEnabled(boolean enabled) {
        isOptimizationEnabled = enabled;
        Log.i(TAG, "Battery optimization " + (enabled ? "enabled" : "disabled"));
        
        if (!enabled && currentPowerMode != PowerMode.NORMAL) {
            setPowerMode(PowerMode.NORMAL);
        }
    }
    
    /**
     * 获取电池状态信息
     */
    public String getBatteryStatusInfo() {
        return String.format("Battery Status - Level: %d%%, Charging: %s, Mode: %s, SystemPowerSave: %s",
                           batteryLevel, isCharging, currentPowerMode, isPowerSaveMode);
    }
    
    /**
     * 获取省电建议
     */
    public List<String> getPowerSavingTips() {
        List<String> tips = new ArrayList<>();
        
        if (batteryLevel <= LOW_BATTERY_THRESHOLD && !isCharging) {
            tips.add("电量较低，建议连接充电器");
            tips.add("关闭不必要的后台应用");
            tips.add("降低屏幕亮度");
            tips.add("关闭蓝牙和WiFi（如不需要）");
        }
        
        if (currentPowerMode == PowerMode.POWER_SAVE) {
            tips.add("当前处于省电模式，某些功能可能受限");
        } else if (currentPowerMode == PowerMode.ULTRA_SAVE) {
            tips.add("当前处于超级省电模式，仅保留核心功能");
        }
        
        return tips;
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
        
        listeners.clear();
        Log.i(TAG, "BatteryOptimizer released");
    }
}