package com.example.offlinecantoneseasr;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

// TensorFlowLiteHelper.java
public class TensorFlowLiteHelper {
    private static final String TAG = "TFLiteHelper";

    public static Interpreter loadModel(Context context, String modelPath) {
        try {
            // 从assets加载模型
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // 使用4个线程加速推理
            options.setUseNNAPI(true); // 启用NNAPI加速

            return new Interpreter(modelBuffer, options);

        } catch (Exception e) {
            Log.e(TAG, "加载TFLite模型失败: " + e.getMessage());
            return null;
        }
    }

    public static String loadTokenizer(Context context, String tokenizerPath) {
        try {
            InputStream inputStream = context.getAssets().open(tokenizerPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            reader.close();
            return content.toString();

        } catch (Exception e) {
            Log.e(TAG, "加载tokenizer失败: " + e.getMessage());
            return null;
        }
    }
}
