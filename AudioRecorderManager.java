// AudioRecorderManager.java
package com.example.offlinecantoneseasr;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AudioRecorderManager {
    private static final String TAG = "AudioRecorderManager";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private File outputFile;
    private Context context;

    private List<short[]> recordedChunks = new ArrayList<>();
    private int totalRecordedSamples = 0;

    // 录音状态回调接口
    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped(File audioFile);
        void onRecordingError(String errorMessage);
        void onRecordingProgress(int durationInSeconds);
    }

    private RecordingCallback recordingCallback;

    public AudioRecorderManager(Context context) {
        this.context = context;
    }

    public void setRecordingCallback(RecordingCallback callback) {
        this.recordingCallback = callback;
    }

    /**
     * 安全地开始录音
     */
    public void startRecording() {
        // 检查录音权限
        if (!hasAudioPermission()) {
            Log.e(TAG, "没有录音权限");
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("没有录音权限，请授予录音权限后重试");
            }
            return;
        }

        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "无效的音频参数");
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("音频参数无效");
                }
                return;
            }

            // 创建AudioRecord实例
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
            );

            // 检查AudioRecord状态
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord初始化失败");
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("音频录制器初始化失败");
                }
                releaseAudioRecord();
                return;
            }

            // 重置录音数据
            recordedChunks.clear();
            totalRecordedSamples = 0;

            // 开始录音
            try {
                audioRecord.startRecording();
                isRecording = true;

                Log.d(TAG, "开始录制音频");

                if (recordingCallback != null) {
                    recordingCallback.onRecordingStarted();
                }

                // 开始录音线程
                startRecordingThread();

            } catch (IllegalStateException e) {
                Log.e(TAG, "AudioRecord状态异常: " + e.getMessage());
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("音频录制器状态异常");
                }
                releaseAudioRecord();
            }

        } catch (SecurityException e) {
            Log.e(TAG, "安全异常 - 没有录音权限: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("没有录音权限，请检查应用权限设置");
            }
            releaseAudioRecord();
        } catch (Exception e) {
            Log.e(TAG, "开始录音时发生异常: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("开始录音时发生错误");
            }
            releaseAudioRecord();
        }
    }

    /**
     * 安全地停止录音
     */
    public void stopRecording() {
        if (!isRecording || audioRecord == null) {
            Log.d(TAG, "录音未在进行中，无需停止");
            return;
        }

        isRecording = false;

        try {
            // 停止录音
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }

            Log.d(TAG, "停止录制音频，总样本数: " + totalRecordedSamples);

            // 保存录音文件
            File audioFile = saveRecordingToFile();

            if (recordingCallback != null) {
                recordingCallback.onRecordingStopped(audioFile);
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "停止录音时状态异常: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("停止录音时发生错误");
            }
        } finally {
            releaseAudioRecord();
        }
    }

    /**
     * 保存录音到文件 - 改为公共方法
     */
    public File saveRecordingToFile() {
        if (recordedChunks.isEmpty()) {
            Log.w(TAG, "没有录音数据可保存");
            return null;
        }

        try {
            short[] audioData = getRecordedAudio();
            if (audioData == null || audioData.length == 0) {
                Log.w(TAG, "音频数据为空");
                return null;
            }

            outputFile = createAudioFile();
            if (outputFile == null) {
                Log.e(TAG, "创建音频文件失败");
                return null;
            }

            // 保存为PCM文件
            saveAsPcmFile(audioData, outputFile);
            Log.d(TAG, "音频保存到: " + outputFile.getAbsolutePath());

            return outputFile;

        } catch (SecurityException e) {
            Log.e(TAG, "保存文件时权限不足: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("保存录音文件时权限不足");
            }
            return null;
        } catch (IOException e) {
            Log.e(TAG, "保存文件时IO错误: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("保存录音文件时发生错误");
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "保存文件时未知错误: " + e.getMessage());
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("保存录音文件失败");
            }
            return null;
        }
    }

    /**
     * 启动录音线程
     */
    private void startRecordingThread() {
        new Thread(() -> {
            int bufferSize = 4096;
            short[] buffer = new short[bufferSize];
            long startTime = System.currentTimeMillis();

            while (isRecording && audioRecord != null) {
                try {
                    int samplesRead = audioRecord.read(buffer, 0, bufferSize);

                    if (samplesRead > 0) {
                        // 复制数据到新数组
                        short[] chunk = new short[samplesRead];
                        System.arraycopy(buffer, 0, chunk, 0, samplesRead);
                        recordedChunks.add(chunk);
                        totalRecordedSamples += samplesRead;

                        // 计算录音时长并回调
                        long currentTime = System.currentTimeMillis();
                        int duration = (int) ((currentTime - startTime) / 1000);
                        if (recordingCallback != null) {
                            recordingCallback.onRecordingProgress(duration);
                        }

                    } else if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord读取错误: ERROR_INVALID_OPERATION");
                        break;
                    } else if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "AudioRecord读取错误: ERROR_BAD_VALUE");
                        break;
                    } else if (samplesRead == AudioRecord.ERROR) {
                        Log.e(TAG, "AudioRecord读取错误: ERROR");
                        break;
                    }

                } catch (IllegalStateException e) {
                    Log.e(TAG, "录音线程状态异常: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "录音线程异常: " + e.getMessage());
                    break;
                }
            }

            // 如果录音意外停止，通知回调
            if (isRecording) {
                isRecording = false;
                Log.w(TAG, "录音意外停止");
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("录音过程意外中断");
                }
                releaseAudioRecord();
            }
        }).start();
    }

    /**
     * 获取录制的音频数据 - 改为公共方法
     */
    public short[] getRecordedAudio() {
        if (recordedChunks.isEmpty()) {
            return new short[0];
        }

        short[] fullAudio = new short[totalRecordedSamples];
        int position = 0;

        for (short[] chunk : recordedChunks) {
            System.arraycopy(chunk, 0, fullAudio, position, chunk.length);
            position += chunk.length;
        }

        return fullAudio;
    }

    /**
     * 创建音频文件
     */
    private File createAudioFile() {
        try {
            File storageDir;

            // 优先使用外部存储
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            } else {
                storageDir = context.getFilesDir();
            }

            if (storageDir == null) {
                Log.e(TAG, "无法获取存储目录");
                return null;
            }

            // 确保目录存在
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Log.e(TAG, "创建存储目录失败: " + storageDir.getAbsolutePath());
                return null;
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "cantonese_audio_" + timeStamp + ".pcm";

            return new File(storageDir, fileName);

        } catch (Exception e) {
            Log.e(TAG, "创建音频文件时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存为PCM文件
     */
    private void saveAsPcmFile(short[] audioData, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 将short数组转换为byte数组
            byte[] byteData = new byte[audioData.length * 2];
            for (int i = 0; i < audioData.length; i++) {
                byteData[2 * i] = (byte) (audioData[i] & 0xff);
                byteData[2 * i + 1] = (byte) ((audioData[i] >> 8) & 0xff);
            }
            fos.write(byteData);
            fos.flush();
        }
    }

    /**
     * 释放AudioRecord资源
     */
    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "释放AudioRecord资源时出错: " + e.getMessage());
            } finally {
                audioRecord = null;
            }
        }
    }

    /**
     * 检查是否有录音权限
     */
    private boolean hasAudioPermission() {
        try {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "检查录音权限时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取输出文件
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * 检查是否正在录音
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * 获取录音时长（秒）
     */
    public int getRecordingDuration() {
        return totalRecordedSamples / SAMPLE_RATE;
    }

    /**
     * 获取录音数据大小
     */
    public int getRecordingDataSize() {
        return totalRecordedSamples;
    }

    /**
     * 手动保存录音文件（新增的公共方法）
     */
    public File manualSaveRecording() {
        return saveRecordingToFile();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        stopRecording();
        recordedChunks.clear();
        totalRecordedSamples = 0;
        outputFile = null;
        recordingCallback = null;
    }
}