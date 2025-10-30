package com.example.cantonesevoicerecognition.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.cantonesevoicerecognition.R;

/**
 * 权限管理器 - 提供完整的权限管理功能
 */
public class PermissionManager {
    
    private static final String TAG = "PermissionManager";
    
    private Activity activity;
    private PermissionManagerCallback callback;
    private AlertDialog permissionDialog;
    
    public PermissionManager(Activity activity) {
        this.activity = activity;
    }
    
    /**
     * 设置权限管理回调
     * @param callback 回调接口
     */
    public void setCallback(PermissionManagerCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 检查并请求所有必需权限
     */
    public void checkAndRequestPermissions() {
        if (PermissionUtils.hasAllRequiredPermissions(activity)) {
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }
            return;
        }
        
        // 显示权限引导对话框
        showPermissionGuideDialog();
    }
    
    /**
     * 显示权限引导对话框
     */
    private void showPermissionGuideDialog() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            return;
        }
        
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_permission_guide, null);
        
        // 更新权限状态显示
        updatePermissionStatus(dialogView);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        permissionDialog = builder.create();
        
        // 设置按钮点击事件
        setupDialogButtons(dialogView);
        
        permissionDialog.show();
    }
    
    /**
     * 更新权限状态显示
     * @param dialogView 对话框视图
     */
    private void updatePermissionStatus(View dialogView) {
        TextView audioStatus = dialogView.findViewById(R.id.tv_audio_permission_status);
        TextView storageStatus = dialogView.findViewById(R.id.tv_storage_permission_status);
        TextView foregroundServiceStatus = dialogView.findViewById(R.id.tv_foreground_service_permission_status);
        View storagePermissionLayout = dialogView.findViewById(R.id.ll_storage_permission);
        
        // 更新录音权限状态
        if (PermissionUtils.hasAudioPermission(activity)) {
            audioStatus.setText("已授权");
            audioStatus.setTextColor(ContextCompat.getColor(activity, R.color.success_color));
        } else {
            audioStatus.setText("未授权");
            audioStatus.setTextColor(ContextCompat.getColor(activity, R.color.error_color));
        }
        
        // 更新存储权限状态
        if (PermissionUtils.hasStoragePermission(activity)) {
            storageStatus.setText("已授权");
            storageStatus.setTextColor(ContextCompat.getColor(activity, R.color.success_color));
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10及以上版本不需要存储权限
                storageStatus.setText("不需要");
                storageStatus.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
                storagePermissionLayout.setVisibility(View.GONE);
            } else {
                storageStatus.setText("未授权");
                storageStatus.setTextColor(ContextCompat.getColor(activity, R.color.error_color));
            }
        }
        
        // 更新前台服务权限状态
        if (PermissionUtils.hasForegroundServicePermission(activity)) {
            foregroundServiceStatus.setText("已授权");
            foregroundServiceStatus.setTextColor(ContextCompat.getColor(activity, R.color.success_color));
        } else {
            foregroundServiceStatus.setText("未授权");
            foregroundServiceStatus.setTextColor(ContextCompat.getColor(activity, R.color.error_color));
        }
    }
    
    /**
     * 设置对话框按钮事件
     * @param dialogView 对话框视图
     */
    private void setupDialogButtons(View dialogView) {
        View cancelButton = dialogView.findViewById(R.id.btn_cancel_permission);
        View grantButton = dialogView.findViewById(R.id.btn_grant_permission);
        
        cancelButton.setOnClickListener(v -> {
            dismissDialog();
            if (callback != null) {
                callback.onPermissionDenied();
            }
        });
        
        grantButton.setOnClickListener(v -> {
            dismissDialog();
            requestMissingPermissions();
        });
    }
    
    /**
     * 请求缺失的权限
     */
    private void requestMissingPermissions() {
        String[] missingPermissions = PermissionUtils.getMissingRequiredPermissions(activity);
        if (missingPermissions.length > 0) {
            PermissionUtils.requestAllRequiredPermissions(activity);
        }
    }
    
    /**
     * 处理权限请求结果
     * @param requestCode 请求代码
     * @param permissions 权限数组
     * @param grantResults 授权结果数组
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionUtils.handlePermissionResult(activity, requestCode, permissions, grantResults, 
            new PermissionUtils.PermissionResultCallback() {
                @Override
                public void onAllPermissionsGranted() {
                    if (callback != null) {
                        callback.onAllPermissionsGranted();
                    }
                }
                
                @Override
                public void onPermissionDenied(String[] deniedPermissions) {
                    handlePermissionDenied(deniedPermissions);
                }
                
                @Override
                public void onPermissionPermanentlyDenied(String[] permanentlyDeniedPermissions) {
                    handlePermissionPermanentlyDenied(permanentlyDeniedPermissions);
                }
            });
    }
    
    /**
     * 处理权限被拒绝
     * @param deniedPermissions 被拒绝的权限
     */
    private void handlePermissionDenied(String[] deniedPermissions) {
        if (deniedPermissions.length == 0) {
            return;
        }
        
        // 检查是否需要显示权限说明
        boolean shouldShowRationale = false;
        for (String permission : deniedPermissions) {
            if (PermissionUtils.shouldShowPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
                break;
            }
        }
        
        if (shouldShowRationale) {
            showPermissionRationaleDialog(deniedPermissions);
        } else {
            if (callback != null) {
                callback.onPermissionDenied();
            }
        }
    }
    
    /**
     * 处理权限被永久拒绝
     * @param permanentlyDeniedPermissions 被永久拒绝的权限
     */
    private void handlePermissionPermanentlyDenied(String[] permanentlyDeniedPermissions) {
        if (permanentlyDeniedPermissions.length == 0) {
            return;
        }
        
        showPermissionPermanentlyDeniedDialog(permanentlyDeniedPermissions);
    }
    
    /**
     * 显示权限说明对话框
     * @param deniedPermissions 被拒绝的权限
     */
    private void showPermissionRationaleDialog(String[] deniedPermissions) {
        StringBuilder message = new StringBuilder();
        message.append("应用需要以下权限才能正常工作：\n\n");
        
        for (String permission : deniedPermissions) {
            switch (permission) {
                case android.Manifest.permission.RECORD_AUDIO:
                    message.append("• 录音权限：用于语音识别\n");
                    break;
                case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                case android.Manifest.permission.READ_EXTERNAL_STORAGE:
                    message.append("• 存储权限：用于保存转录记录\n");
                    break;
            }
        }
        
        message.append("\n请授权后继续使用。");
        
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.permission_title))
                .setMessage(message.toString())
                .setPositiveButton(activity.getString(R.string.button_grant_permission), 
                    (dialog, which) -> requestMissingPermissions())
                .setNegativeButton(activity.getString(R.string.button_cancel), 
                    (dialog, which) -> {
                        if (callback != null) {
                            callback.onPermissionDenied();
                        }
                    })
                .setCancelable(false)
                .show();
    }
    
    /**
     * 显示权限被永久拒绝对话框
     * @param permanentlyDeniedPermissions 被永久拒绝的权限
     */
    private void showPermissionPermanentlyDeniedDialog(String[] permanentlyDeniedPermissions) {
        StringBuilder message = new StringBuilder();
        message.append("以下权限被永久拒绝：\n\n");
        
        for (String permission : permanentlyDeniedPermissions) {
            switch (permission) {
                case android.Manifest.permission.RECORD_AUDIO:
                    message.append("• 录音权限\n");
                    break;
                case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                case android.Manifest.permission.READ_EXTERNAL_STORAGE:
                    message.append("• 存储权限\n");
                    break;
            }
        }
        
        message.append("\n请在设置中手动授权后重新启动应用。");
        
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.permission_permanently_denied_title))
                .setMessage(message.toString())
                .setPositiveButton(activity.getString(R.string.button_go_to_settings), 
                    (dialog, which) -> {
                        PermissionUtils.openAppSettings(activity);
                        if (callback != null) {
                            callback.onPermissionPermanentlyDenied();
                        }
                    })
                .setNegativeButton(activity.getString(R.string.button_exit), 
                    (dialog, which) -> {
                        if (callback != null) {
                            callback.onPermissionPermanentlyDenied();
                        }
                    })
                .setCancelable(false)
                .show();
    }
    
    /**
     * 关闭对话框
     */
    private void dismissDialog() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
            permissionDialog = null;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        dismissDialog();
        activity = null;
        callback = null;
    }
    
    /**
     * 权限管理回调接口
     */
    public interface PermissionManagerCallback {
        /**
         * 所有权限都已授权
         */
        void onAllPermissionsGranted();
        
        /**
         * 权限被拒绝
         */
        void onPermissionDenied();
        
        /**
         * 权限被永久拒绝
         */
        void onPermissionPermanentlyDenied();
    }
}