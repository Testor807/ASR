package com.example.offlinecantoneseasr;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    private static final String TAG = "PermissionManager";

    // 所需权限列表
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // 可选的额外权限
    private static final String[] OPTIONAL_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
    };

    private Activity activity;
    private PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied(List<String> deniedPermissions);
        void onShowRationale(List<String> permissions);
    }

    public PermissionManager(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(PermissionCallback callback) {
        this.callback = callback;
    }

    /**
     * 检查所有必需权限是否已授予
     */
    public boolean hasAllRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查单个权限是否已授予
     */
    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查录音权限（最重要的权限）
     */
    public boolean hasAudioPermission() {
        return hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    /**
     * 检查存储权限
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用分区存储，不需要READ_EXTERNAL_STORAGE
            return true;
        }
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    /**
     * 请求所有必需权限
     */
    public void requestRequiredPermissions() {
        requestPermissions(REQUIRED_PERMISSIONS);
    }

    /**
     * 只请求录音权限
     */
    public void requestAudioPermission() {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO});
    }

    /**
     * 请求存储权限
     */
    public void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 不需要这些权限
            if (callback != null) {
                callback.onPermissionsGranted();
            }
            return;
        }

        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        });
    }

    /**
     * 请求特定权限
     */
    public void requestPermissions(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "所有权限已授予");
            if (callback != null) {
                callback.onPermissionsGranted();
            }
            return;
        }

        // 检查是否需要显示权限说明
        List<String> shouldShowRationale = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale.add(permission);
            }
        }

        if (!shouldShowRationale.isEmpty() && callback != null) {
            Log.d(TAG, "需要显示权限说明: " + shouldShowRationale);
            callback.onShowRationale(shouldShowRationale);
            return;
        }

        // 请求权限
        String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
        ActivityCompat.requestPermissions(activity, permissionsArray, getRequestCode(permissionsArray));
        Log.d(TAG, "请求权限: " + String.join(", ", permissionsToRequest));
    }

    /**
     * 处理权限请求结果
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "权限请求结果处理");

        List<String> grantedPermissions = new ArrayList<>();
        List<String> deniedPermissions = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
                Log.d(TAG, "权限授予: " + permission);
            } else {
                deniedPermissions.add(permission);
                Log.d(TAG, "权限拒绝: " + permission);
            }
        }

        if (callback == null) {
            return;
        }

        if (deniedPermissions.isEmpty()) {
            Log.d(TAG, "所有请求的权限都已授予");
            callback.onPermissionsGranted();
        } else {
            Log.d(TAG, "部分权限被拒绝: " + deniedPermissions);

            // 检查是否所有必需权限都被授予
            boolean allRequiredGranted = true;
            for (String requiredPermission : REQUIRED_PERMISSIONS) {
                if (!hasPermission(requiredPermission)) {
                    allRequiredGranted = false;
                    break;
                }
            }

            if (allRequiredGranted) {
                Log.d(TAG, "所有必需权限已授予，可选权限被拒绝");
                callback.onPermissionsGranted();
            } else {
                Log.d(TAG, "必需权限被拒绝");
                callback.onPermissionsDenied(deniedPermissions);
            }
        }
    }

    /**
     * 获取缺失的必需权限
     */
    public List<String> getMissingRequiredPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions;
    }

    /**
     * 获取缺失的所有权限（包括可选）
     */
    public List<String> getAllMissingPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        // 添加缺失的必需权限
        missingPermissions.addAll(getMissingRequiredPermissions());

        // 添加缺失的可选权限
        for (String permission : OPTIONAL_PERMISSIONS) {
            if (!hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions;
    }

    /**
     * 检查是否应该显示权限说明
     */
    public boolean shouldShowRationale(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * 获取权限的显示名称
     */
    public String getPermissionDisplayName(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "录音权限";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "写入存储权限";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "读取存储权限";
            case Manifest.permission.CAMERA:
                return "相机权限";
            case Manifest.permission.CALL_PHONE:
                return "打电话权限";
            case Manifest.permission.SEND_SMS:
                return "发送短信权限";
            default:
                return permission;
        }
    }

    /**
     * 获取权限说明信息
     */
    public String getPermissionExplanation(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "需要录音权限来识别您的粤语语音指令";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "需要存储权限来保存录音文件";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "需要存储权限来读取录音文件";
            case Manifest.permission.CAMERA:
                return "需要相机权限来执行拍照指令";
            case Manifest.permission.CALL_PHONE:
                return "需要电话权限来执行打电话指令";
            case Manifest.permission.SEND_SMS:
                return "需要短信权限来执行发信息指令";
            default:
                return "需要此权限来提供完整功能";
        }
    }

    /**
     * 生成请求码
     */
    private int getRequestCode(String[] permissions) {
        if (permissions.length == 1 && permissions[0].equals(Manifest.permission.RECORD_AUDIO)) {
            return 1001;
        } else if (permissions.length == 2 &&
                permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                permissions[1].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return 1002;
        } else {
            int code = 0;
            for (String permission : permissions) {
                code += permission.hashCode();
            }
            return Math.abs(code) % 10000 + 1000;
        }
    }

    /**
     * 清理资源
     */
    public void destroy() {
        activity = null;
        callback = null;
    }
}
