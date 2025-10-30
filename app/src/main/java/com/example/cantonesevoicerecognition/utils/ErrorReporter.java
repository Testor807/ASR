package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.example.cantonesevoicerecognition.engine.TranscriptionError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * 错误报告器
 * 提供详细的错误报告和系统信息收集功能
 */
public class ErrorReporter {
    
    private static final String TAG = "ErrorReporter";
    private static ErrorReporter instance;
    
    private final Context context;
    private final LogManager logManager;
    private final SimpleDateFormat dateFormat;
    
    private ErrorReporter(Context context) {
        this.context = context.getApplicationContext();
        this.logManager = LogManager.getInstance(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    /**
     * 获取ErrorReporter单例实例
     */
    public static synchronized ErrorReporter getInstance(Context context) {
        if (instance == null) {
            instance = new ErrorReporter(context);
        }
        return instance;
    }
    
    /**
     * 生成详细的错误报告
     */
    public String generateErrorReport(TranscriptionError error, Throwable throwable, 
                                    String additionalInfo) {
        StringBuilder report = new StringBuilder();
        
        // 报告头部
        report.append("=== 错误报告 ===\n");
        report.append("时间: ").append(dateFormat.format(new Date())).append("\n");
        report.append("错误类型: ").append(error.name()).append("\n");
        report.append("错误消息: ").append(error.getDefaultMessage()).append("\n");
        report.append("错误类别: ").append(error.getCategory().getDisplayName()).append("\n");
        report.append("是否严重: ").append(error.isCritical() ? "是" : "否").append("\n");
        report.append("是否可恢复: ").append(error.isRecoverable() ? "是" : "否").append("\n");
        report.append("\n");
        
        // 应用信息
        report.append("=== 应用信息 ===\n");
        report.append(getApplicationInfo()).append("\n");
        
        // 设备信息
        report.append("=== 设备信息 ===\n");
        report.append(getDeviceInfo()).append("\n");
        
        // 系统信息
        report.append("=== 系统信息 ===\n");
        report.append(getSystemInfo()).append("\n");
        
        // 网络信息
        report.append("=== 网络信息 ===\n");
        report.append(getNetworkInfo()).append("\n");
        
        // 异常堆栈
        if (throwable != null) {
            report.append("=== 异常堆栈 ===\n");
            report.append(getStackTrace(throwable)).append("\n");
        }
        
        // 错误统计
        report.append("=== 错误统计 ===\n");
        report.append(getErrorStatistics()).append("\n");
        
        // 附加信息
        if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
            report.append("=== 附加信息 ===\n");
            report.append(additionalInfo).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 获取应用信息
     */
    private String getApplicationInfo() {
        StringBuilder info = new StringBuilder();
        
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            
            info.append("应用名称: ").append(packageInfo.applicationInfo.loadLabel(pm)).append("\n");
            info.append("包名: ").append(packageInfo.packageName).append("\n");
            info.append("版本名称: ").append(packageInfo.versionName).append("\n");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.append("版本代码: ").append(packageInfo.getLongVersionCode()).append("\n");
            } else {
                info.append("版本代码: ").append(packageInfo.versionCode).append("\n");
            }
            info.append("目标SDK: ").append(packageInfo.applicationInfo.targetSdkVersion).append("\n");
            info.append("最小SDK: ").append(packageInfo.applicationInfo.minSdkVersion).append("\n");
            
        } catch (PackageManager.NameNotFoundException e) {
            info.append("无法获取应用信息: ").append(e.getMessage()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * 获取设备信息
     */
    private String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("设备制造商: ").append(Build.MANUFACTURER).append("\n");
        info.append("设备型号: ").append(Build.MODEL).append("\n");
        info.append("设备品牌: ").append(Build.BRAND).append("\n");
        info.append("设备产品: ").append(Build.PRODUCT).append("\n");
        info.append("设备ID: ").append(Build.ID).append("\n");
        info.append("硬件信息: ").append(Build.HARDWARE).append("\n");
        info.append("主板信息: ").append(Build.BOARD).append("\n");
        info.append("CPU架构: ").append(Build.CPU_ABI).append("\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info.append("支持的架构: ");
            for (String abi : Build.SUPPORTED_ABIS) {
                info.append(abi).append(" ");
            }
            info.append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * 获取系统信息
     */
    private String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("API级别: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("安全补丁: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        info.append("构建时间: ").append(new Date(Build.TIME)).append("\n");
        info.append("构建类型: ").append(Build.TYPE).append("\n");
        info.append("构建标签: ").append(Build.TAGS).append("\n");
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        info.append("最大内存: ").append(formatBytes(maxMemory)).append("\n");
        info.append("总内存: ").append(formatBytes(totalMemory)).append("\n");
        info.append("已用内存: ").append(formatBytes(usedMemory)).append("\n");
        info.append("可用内存: ").append(formatBytes(freeMemory)).append("\n");
        
        return info.toString();
    }
    
    /**
     * 获取网络信息
     */
    private String getNetworkInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("网络可用: ").append(NetworkUtils.isNetworkAvailable(context) ? "是" : "否").append("\n");
        info.append("WiFi连接: ").append(NetworkUtils.isWifiConnected(context) ? "是" : "否").append("\n");
        info.append("移动网络: ").append(NetworkUtils.isMobileConnected(context) ? "是" : "否").append("\n");
        info.append("网络类型: ").append(NetworkUtils.getNetworkTypeName(context)).append("\n");
        
        return info.toString();
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 获取错误统计信息
     */
    private String getErrorStatistics() {
        StringBuilder info = new StringBuilder();
        
        Map<TranscriptionError, Integer> statistics = logManager.getErrorStatistics();
        if (statistics.isEmpty()) {
            info.append("暂无错误统计数据\n");
        } else {
            info.append("错误发生次数统计:\n");
            for (Map.Entry<TranscriptionError, Integer> entry : statistics.entrySet()) {
                info.append("  ").append(entry.getKey().name())
                    .append(": ").append(entry.getValue()).append("次\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 保存错误报告到文件
     */
    public void saveErrorReport(TranscriptionError error, Throwable throwable, 
                              String additionalInfo) {
        try {
            String report = generateErrorReport(error, throwable, additionalInfo);
            String fileName = "error_report_" + System.currentTimeMillis() + ".txt";
            
            FileUtils.writeTextToFile(context, fileName, report);
            logManager.i(TAG, "错误报告已保存: " + fileName);
            
        } catch (Exception e) {
            logManager.e(TAG, "保存错误报告失败", e);
        }
    }
    
    /**
     * 生成简化的错误摘要
     */
    public String generateErrorSummary(TranscriptionError error, Throwable throwable) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("错误: ").append(error.getDefaultMessage()).append("\n");
        summary.append("时间: ").append(dateFormat.format(new Date())).append("\n");
        summary.append("设备: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        summary.append("系统: Android ").append(Build.VERSION.RELEASE).append("\n");
        
        if (throwable != null) {
            summary.append("异常: ").append(throwable.getClass().getSimpleName())
                   .append(" - ").append(throwable.getMessage()).append("\n");
        }
        
        return summary.toString();
    }
}