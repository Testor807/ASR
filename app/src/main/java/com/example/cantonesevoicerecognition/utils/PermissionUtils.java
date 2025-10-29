package com.example.cantonesevoicerecognition.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility class for handling permissions
 */
public class PermissionUtils {
    
    /**
     * Check if audio recording permission is granted
     * @param context Application context
     * @return true if permission granted, false otherwise
     */
    public static boolean hasAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if storage permission is granted
     * @param context Application context
     * @return true if permission granted, false otherwise
     */
    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request audio recording permission
     * @param activity Activity instance
     */
    public static void requestAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.RECORD_AUDIO},
            Constants.PERMISSION_REQUEST_RECORD_AUDIO);
    }
    
    /**
     * Request storage permission
     * @param activity Activity instance
     */
    public static void requestStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
            Constants.PERMISSION_REQUEST_STORAGE);
    }
    
    /**
     * Check if all required permissions are granted
     * @param context Application context
     * @return true if all permissions granted, false otherwise
     */
    public static boolean hasAllPermissions(Context context) {
        return hasAudioPermission(context) && hasStoragePermission(context);
    }
}