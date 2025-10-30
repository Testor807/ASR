package com.example.cantonesevoicerecognition.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

/**
 * 权限管理工具类 - 处理应用所需的各种权限
 */
public class PermissionUtils {
    
    private static final String TAG = "PermissionUtils";
    
    // 权限组定义
    public static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    public static final String[] OPTIONAL_PERMISSIONS = {
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
    };
    
    /**
     * 检查录音权限是否已授权
     * @param context 应用上下文
     * @return true表示已授权，false表示未授权
     */
    public static boolean hasAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查存储权限是否已授权
     * @param context 应用上下文
     * @return true表示已授权，false表示未授权
     */
    public static boolean hasStoragePermission(Context context) {
        // Android 10及以上版本不需要存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查前台服务权限是否已授权
     * @param context 应用上下文
     * @return true表示已授权，false表示未授权
     */
    public static boolean hasForegroundServicePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 低版本不需要此权限
    }
    
    /**
     * 检查麦克风前台服务权限是否已授权
     * @param context 应用上下文
     * @return true表示已授权，false表示未授权
     */
    public static boolean hasMicrophoneForegroundServicePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 低版本不需要此权限
    }
    
    /**
     * 检查所有必需权限是否已授权
     * @param context 应用上下文
     * @return true表示所有权限已授权，false表示有权限未授权
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        return hasAudioPermission(context) && hasStoragePermission(context);
    }
    
    /**
     * 检查所有权限（包括可选权限）是否已授权
     * @param context 应用上下文
     * @return true表示所有权限已授权，false表示有权限未授权
     */
    public static boolean hasAllPermissions(Context context) {
        return hasAllRequiredPermissions(context) && 
               hasForegroundServicePermission(context) && 
               hasMicrophoneForegroundServicePermission(context);
    }
    
    /**
     * 获取未授权的必需权限列表
     * @param context 应用上下文
     * @return 未授权的权限数组
     */
    public static String[] getMissingRequiredPermissions(Context context) {
        java.util.List<String> missingPermissions = new java.util.ArrayList<>();
        
        if (!hasAudioPermission(context)) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (!hasStoragePermission(context) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        return missingPermissions.toArray(new String[0]);
    }
    
    /**
     * 请求录音权限
     * @param activity Activity实例
     */
    public static void requestAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.RECORD_AUDIO},
            Constants.PERMISSION_REQUEST_RECORD_AUDIO);
    }
    
    /**
     * 请求存储权限
     * @param activity Activity实例
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                },
                Constants.PERMISSION_REQUEST_STORAGE);
        }
    }
    
    /**
     * 请求所有必需权限
     * @param activity Activity实例
     */
    public static void requestAllRequiredPermissions(Activity activity) {
        String[] missingPermissions = getMissingRequiredPermissions(activity);
        if (missingPermissions.length > 0) {
            ActivityCompat.requestPermissions(activity, missingPermissions, 
                Constants.PERMISSION_REQUEST_ALL);
        }
    }
    
    /**
     * 检查权限是否被永久拒绝
     * @param activity Activity实例
     * @param permission 权限名称
     * @return true表示被永久拒绝，false表示可以再次请求
     */
    public static boolean isPermissionPermanentlyDenied(Activity activity, String permission) {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
               ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查是否应该显示权限说明
     * @param activity Activity实例
     * @param permission 权限名称
     * @return true表示应该显示说明，false表示不需要
     */
    public static boolean shouldShowPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
    
    /**
     * 显示权限说明对话框
     * @param activity Activity实例
     * @param permission 权限名称
     * @param onPositive 用户点击确定的回调
     * @param onNegative 用户点击取消的回调
     */
    public static void showPermissionRationaleDialog(Activity activity, String permission, 
                                                   Runnable onPositive, Runnable onNegative) {
        String title = "需要权限";
        String message = getPermissionRationaleMessage(permission);
        
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("授权", (dialog, which) -> {
                    if (onPositive != null) onPositive.run();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    if (onNegative != null) onNegative.run();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * 显示权限被永久拒绝的对话框
     * @param activity Activity实例
     * @param permission 权限名称
     */
    public static void showPermissionPermanentlyDeniedDialog(Activity activity, String permission) {
        String title = "权限被拒绝";
        String message = getPermissionDeniedMessage(permission) + "\n\n请在设置中手动授权。";
        
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("去设置", (dialog, which) -> openAppSettings(activity))
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 获取权限说明信息
     * @param permission 权限名称
     * @return 权限说明文本
     */
    private static String getPermissionRationaleMessage(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "应用需要录音权限来捕获您的语音输入，这是语音识别功能的核心需求。";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "应用需要存储权限来保存转录记录和音频文件，确保您的数据不会丢失。";
            case Manifest.permission.FOREGROUND_SERVICE:
                return "应用需要前台服务权限来在后台持续进行语音转录。";
            case Manifest.permission.FOREGROUND_SERVICE_MICROPHONE:
                return "应用需要麦克风前台服务权限来在后台使用麦克风进行实时转录。";
            default:
                return "应用需要此权限来正常工作。";
        }
    }
    
    /**
     * 获取权限被拒绝的提示信息
     * @param permission 权限名称
     * @return 权限被拒绝的提示文本
     */
    private static String getPermissionDeniedMessage(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "没有录音权限，无法进行语音识别。";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "没有存储权限，无法保存转录记录。";
            case Manifest.permission.FOREGROUND_SERVICE:
                return "没有前台服务权限，无法在后台运行。";
            case Manifest.permission.FOREGROUND_SERVICE_MICROPHONE:
                return "没有麦克风前台服务权限，无法在后台使用麦克风。";
            default:
                return "权限被拒绝，部分功能可能无法使用。";
        }
    }
    
    /**
     * 打开应用设置页面
     * @param activity Activity实例
     */
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
    
    /**
     * 处理权限请求结果
     * @param activity Activity实例
     * @param requestCode 请求代码
     * @param permissions 权限数组
     * @param grantResults 授权结果数组
     * @param callback 结果回调
     */
    public static void handlePermissionResult(Activity activity, int requestCode, 
                                            String[] permissions, int[] grantResults, 
                                            PermissionResultCallback callback) {
        if (permissions.length == 0 || grantResults.length == 0) {
            if (callback != null) callback.onPermissionDenied(new String[0]);
            return;
        }
        
        java.util.List<String> grantedPermissions = new java.util.ArrayList<>();
        java.util.List<String> deniedPermissions = new java.util.ArrayList<>();
        java.util.List<String> permanentlyDeniedPermissions = new java.util.ArrayList<>();
        
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
            } else {
                deniedPermissions.add(permission);
                if (isPermissionPermanentlyDenied(activity, permission)) {
                    permanentlyDeniedPermissions.add(permission);
                }
            }
        }
        
        if (callback != null) {
            if (deniedPermissions.isEmpty()) {
                callback.onAllPermissionsGranted();
            } else {
                callback.onPermissionDenied(deniedPermissions.toArray(new String[0]));
                if (!permanentlyDeniedPermissions.isEmpty()) {
                    callback.onPermissionPermanentlyDenied(permanentlyDeniedPermissions.toArray(new String[0]));
                }
            }
        }
    }
    
    /**
     * 权限请求结果回调接口
     */
    public interface PermissionResultCallback {
        /**
         * 所有权限都已授权
         */
        void onAllPermissionsGranted();
        
        /**
         * 有权限被拒绝
         * @param deniedPermissions 被拒绝的权限数组
         */
        void onPermissionDenied(String[] deniedPermissions);
        
        /**
         * 有权限被永久拒绝
         * @param permanentlyDeniedPermissions 被永久拒绝的权限数组
         */
        default void onPermissionPermanentlyDenied(String[] permanentlyDeniedPermissions) {
            // 默认实现为空
        }
    }
}