package com.example.cantonesevoicerecognition.utils;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    private static final String AUDIO_DIRECTORY = "audio";
    private static final String AUDIO_FILE_EXTENSION = ".wav";
    
    /**
     * Get the audio directory for storing recorded files
     * @param context Application context
     * @return Audio directory file
     */
    public static File getAudioDirectory(Context context) {
        File audioDir = new File(context.getFilesDir(), AUDIO_DIRECTORY);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        return audioDir;
    }
    
    /**
     * Generate a unique filename for audio recording
     * @return Unique filename with timestamp
     */
    public static String generateAudioFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "audio_" + sdf.format(new Date()) + AUDIO_FILE_EXTENSION;
    }
    
    /**
     * Create a new audio file
     * @param context Application context
     * @return New audio file
     */
    public static File createAudioFile(Context context) {
        File audioDir = getAudioDirectory(context);
        String fileName = generateAudioFileName();
        return new File(audioDir, fileName);
    }
    
    /**
     * Get file size in bytes
     * @param file File to check
     * @return File size in bytes
     */
    public static long getFileSize(File file) {
        if (file != null && file.exists()) {
            return file.length();
        }
        return 0;
    }
    
    /**
     * Delete file if it exists
     * @param file File to delete
     * @return true if deleted successfully, false otherwise
     */
    public static boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }
    
    /**
     * Copy file from source to destination
     * @param source Source file
     * @param destination Destination file
     * @throws IOException if copy fails
     */
    public static void copyFile(File source, File destination) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
}