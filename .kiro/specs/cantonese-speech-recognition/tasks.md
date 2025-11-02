# 粤语语音识别Android应用 - 实施计划

## 📋 项目概述

本项目开发一个基于OpenAI Whisper模型的粤语语音识别Android应用，支持实时转录、离线模式和历史记录管理。使用Java语言开发，采用MVVM架构模式，为用户提供高效的粤语语音识别体验。

### 🎯 核心功能
- 粤语语音录制和转录
- 实时语音识别
- 离线模式支持
- 转录历史记录管理
- 用户友好的界面设计

### 📊 技术栈
- **开发语言**: Java
- **最低API**: Android 7.0 (API 24)
- **数据库**: Room
- **语音引擎**: OpenAI Whisper (JNI集成)
- **架构**: MVVM + Repository

## 🚀 开发环境准备

### 必需工具
1. **Android Studio** (最新稳定版)
2. **Android SDK** (API 24-34)
3. **NDK** (用于Whisper C++集成)
4. **CMake** (用于native代码构建)

### 开发前准备
1. 创建新的Android项目
2. 配置NDK和CMake支持
3. 添加必要的依赖库
4. 设置项目包结构

---

## 📋 实施任务清单

### 阶段一：项目基础搭建

- [ ] **1. 创建Android项目和基础配置**
  
  **目标**: 搭建项目基础架构，配置开发环境
  
  **实施步骤**:
  
  1. **创建Android项目**
     - 在Android Studio中创建新项目
     - 选择"Empty Activity"模板
     - 设置包名: `com.example.cantonesevoicerecognition`
     - 最低SDK版本: API 24 (Android 7.0)
     - 目标SDK版本: API 34 
 
  2. **配置build.gradle (Module: app)**
     ```gradle
     android {
         compileSdk 34
         ndkVersion "25.1.8937393"
         
         defaultConfig {
             applicationId "com.example.cantonesevoicerecognition"
             minSdk 24
             targetSdk 34
             versionCode 1
             versionName "1.0"
             
             ndk {
                 abiFilters 'arm64-v8a', 'armeabi-v7a'
             }
         }
         
         externalNativeBuild {
             cmake {
                 path "src/main/cpp/CMakeLists.txt"
                 version "3.22.1"
             }
         }
     }
     
     dependencies {
         // Room数据库
         implementation "androidx.room:room-runtime:2.5.0"
         annotationProcessor "androidx.room:room-compiler:2.5.0"
         
         // Material Design
         implementation 'com.google.android.material:material:1.10.0'
         
         // ViewModel和LiveData
         implementation "androidx.lifecycle:lifecycle-viewmodel:2.7.0"
         implementation "androidx.lifecycle:lifecycle-livedata:2.7.0"
         
         // 权限处理
         implementation 'androidx.core:core:1.12.0'
     }
     ```
  
  3. **配置AndroidManifest.xml权限**
     ```xml
     <uses-permission android:name="android.permission.RECORD_AUDIO" />
     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
     ```
  
  4. **创建包结构**
     ```
     com.example.cantonesevoicerecognition/
     ├── data/
     │   ├── model/          # 数据模型
     │   ├── dao/            # 数据访问对象
     │   └── repository/     # 数据仓库
     ├── engine/             # Whisper引擎
     ├── audio/              # 音频处理
     ├── service/            # 后台服务
     ├── ui/                 # 用户界面
     │   ├── main/
     │   ├── history/
     │   └── settings/
     └── utils/              # 工具类
     ```
  
  5. **创建CMakeLists.txt**
     ```cmake
     cmake_minimum_required(VERSION 3.22.1)
     project("cantonese_voice")
     
     add_library(cantonese_voice SHARED
         cantonese_voice.cpp
         whisper_wrapper.cpp)
     
     find_library(log-lib log)
     target_link_libraries(cantonese_voice ${log-lib})
     ```
  
  **验收标准**: 
  - 项目成功编译无错误
  - 包结构创建完整
  - NDK配置正确
  - 权限声明完整
  
  _需求映射: 5.3, 5.4_
### 阶段二：数
据层实现

- [ ] **2. 创建数据模型类**
  
  **目标**: 实现应用的核心数据结构
  
  **实施步骤**:
  
  1. **创建TranscriptionRecord实体类** (`data/model/TranscriptionRecord.java`)
     ```java
     @Entity(tableName = "transcription_records")
     public class TranscriptionRecord {
         @PrimaryKey(autoGenerate = true)
         private long id;
         
         @ColumnInfo(name = "transcription_text")
         private String transcriptionText;
         
         @ColumnInfo(name = "created_at")
         private long createdAt;
         
         @ColumnInfo(name = "duration_ms")
         private int durationMs;
         
         @ColumnInfo(name = "confidence_score")
         private float confidenceScore;
         
         // 构造函数和getter/setter方法
         public TranscriptionRecord(String transcriptionText, long createdAt, 
                                   int durationMs, float confidenceScore) {
             this.transcriptionText = transcriptionText;
             this.createdAt = createdAt;
             this.durationMs = durationMs;
             this.confidenceScore = confidenceScore;
         }
         
         // 所有字段的getter和setter方法
     }
     ```
  
  2. **创建AudioData类** (`data/model/AudioData.java`)
     ```java
     public class AudioData {
         private byte[] rawData;
         private int sampleRate;
         private int channels;
         private long durationMs;
         
         public AudioData(byte[] rawData, int sampleRate, int channels) {
             this.rawData = rawData;
             this.sampleRate = sampleRate;
             this.channels = channels;
             this.durationMs = calculateDuration();
         }
         
         private long calculateDuration() {
             // 计算音频时长的逻辑
             return (rawData.length * 1000L) / (sampleRate * channels * 2);
         }
         
         // getter和setter方法
     }
     ```
  
  3. **创建TranscriptionResult类** (`data/model/TranscriptionResult.java`)
     ```java
     public class TranscriptionResult {
         private String text;
         private float confidence;
         private long processingTimeMs;
         private boolean isComplete;
         
         public TranscriptionResult(String text, float confidence, 
                                   long processingTimeMs, boolean isComplete) {
             this.text = text;
             this.confidence = confidence;
             this.processingTimeMs = processingTimeMs;
             this.isComplete = isComplete;
         }
         
         // getter和setter方法
     }
     ```
  
  **验收标准**: 数据模型类编译无错误，包含完整的构造函数和访问方法
  
  _需求映射: 4.1, 4.2_- [ ] **3. 
实现Room数据库**
  
  **目标**: 设置本地数据库存储
  
  **实施步骤**:
  
  1. **创建TranscriptionDao接口** (`data/dao/TranscriptionDao.java`)
     ```java
     @Dao
     public interface TranscriptionDao {
         @Query("SELECT * FROM transcription_records ORDER BY created_at DESC")
         LiveData<List<TranscriptionRecord>> getAllRecords();
         
         @Insert
         long insertRecord(TranscriptionRecord record);
         
         @Update
         void updateRecord(TranscriptionRecord record);
         
         @Delete
         void deleteRecord(TranscriptionRecord record);
         
         @Query("SELECT * FROM transcription_records WHERE transcription_text LIKE :searchQuery")
         LiveData<List<TranscriptionRecord>> searchRecords(String searchQuery);
     }
     ```
  
  2. **创建AppDatabase类** (`data/AppDatabase.java`)
     ```java
     @Database(entities = {TranscriptionRecord.class}, version = 1)
     public abstract class AppDatabase extends RoomDatabase {
         public abstract TranscriptionDao transcriptionDao();
         
         private static volatile AppDatabase INSTANCE;
         
         public static AppDatabase getDatabase(final Context context) {
             if (INSTANCE == null) {
                 synchronized (AppDatabase.class) {
                     if (INSTANCE == null) {
                         INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                 AppDatabase.class, "transcription_database")
                                 .build();
                     }
                 }
             }
             return INSTANCE;
         }
     }
     ```
  
  **验收标准**: 数据库能够正常创建，支持CRUD操作
  
  _需求映射: 4.1, 4.2, 4.5_

- [ ] **4. 实现Repository层**
  
  **目标**: 创建数据访问层，封装数据库操作
  
  **实施步骤**:
  
  1. **创建RepositoryCallback接口** (`data/repository/RepositoryCallback.java`)
     ```java
     public interface RepositoryCallback<T> {
         void onSuccess(T result);
         void onError(Exception error);
     }
     ```
  
  2. **创建TranscriptionRepository类** (`data/repository/TranscriptionRepository.java`)
     ```java
     public class TranscriptionRepository {
         private TranscriptionDao transcriptionDao;
         private ExecutorService executor;
         
         public TranscriptionRepository(Application application) {
             AppDatabase database = AppDatabase.getDatabase(application);
             transcriptionDao = database.transcriptionDao();
             executor = Executors.newFixedThreadPool(4);
         }
         
         public LiveData<List<TranscriptionRecord>> getAllRecords() {
             return transcriptionDao.getAllRecords();
         }
         
         public void insertRecord(TranscriptionRecord record, RepositoryCallback<Long> callback) {
             executor.execute(() -> {
                 try {
                     long id = transcriptionDao.insertRecord(record);
                     callback.onSuccess(id);
                 } catch (Exception e) {
                     callback.onError(e);
                 }
             });
         }
         
         public void deleteRecord(TranscriptionRecord record, RepositoryCallback<Void> callback) {
             executor.execute(() -> {
                 try {
                     transcriptionDao.deleteRecord(record);
                     callback.onSuccess(null);
                 } catch (Exception e) {
                     callback.onError(e);
                 }
             });
         }
         
         public LiveData<List<TranscriptionRecord>> searchRecords(String query) {
             return transcriptionDao.searchRecords("%" + query + "%");
         }
     }
     ```
  
  **验收标准**: Repository能够正确执行异步数据库操作，错误处理完善
  
  _需求映射: 4.2, 4.3, 4.4, 4.5_#
## 阶段三：音频处理模块

- [ ] **5. 实现音频录制管理器**
  
  **目标**: 创建音频录制和处理功能
  
  **实施步骤**:
  
  1. **创建AudioStreamListener接口** (`audio/AudioStreamListener.java`)
     ```java
     public interface AudioStreamListener {
         void onRecordingStarted();
         void onRecordingStopped();
         void onAudioDataAvailable(byte[] audioData);
         void onRecordingError(String error);
     }
     ```
  
  2. **创建AudioRecorderManager类** (`audio/AudioRecorderManager.java`)
     ```java
     public class AudioRecorderManager {
         private static final int SAMPLE_RATE = 16000; // Whisper推荐采样率
         private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
         private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
         
         private AudioRecord audioRecord;
         private AudioStreamListener listener;
         private Thread recordingThread;
         private volatile boolean isRecording = false;
         private int bufferSize;
         
         public AudioRecorderManager(Context context) {
             calculateBufferSize();
         }
         
         private void calculateBufferSize() {
             bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
             if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                 throw new RuntimeException("不支持的音频配置");
             }
             bufferSize *= 2; // 使用2倍缓冲区大小确保稳定性
         }
         
         public boolean startRecording() {
             if (isRecording) return true;
             
             try {
                 audioRecord = new AudioRecord(
                     MediaRecorder.AudioSource.MIC,
                     SAMPLE_RATE,
                     CHANNEL_CONFIG,
                     AUDIO_FORMAT,
                     bufferSize
                 );
                 
                 if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                     return false;
                 }
                 
                 audioRecord.startRecording();
                 isRecording = true;
                 
                 recordingThread = new Thread(this::recordingLoop);
                 recordingThread.start();
                 
                 if (listener != null) {
                     listener.onRecordingStarted();
                 }
                 
                 return true;
             } catch (Exception e) {
                 if (listener != null) {
                     listener.onRecordingError("录音启动失败: " + e.getMessage());
                 }
                 return false;
             }
         }
         
         public void stopRecording() {
             isRecording = false;
             
             if (recordingThread != null) {
                 try {
                     recordingThread.join(1000);
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
             }
             
             if (audioRecord != null) {
                 audioRecord.stop();
                 audioRecord.release();
                 audioRecord = null;
             }
             
             if (listener != null) {
                 listener.onRecordingStopped();
             }
         }
         
         private void recordingLoop() {
             byte[] buffer = new byte[bufferSize];
             
             while (isRecording) {
                 int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                 
                 if (bytesRead > 0) {
                     byte[] audioData = new byte[bytesRead];
                     System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                     
                     if (listener != null) {
                         listener.onAudioDataAvailable(audioData);
                     }
                 }
             }
         }
         
         public void setAudioStreamListener(AudioStreamListener listener) {
             this.listener = listener;
         }
         
         public boolean isRecording() {
             return isRecording;
         }
     }
     ```
  
  **验收标准**: 能够成功录制音频，正确处理权限，提供音频数据回调
  
  _需求映射: 1.1, 1.2, 2.1_- 
[ ] **6. 实现音频处理工具**
  
  **目标**: 创建音频格式转换和预处理功能
  
  **实施步骤**:
  
  1. **创建AudioProcessor类** (`audio/AudioProcessor.java`)
     ```java
     public class AudioProcessor {
         
         public static AudioData convertToWhisperFormat(byte[] audioData, int sampleRate) {
             // 转换为Whisper所需的16kHz单声道格式
             float[] floatData = convertBytesToFloat(audioData);
             
             // 如果采样率不是16kHz，进行重采样
             if (sampleRate != 16000) {
                 floatData = resample(floatData, sampleRate, 16000);
             }
             
             // 归一化音频数据
             floatData = normalize(floatData);
             
             return new AudioData(convertFloatToBytes(floatData), 16000, 1);
         }
         
         private static float[] convertBytesToFloat(byte[] audioData) {
             float[] floatData = new float[audioData.length / 2];
             for (int i = 0; i < floatData.length; i++) {
                 short sample = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
                 floatData[i] = sample / 32768.0f;
             }
             return floatData;
         }
         
         private static byte[] convertFloatToBytes(float[] floatData) {
             byte[] byteData = new byte[floatData.length * 2];
             for (int i = 0; i < floatData.length; i++) {
                 short sample = (short) (floatData[i] * 32767);
                 byteData[i * 2] = (byte) (sample & 0xFF);
                 byteData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
             }
             return byteData;
         }
         
         private static float[] normalize(float[] audioData) {
             float max = 0;
             for (float sample : audioData) {
                 max = Math.max(max, Math.abs(sample));
             }
             
             if (max > 0) {
                 for (int i = 0; i < audioData.length; i++) {
                     audioData[i] /= max;
                 }
             }
             
             return audioData;
         }
         
         private static float[] resample(float[] input, int inputRate, int outputRate) {
             if (inputRate == outputRate) return input;
             
             double ratio = (double) outputRate / inputRate;
             int outputLength = (int) (input.length * ratio);
             float[] output = new float[outputLength];
             
             for (int i = 0; i < outputLength; i++) {
                 double srcIndex = i / ratio;
                 int index = (int) srcIndex;
                 
                 if (index < input.length - 1) {
                     double fraction = srcIndex - index;
                     output[i] = (float) (input[index] * (1 - fraction) + input[index + 1] * fraction);
                 } else if (index < input.length) {
                     output[i] = input[index];
                 }
             }
             
             return output;
         }
         
         public static boolean detectVoiceActivity(byte[] audioData, float threshold) {
             float energy = calculateAudioEnergy(audioData);
             return energy > threshold;
         }
         
         private static float calculateAudioEnergy(byte[] audioData) {
             long sum = 0;
             for (int i = 0; i < audioData.length; i += 2) {
                 short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                 sum += sample * sample;
             }
             return (float) Math.sqrt(sum / (audioData.length / 2.0));
         }
     }
     ```
  
  **验收标准**: 音频数据能够正确转换为Whisper所需格式，VAD功能正常工作
  
  _需求映射: 1.3, 2.2, 3.3_### 阶
段四：Whisper引擎集成

- [ ] **7. 创建JNI接口**
  
  **目标**: 通过JNI集成Whisper C++库
  
  **实施步骤**:
  
  1. **创建WhisperJNI类** (`engine/WhisperJNI.java`)
     ```java
     public class WhisperJNI {
         static {
             System.loadLibrary("cantonese_voice");
         }
         
         public native boolean initializeModel(String modelPath);
         public native String transcribeAudio(float[] audioData, int length);
         public native void releaseModel();
         public native boolean isModelLoaded();
     }
     ```
  
  2. **创建C++实现文件** (`src/main/cpp/cantonese_voice.cpp`)
     ```cpp
     #include <jni.h>
     #include <string>
     #include <android/log.h>
     
     #define LOG_TAG "WhisperJNI"
     #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
     #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
     
     // 全局变量存储模型状态
     static bool model_loaded = false;
     
     extern "C" JNIEXPORT jboolean JNICALL
     Java_com_example_cantonesevoicerecognition_engine_WhisperJNI_initializeModel(
         JNIEnv *env, jobject thiz, jstring model_path) {
         
         const char *path = env->GetStringUTFChars(model_path, 0);
         LOGI("初始化Whisper模型: %s", path);
         
         // 这里应该集成实际的Whisper库
         // 目前返回true表示成功加载
         model_loaded = true;
         
         env->ReleaseStringUTFChars(model_path, path);
         return model_loaded;
     }
     
     extern "C" JNIEXPORT jstring JNICALL
     Java_com_example_cantonesevoicerecognition_engine_WhisperJNI_transcribeAudio(
         JNIEnv *env, jobject thiz, jfloatArray audio_data, jint length) {
         
         if (!model_loaded) {
             return env->NewStringUTF("模型未加载");
         }
         
         jfloat *audio = env->GetFloatArrayElements(audio_data, 0);
         
         // 这里应该调用实际的Whisper转录功能
         // 目前返回模拟结果
         std::string result = "这是模拟的粤语转录结果";
         
         env->ReleaseFloatArrayElements(audio_data, audio, 0);
         return env->NewStringUTF(result.c_str());
     }
     
     extern "C" JNIEXPORT void JNICALL
     Java_com_example_cantonesevoicerecognition_engine_WhisperJNI_releaseModel(
         JNIEnv *env, jobject thiz) {
         
         LOGI("释放Whisper模型");
         model_loaded = false;
     }
     
     extern "C" JNIEXPORT jboolean JNICALL
     Java_com_example_cantonesevoicerecognition_engine_WhisperJNI_isModelLoaded(
         JNIEnv *env, jobject thiz) {
         
         return model_loaded;
     }
     ```
  
  3. **更新CMakeLists.txt**
     ```cmake
     cmake_minimum_required(VERSION 3.22.1)
     project("cantonese_voice")
     
     add_library(cantonese_voice SHARED
         cantonese_voice.cpp)
     
     find_library(log-lib log)
     find_library(android-lib android)
     
     target_link_libraries(cantonese_voice
         ${log-lib}
         ${android-lib})
     ```
  
  **验收标准**: JNI接口正常工作，能够调用native方法
  
  _需求映射: 1.4, 3.3, 3.4_- [
 ] **8. 实现WhisperEngine类**
  
  **目标**: 创建Java层的Whisper引擎封装
  
  **实施步骤**:
  
  1. **创建TranscriptionCallback接口** (`engine/TranscriptionCallback.java`)
     ```java
     public interface TranscriptionCallback {
         void onTranscriptionStarted();
         void onPartialResult(String partialText);
         void onTranscriptionCompleted(TranscriptionResult result);
         void onTranscriptionError(String error);
     }
     ```
  
  2. **创建WhisperEngine类** (`engine/WhisperEngine.java`)
     ```java
     public class WhisperEngine {
         private WhisperJNI whisperJNI;
         private boolean isModelLoaded = false;
         private ExecutorService executorService;
         
         public WhisperEngine() {
             whisperJNI = new WhisperJNI();
             executorService = Executors.newSingleThreadExecutor();
         }
         
         public boolean initializeModel(String modelPath) {
             try {
                 isModelLoaded = whisperJNI.initializeModel(modelPath);
                 return isModelLoaded;
             } catch (Exception e) {
                 isModelLoaded = false;
                 return false;
             }
         }
         
         public void transcribe(AudioData audioData, TranscriptionCallback callback) {
             if (!isModelLoaded) {
                 callback.onTranscriptionError("模型未加载");
                 return;
             }
             
             executorService.execute(() -> {
                 try {
                     callback.onTranscriptionStarted();
                     
                     // 转换音频数据为float数组
                     float[] floatData = convertToFloatArray(audioData);
                     
                     // 调用native转录方法
                     String result = whisperJNI.transcribeAudio(floatData, floatData.length);
                     
                     // 创建转录结果
                     TranscriptionResult transcriptionResult = new TranscriptionResult(
                         result, 0.9f, System.currentTimeMillis(), true
                     );
                     
                     callback.onTranscriptionCompleted(transcriptionResult);
                     
                 } catch (Exception e) {
                     callback.onTranscriptionError("转录失败: " + e.getMessage());
                 }
             });
         }
         
         private float[] convertToFloatArray(AudioData audioData) {
             byte[] rawData = audioData.getRawData();
             float[] floatData = new float[rawData.length / 2];
             
             for (int i = 0; i < floatData.length; i++) {
                 short sample = (short) ((rawData[i * 2 + 1] << 8) | (rawData[i * 2] & 0xFF));
                 floatData[i] = sample / 32768.0f;
             }
             
             return floatData;
         }
         
         public boolean isModelLoaded() {
             return isModelLoaded && whisperJNI.isModelLoaded();
         }
         
         public void release() {
             if (isModelLoaded) {
                 whisperJNI.releaseModel();
                 isModelLoaded = false;
             }
             
             if (executorService != null && !executorService.isShutdown()) {
                 executorService.shutdown();
             }
         }
     }
     ```
  
  **验收标准**: WhisperEngine能够正确加载模型，执行转录任务，处理错误情况
  
  _需求映射: 1.3, 1.4, 2.3, 3.3_-
 [ ] **9. 实现离线模式管理**
  
  **目标**: 管理离线模式，处理模型文件下载和本地存储
  
  **实施步骤**:
  
  1. **创建OfflineModeManager类** (`engine/OfflineModeManager.java`)
     ```java
     public class OfflineModeManager {
         private static final String MODEL_FILENAME = "whisper_cantonese.bin";
         private Context context;
         private SharedPreferences preferences;
         
         public OfflineModeManager(Context context) {
             this.context = context.getApplicationContext();
             this.preferences = context.getSharedPreferences("offline_mode", Context.MODE_PRIVATE);
         }
         
         public boolean isOfflineModeAvailable() {
             String modelPath = getModelPath();
             File modelFile = new File(modelPath);
             return modelFile.exists() && modelFile.length() > 0;
         }
         
         public String getModelPath() {
             File modelsDir = new File(context.getFilesDir(), "models");
             if (!modelsDir.exists()) {
                 modelsDir.mkdirs();
             }
             return new File(modelsDir, MODEL_FILENAME).getAbsolutePath();
         }
         
         public void enableOfflineMode() {
             preferences.edit().putBoolean("offline_enabled", true).apply();
         }
         
         public void disableOfflineMode() {
             preferences.edit().putBoolean("offline_enabled", false).apply();
         }
         
         public boolean isOfflineModeEnabled() {
             return preferences.getBoolean("offline_enabled", false);
         }
         
         // 简化的模型"下载"方法（实际项目中需要实现真实的下载逻辑）
         public void downloadModel(ModelDownloadCallback callback) {
             // 模拟下载过程
             new Thread(() -> {
                 try {
                     callback.onDownloadStarted();
                     
                     // 模拟下载进度
                     for (int i = 0; i <= 100; i += 10) {
                         Thread.sleep(100);
                         callback.onDownloadProgress(i / 100.0f);
                     }
                     
                     // 创建模拟模型文件
                     String modelPath = getModelPath();
                     File modelFile = new File(modelPath);
                     modelFile.createNewFile();
                     
                     callback.onDownloadCompleted(modelPath);
                     
                 } catch (Exception e) {
                     callback.onDownloadError(e);
                 }
             }).start();
         }
         
         public interface ModelDownloadCallback {
             void onDownloadStarted();
             void onDownloadProgress(float progress);
             void onDownloadCompleted(String modelPath);
             void onDownloadError(Exception error);
         }
     }
     ```
  
  **验收标准**: 能够检测离线模式可用性，正确管理模型文件
  
  _需求映射: 3.1, 3.2, 3.4_### 阶
段五：转录服务实现

- [ ] **10. 实现TranscriptionService后台服务**
  
  **目标**: 创建Android后台服务，处理长时间的转录任务
  
  **实施步骤**:
  
  1. **创建TranscriptionService类** (`service/TranscriptionService.java`)
     ```java
     public class TranscriptionService extends Service {
         private static final int NOTIFICATION_ID = 1001;
         private static final String CHANNEL_ID = "transcription_channel";
         
         private WhisperEngine whisperEngine;
         private AudioRecorderManager audioRecorder;
         private TranscriptionRepository repository;
         private boolean isRealTimeMode = false;
         
         @Override
         public void onCreate() {
             super.onCreate();
             
             // 初始化组件
             whisperEngine = new WhisperEngine();
             audioRecorder = new AudioRecorderManager(this);
             repository = new TranscriptionRepository(getApplication());
             
             // 创建通知渠道
             createNotificationChannel();
         }
         
         @Override
         public int onStartCommand(Intent intent, int flags, int startId) {
             String action = intent.getAction();
             
             if ("START_REAL_TIME".equals(action)) {
                 startRealTimeTranscription();
             } else if ("STOP_REAL_TIME".equals(action)) {
                 stopRealTimeTranscription();
             }
             
             return START_STICKY;
         }
         
         public void startRealTimeTranscription() {
             if (isRealTimeMode) return;
             
             // 启动前台服务
             startForeground(NOTIFICATION_ID, createNotification("正在进行实时转录"));
             
             // 设置音频监听器
             audioRecorder.setAudioStreamListener(new AudioStreamListener() {
                 @Override
                 public void onRecordingStarted() {
                     // 录音开始
                 }
                 
                 @Override
                 public void onRecordingStopped() {
                     // 录音停止
                 }
                 
                 @Override
                 public void onAudioDataAvailable(byte[] audioData) {
                     // 处理音频数据
                     processAudioData(audioData);
                 }
                 
                 @Override
                 public void onRecordingError(String error) {
                     // 处理录音错误
                 }
             });
             
             // 开始录音
             audioRecorder.startRecording();
             isRealTimeMode = true;
         }
         
         public void stopRealTimeTranscription() {
             if (!isRealTimeMode) return;
             
             audioRecorder.stopRecording();
             isRealTimeMode = false;
             
             stopForeground(true);
             stopSelf();
         }
         
         private void processAudioData(byte[] audioData) {
             // 转换音频格式
             AudioData processedAudio = AudioProcessor.convertToWhisperFormat(audioData, 16000);
             
             // 进行转录
             whisperEngine.transcribe(processedAudio, new TranscriptionCallback() {
                 @Override
                 public void onTranscriptionStarted() {
                     // 转录开始
                 }
                 
                 @Override
                 public void onPartialResult(String partialText) {
                     // 发送部分结果广播
                     sendTranscriptionBroadcast(partialText, false);
                 }
                 
                 @Override
                 public void onTranscriptionCompleted(TranscriptionResult result) {
                     // 发送完整结果广播
                     sendTranscriptionBroadcast(result.getText(), true);
                     
                     // 保存到数据库
                     saveTranscriptionResult(result);
                 }
                 
                 @Override
                 public void onTranscriptionError(String error) {
                     // 处理转录错误
                 }
             });
         }
         
         private void sendTranscriptionBroadcast(String text, boolean isComplete) {
             Intent intent = new Intent("TRANSCRIPTION_RESULT");
             intent.putExtra("text", text);
             intent.putExtra("isComplete", isComplete);
             sendBroadcast(intent);
         }
         
         private void saveTranscriptionResult(TranscriptionResult result) {
             TranscriptionRecord record = new TranscriptionRecord(
                 result.getText(),
                 System.currentTimeMillis(),
                 (int) result.getProcessingTime(),
                 result.getConfidence()
             );
             
             repository.insertRecord(record, new RepositoryCallback<Long>() {
                 @Override
                 public void onSuccess(Long result) {
                     // 保存成功
                 }
                 
                 @Override
                 public void onError(Exception error) {
                     // 保存失败
                 }
             });
         }
         
         private void createNotificationChannel() {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 NotificationChannel channel = new NotificationChannel(
                     CHANNEL_ID, "转录服务", NotificationManager.IMPORTANCE_LOW);
                 NotificationManager manager = getSystemService(NotificationManager.class);
                 manager.createNotificationChannel(channel);
             }
         }
         
         private Notification createNotification(String content) {
             return new NotificationCompat.Builder(this, CHANNEL_ID)
                 .setContentTitle("粤语语音识别")
                 .setContentText(content)
                 .setSmallIcon(R.drawable.ic_mic)
                 .build();
         }
         
         @Override
         public IBinder onBind(Intent intent) {
             return null;
         }
         
         @Override
         public void onDestroy() {
             super.onDestroy();
             if (whisperEngine != null) {
                 whisperEngine.release();
             }
         }
     }
     ```
  
  **验收标准**: 后台服务能够正常运行，支持前台服务通知，正确处理转录任务
  
  _需求映射: 1.3, 2.1, 2.2, 2.3_##
# 阶段六：用户界面实现

- [ ] **11. 创建主界面Activity**
  
  **目标**: 实现应用主界面，提供录音控制和转录结果显示功能
  
  **实施步骤**:
  
  1. **创建MainActivity类** (`ui/main/MainActivity.java`)
     ```java
     public class MainActivity extends AppCompatActivity {
         private TextView transcriptionText;
         private FloatingActionButton recordButton;
         private TextView statusText;
         
         private TranscriptionService transcriptionService;
         private boolean isRecording = false;
         private BroadcastReceiver transcriptionReceiver;
         
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_main);
             
             initViews();
             setupRecordButton();
             registerTranscriptionReceiver();
             checkPermissions();
         }
         
         private void initViews() {
             transcriptionText = findViewById(R.id.transcriptionText);
             recordButton = findViewById(R.id.recordButton);
             statusText = findViewById(R.id.statusText);
         }
         
         private void setupRecordButton() {
             recordButton.setOnClickListener(v -> {
                 if (isRecording) {
                     stopRecording();
                 } else {
                     startRecording();
                 }
             });
         }
         
         private void startRecording() {
             Intent serviceIntent = new Intent(this, TranscriptionService.class);
             serviceIntent.setAction("START_REAL_TIME");
             startService(serviceIntent);
             
             isRecording = true;
             recordButton.setImageResource(R.drawable.ic_stop);
             statusText.setText("正在录音...");
         }
         
         private void stopRecording() {
             Intent serviceIntent = new Intent(this, TranscriptionService.class);
             serviceIntent.setAction("STOP_REAL_TIME");
             startService(serviceIntent);
             
             isRecording = false;
             recordButton.setImageResource(R.drawable.ic_mic);
             statusText.setText("录音已停止");
         }
         
         private void registerTranscriptionReceiver() {
             transcriptionReceiver = new BroadcastReceiver() {
                 @Override
                 public void onReceive(Context context, Intent intent) {
                     String text = intent.getStringExtra("text");
                     boolean isComplete = intent.getBooleanExtra("isComplete", false);
                     
                     if (isComplete) {
                         transcriptionText.setText(text);
                     } else {
                         transcriptionText.setText(text + "...");
                     }
                 }
             };
             
             IntentFilter filter = new IntentFilter("TRANSCRIPTION_RESULT");
             registerReceiver(transcriptionReceiver, filter);
         }
         
         private void checkPermissions() {
             if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                     != PackageManager.PERMISSION_GRANTED) {
                 ActivityCompat.requestPermissions(this,
                         new String[]{Manifest.permission.RECORD_AUDIO}, 1);
             }
         }
         
         @Override
         protected void onDestroy() {
             super.onDestroy();
             if (transcriptionReceiver != null) {
                 unregisterReceiver(transcriptionReceiver);
             }
         }
     }
     ```
  
  2. **创建主界面布局** (`res/layout/activity_main.xml`)
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical"
         android:padding="16dp">
         
         <TextView
             android:id="@+id/statusText"
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:text="点击录音按钮开始"
             android:textAlignment="center"
             android:textSize="16sp"
             android:layout_marginBottom="16dp" />
         
         <ScrollView
             android:layout_width="match_parent"
             android:layout_height="0dp"
             android:layout_weight="1"
             android:background="@drawable/transcription_background"
             android:padding="16dp">
             
             <TextView
                 android:id="@+id/transcriptionText"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="转录结果将在这里显示..."
                 android:textSize="18sp"
                 android:lineSpacingExtra="4dp" />
         </ScrollView>
         
         <com.google.android.material.floatingactionbutton.FloatingActionButton
             android:id="@+id/recordButton"
             android:layout_width="wrap_content"
             android:layout_height="wrap_content"
             android:layout_gravity="center"
             android:layout_marginTop="16dp"
             android:src="@drawable/ic_mic" />
         
     </LinearLayout>
     ```
  
  **验收标准**: 主界面能够正常显示，录音功能可用，转录结果正确显示
  
  _需求映射: 1.1, 1.2, 1.5, 5.5_- [ ] **
12. 实现历史记录界面**
  
  **目标**: 创建转录历史记录管理界面
  
  **实施步骤**:
  
  1. **创建HistoryFragment类** (`ui/history/HistoryFragment.java`)
     ```java
     public class HistoryFragment extends Fragment {
         private RecyclerView historyRecyclerView;
         private EditText searchEditText;
         private HistoryAdapter historyAdapter;
         private TranscriptionRepository repository;
         
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {
             View view = inflater.inflate(R.layout.fragment_history, container, false);
             
             initViews(view);
             setupRecyclerView();
             setupSearch();
             loadHistoryData();
             
             return view;
         }
         
         private void initViews(View view) {
             historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
             searchEditText = view.findViewById(R.id.searchEditText);
             repository = new TranscriptionRepository(getActivity().getApplication());
         }
         
         private void setupRecyclerView() {
             historyAdapter = new HistoryAdapter(new ArrayList<>(), this::onItemClick);
             historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
             historyRecyclerView.setAdapter(historyAdapter);
         }
         
         private void setupSearch() {
             searchEditText.addTextChangedListener(new TextWatcher() {
                 @Override
                 public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                 
                 @Override
                 public void onTextChanged(CharSequence s, int start, int before, int count) {
                     performSearch(s.toString());
                 }
                 
                 @Override
                 public void afterTextChanged(Editable s) {}
             });
         }
         
         private void loadHistoryData() {
             repository.getAllRecords().observe(this, records -> {
                 historyAdapter.updateData(records);
             });
         }
         
         private void performSearch(String query) {
             if (query.isEmpty()) {
                 loadHistoryData();
             } else {
                 repository.searchRecords(query).observe(this, records -> {
                     historyAdapter.updateData(records);
                 });
             }
         }
         
         private void onItemClick(TranscriptionRecord record) {
             // 处理项目点击事件，可以编辑或查看详情
             showEditDialog(record);
         }
         
         private void showEditDialog(TranscriptionRecord record) {
             AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
             builder.setTitle("编辑转录记录");
             
             EditText editText = new EditText(getContext());
             editText.setText(record.getTranscriptionText());
             builder.setView(editText);
             
             builder.setPositiveButton("保存", (dialog, which) -> {
                 record.setTranscriptionText(editText.getText().toString());
                 repository.updateRecord(record, new RepositoryCallback<Void>() {
                     @Override
                     public void onSuccess(Void result) {
                         // 更新成功
                     }
                     
                     @Override
                     public void onError(Exception error) {
                         // 更新失败
                     }
                 });
             });
             
             builder.setNegativeButton("取消", null);
             builder.show();
         }
     }
     ```
  
  2. **创建HistoryAdapter类** (`ui/history/HistoryAdapter.java`)
     ```java
     public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
         private List<TranscriptionRecord> records;
         private OnItemClickListener listener;
         
         public interface OnItemClickListener {
             void onItemClick(TranscriptionRecord record);
         }
         
         public HistoryAdapter(List<TranscriptionRecord> records, OnItemClickListener listener) {
             this.records = records;
             this.listener = listener;
         }
         
         @Override
         public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
             View view = LayoutInflater.from(parent.getContext())
                     .inflate(R.layout.item_history, parent, false);
             return new ViewHolder(view);
         }
         
         @Override
         public void onBindViewHolder(ViewHolder holder, int position) {
             TranscriptionRecord record = records.get(position);
             holder.bind(record, listener);
         }
         
         @Override
         public int getItemCount() {
             return records.size();
         }
         
         public void updateData(List<TranscriptionRecord> newRecords) {
             this.records = newRecords;
             notifyDataSetChanged();
         }
         
         static class ViewHolder extends RecyclerView.ViewHolder {
             TextView textView;
             TextView timeView;
             TextView confidenceView;
             
             ViewHolder(View itemView) {
                 super(itemView);
                 textView = itemView.findViewById(R.id.transcriptionText);
                 timeView = itemView.findViewById(R.id.timeText);
                 confidenceView = itemView.findViewById(R.id.confidenceText);
             }
             
             void bind(TranscriptionRecord record, OnItemClickListener listener) {
                 textView.setText(record.getTranscriptionText());
                 timeView.setText(formatTime(record.getCreatedAt()));
                 confidenceView.setText(String.format("置信度: %.1f%%", record.getConfidenceScore() * 100));
                 
                 itemView.setOnClickListener(v -> listener.onItemClick(record));
             }
             
             private String formatTime(long timestamp) {
                 SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                 return sdf.format(new Date(timestamp));
             }
         }
     }
     ```
  
  3. **创建历史记录布局** (`res/layout/fragment_history.xml`)
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical"
         android:padding="16dp">
         
         <com.google.android.material.textfield.TextInputLayout
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:hint="搜索转录记录">
             
             <EditText
                 android:id="@+id/searchEditText"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:inputType="text" />
         </com.google.android.material.textfield.TextInputLayout>
         
         <androidx.recyclerview.widget.RecyclerView
             android:id="@+id/historyRecyclerView"
             android:layout_width="match_parent"
             android:layout_height="match_parent"
             android:layout_marginTop="16dp" />
         
     </LinearLayout>
     ```
  
  **验收标准**: 能够显示历史记录，支持搜索和编辑功能
  
  _需求映射: 4.2, 4.3, 4.4, 4.5_- [ ]
 **13. 实现设置界面**
  
  **目标**: 创建应用设置和配置界面
  
  **实施步骤**:
  
  1. **创建SettingsFragment类** (`ui/settings/SettingsFragment.java`)
     ```java
     public class SettingsFragment extends Fragment {
         private SwitchMaterial offlineModeSwitch;
         private Spinner audioQualitySpinner;
         private Button modelManagementButton;
         private OfflineModeManager offlineModeManager;
         private SharedPreferences preferences;
         
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {
             View view = inflater.inflate(R.layout.fragment_settings, container, false);
             
             initViews(view);
             setupOfflineModeSwitch();
             setupAudioQualitySpinner();
             setupModelManagementButton();
             
             return view;
         }
         
         private void initViews(View view) {
             offlineModeSwitch = view.findViewById(R.id.offlineModeSwitch);
             audioQualitySpinner = view.findViewById(R.id.audioQualitySpinner);
             modelManagementButton = view.findViewById(R.id.modelManagementButton);
             
             offlineModeManager = new OfflineModeManager(getContext());
             preferences = getContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
         }
         
         private void setupOfflineModeSwitch() {
             offlineModeSwitch.setChecked(offlineModeManager.isOfflineModeEnabled());
             offlineModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                 if (isChecked) {
                     if (offlineModeManager.isOfflineModeAvailable()) {
                         offlineModeManager.enableOfflineMode();
                     } else {
                         // 提示用户下载模型
                         showDownloadModelDialog();
                         offlineModeSwitch.setChecked(false);
                     }
                 } else {
                     offlineModeManager.disableOfflineMode();
                 }
             });
         }
         
         private void setupAudioQualitySpinner() {
             String[] qualities = {"标准质量 (16kHz)", "高质量 (44.1kHz)"};
             ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                     android.R.layout.simple_spinner_item, qualities);
             adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             audioQualitySpinner.setAdapter(adapter);
             
             // 加载保存的设置
             int savedQuality = preferences.getInt("audio_quality", 0);
             audioQualitySpinner.setSelection(savedQuality);
             
             audioQualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                 @Override
                 public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                     preferences.edit().putInt("audio_quality", position).apply();
                 }
                 
                 @Override
                 public void onNothingSelected(AdapterView<?> parent) {}
             });
         }
         
         private void setupModelManagementButton() {
             modelManagementButton.setOnClickListener(v -> showModelManagementDialog());
         }
         
         private void showDownloadModelDialog() {
             AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
             builder.setTitle("下载离线模型");
             builder.setMessage("离线模式需要下载语音识别模型，是否现在下载？");
             
             builder.setPositiveButton("下载", (dialog, which) -> {
                 downloadModel();
             });
             
             builder.setNegativeButton("取消", null);
             builder.show();
         }
         
         private void downloadModel() {
             ProgressDialog progressDialog = new ProgressDialog(getContext());
             progressDialog.setTitle("下载模型");
             progressDialog.setMessage("正在下载...");
             progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
             progressDialog.show();
             
             offlineModeManager.downloadModel(new OfflineModeManager.ModelDownloadCallback() {
                 @Override
                 public void onDownloadStarted() {
                     // 下载开始
                 }
                 
                 @Override
                 public void onDownloadProgress(float progress) {
                     progressDialog.setProgress((int) (progress * 100));
                 }
                 
                 @Override
                 public void onDownloadCompleted(String modelPath) {
                     progressDialog.dismiss();
                     offlineModeSwitch.setChecked(true);
                     offlineModeManager.enableOfflineMode();
                     Toast.makeText(getContext(), "模型下载完成", Toast.LENGTH_SHORT).show();
                 }
                 
                 @Override
                 public void onDownloadError(Exception error) {
                     progressDialog.dismiss();
                     Toast.makeText(getContext(), "下载失败: " + error.getMessage(), 
                             Toast.LENGTH_LONG).show();
                 }
             });
         }
         
         private void showModelManagementDialog() {
             AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
             builder.setTitle("模型管理");
             
             String[] options = {"重新下载模型", "删除模型"};
             builder.setItems(options, (dialog, which) -> {
                 switch (which) {
                     case 0:
                         downloadModel();
                         break;
                     case 1:
                         deleteModel();
                         break;
                 }
             });
             
             builder.show();
         }
         
         private void deleteModel() {
             String modelPath = offlineModeManager.getModelPath();
             File modelFile = new File(modelPath);
             if (modelFile.exists()) {
                 modelFile.delete();
                 offlineModeSwitch.setChecked(false);
                 offlineModeManager.disableOfflineMode();
                 Toast.makeText(getContext(), "模型已删除", Toast.LENGTH_SHORT).show();
             }
         }
     }
     ```
  
  2. **创建设置布局** (`res/layout/fragment_settings.xml`)
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:padding="16dp">
         
         <LinearLayout
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:orientation="vertical">
             
             <TextView
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="离线模式"
                 android:textSize="18sp"
                 android:textStyle="bold"
                 android:layout_marginBottom="8dp" />
             
             <com.google.android.material.switchmaterial.SwitchMaterial
                 android:id="@+id/offlineModeSwitch"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="启用离线语音识别"
                 android:layout_marginBottom="24dp" />
             
             <TextView
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="音频质量"
                 android:textSize="18sp"
                 android:textStyle="bold"
                 android:layout_marginBottom="8dp" />
             
             <Spinner
                 android:id="@+id/audioQualitySpinner"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:layout_marginBottom="24dp" />
             
             <TextView
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="模型管理"
                 android:textSize="18sp"
                 android:textStyle="bold"
                 android:layout_marginBottom="8dp" />
             
             <Button
                 android:id="@+id/modelManagementButton"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:text="管理语音模型" />
             
         </LinearLayout>
     </ScrollView>
     ```
  
  **验收标准**: 设置界面功能完整，设置能够正确保存和应用
  
  _需求映射: 3.1, 3.2, 5.4_##
# 阶段七：系统集成和优化

- [ ] **14. 实现权限管理系统**
  
  **目标**: 完善应用权限申请和管理
  
  **实施步骤**:
  
  1. **创建PermissionManager类** (`utils/PermissionManager.java`)
     ```java
     public class PermissionManager {
         public static final String[] REQUIRED_PERMISSIONS = {
             Manifest.permission.RECORD_AUDIO,
             Manifest.permission.WRITE_EXTERNAL_STORAGE,
             Manifest.permission.INTERNET
         };
         
         public static final int PERMISSION_REQUEST_CODE = 1001;
         
         public interface PermissionCallback {
             void onPermissionsGranted();
             void onPermissionsDenied(String[] deniedPermissions);
         }
         
         public static void requestPermissions(Activity activity, PermissionCallback callback) {
             List<String> deniedPermissions = new ArrayList<>();
             
             for (String permission : REQUIRED_PERMISSIONS) {
                 if (ContextCompat.checkSelfPermission(activity, permission)
                         != PackageManager.PERMISSION_GRANTED) {
                     deniedPermissions.add(permission);
                 }
             }
             
             if (deniedPermissions.isEmpty()) {
                 callback.onPermissionsGranted();
             } else {
                 ActivityCompat.requestPermissions(activity,
                         deniedPermissions.toArray(new String[0]),
                         PERMISSION_REQUEST_CODE);
             }
         }
         
         public static boolean hasAllPermissions(Context context) {
             for (String permission : REQUIRED_PERMISSIONS) {
                 if (ContextCompat.checkSelfPermission(context, permission)
                         != PackageManager.PERMISSION_GRANTED) {
                     return false;
                 }
             }
             return true;
         }
         
         public static void showPermissionExplanationDialog(Activity activity) {
             AlertDialog.Builder builder = new AlertDialog.Builder(activity);
             builder.setTitle("需要权限");
             builder.setMessage("应用需要以下权限才能正常工作：\n\n" +
                     "• 录音权限：用于录制语音\n" +
                     "• 存储权限：用于保存转录记录\n" +
                     "• 网络权限：用于在线模式");
             
             builder.setPositiveButton("授权", (dialog, which) -> {
                 requestPermissions(activity, new PermissionCallback() {
                     @Override
                     public void onPermissionsGranted() {
                         // 权限已授予
                     }
                     
                     @Override
                     public void onPermissionsDenied(String[] deniedPermissions) {
                         // 权限被拒绝
                     }
                 });
             });
             
             builder.setNegativeButton("取消", null);
             builder.show();
         }
     }
     ```
  
  2. **更新AndroidManifest.xml**
     ```xml
     <manifest xmlns:android="http://schemas.android.com/apk/res/android"
         package="com.example.cantonesevoicerecognition">
         
         <!-- 权限声明 -->
         <uses-permission android:name="android.permission.RECORD_AUDIO" />
         <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
         <uses-permission android:name="android.permission.INTERNET" />
         <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
         
         <application
             android:allowBackup="true"
             android:icon="@mipmap/ic_launcher"
             android:label="@string/app_name"
             android:theme="@style/AppTheme">
             
             <activity
                 android:name=".ui.main.MainActivity"
                 android:exported="true">
                 <intent-filter>
                     <action android:name="android.intent.action.MAIN" />
                     <category android:name="android.intent.category.LAUNCHER" />
                 </intent-filter>
             </activity>
             
             <service
                 android:name=".service.TranscriptionService"
                 android:enabled="true"
                 android:exported="false" />
             
         </application>
     </manifest>
     ```
  
  **验收标准**: 权限申请流程完整，用户体验友好
  
  _需求映射: 1.1, 1.2, 2.5_

- [ ] **15. 实现错误处理和日志系统**
  
  **目标**: 建立完整的错误处理和日志记录机制
  
  **实施步骤**:
  
  1. **创建TranscriptionError枚举** (`utils/TranscriptionError.java`)
     ```java
     public enum TranscriptionError {
         MODEL_NOT_LOADED("语音模型未加载"),
         AUDIO_FORMAT_UNSUPPORTED("不支持的音频格式"),
         INSUFFICIENT_STORAGE("存储空间不足"),
         PERMISSION_DENIED("权限被拒绝"),
         NETWORK_ERROR("网络错误"),
         MODEL_CORRUPTED("模型文件损坏");
         
         private final String message;
         
         TranscriptionError(String message) {
             this.message = message;
         }
         
         public String getMessage() {
             return message;
         }
     }
     ```
  
  2. **创建ErrorHandler类** (`utils/ErrorHandler.java`)
     ```java
     public class ErrorHandler {
         private static final String TAG = "ErrorHandler";
         
         public static void handleError(Context context, TranscriptionError error, Exception exception) {
             // 记录错误日志
             Log.e(TAG, "Error: " + error.getMessage(), exception);
             
             // 显示用户友好的错误提示
             showUserFriendlyError(context, error);
             
             // 尝试错误恢复
             attemptErrorRecovery(context, error);
         }
         
         private static void showUserFriendlyError(Context context, TranscriptionError error) {
             String message = getLocalizedErrorMessage(error);
             Toast.makeText(context, message, Toast.LENGTH_LONG).show();
         }
         
         private static String getLocalizedErrorMessage(TranscriptionError error) {
             switch (error) {
                 case MODEL_NOT_LOADED:
                     return "语音识别模型未加载，请检查设置";
                 case PERMISSION_DENIED:
                     return "缺少必要权限，请在设置中授权";
                 case INSUFFICIENT_STORAGE:
                     return "存储空间不足，请清理设备存储";
                 case NETWORK_ERROR:
                     return "网络连接失败，已切换到离线模式";
                 default:
                     return error.getMessage();
             }
         }
         
         private static void attemptErrorRecovery(Context context, TranscriptionError error) {
             switch (error) {
                 case MODEL_NOT_LOADED:
                     // 尝试重新加载模型
                     break;
                 case PERMISSION_DENIED:
                     // 引导用户到权限设置
                     break;
                 case NETWORK_ERROR:
                     // 切换到离线模式
                     break;
             }
         }
     }
     ```
  
  **验收标准**: 错误处理完整，日志记录详细，用户体验良好
  
  _需求映射: 1.4, 2.4, 3.4_-
 [ ] **16. 性能优化和测试**
  
  **目标**: 优化应用性能，确保满足性能要求
  
  **实施步骤**:
  
  1. **创建PerformanceOptimizer类** (`utils/PerformanceOptimizer.java`)
     ```java
     public class PerformanceOptimizer {
         
         public enum OptimizationLevel {
             PERFORMANCE,  // 性能优先
             BALANCED,     // 平衡模式
             BATTERY       // 电池优先
         }
         
         private Context context;
         private OptimizationLevel currentLevel;
         
         public PerformanceOptimizer(Context context) {
             this.context = context.getApplicationContext();
             this.currentLevel = OptimizationLevel.BALANCED;
         }
         
         public void setOptimizationLevel(OptimizationLevel level) {
             this.currentLevel = level;
             
             switch (level) {
                 case PERFORMANCE:
                     applyPerformanceOptimizations();
                     break;
                 case BATTERY:
                     applyBatteryOptimizations();
                     break;
                 default:
                     applyBalancedOptimizations();
             }
         }
         
         private void applyPerformanceOptimizations() {
             // 性能优先设置
             // - 使用更高的音频采样率
             // - 增加缓冲区大小
             // - 启用多线程处理
         }
         
         private void applyBatteryOptimizations() {
             // 电池优先设置
             // - 降低音频采样率
             // - 减少处理频率
             // - 启用智能VAD检测
         }
         
         private void applyBalancedOptimizations() {
             // 平衡模式设置
             // - 标准音频采样率
             // - 适中的处理频率
             // - 基础VAD检测
         }
         
         public void optimizeMemoryUsage() {
             // 内存优化
             System.gc(); // 建议垃圾回收
             
             // 清理不必要的缓存
             clearAudioBuffers();
             
             // 释放未使用的资源
             releaseUnusedResources();
         }
         
         private void clearAudioBuffers() {
             // 清理音频缓冲区
         }
         
         private void releaseUnusedResources() {
             // 释放未使用的资源
         }
         
         public long getMemoryUsage() {
             Runtime runtime = Runtime.getRuntime();
             return runtime.totalMemory() - runtime.freeMemory();
         }
         
         public boolean isLowMemory() {
             ActivityManager activityManager = (ActivityManager) 
                 context.getSystemService(Context.ACTIVITY_SERVICE);
             ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
             activityManager.getMemoryInfo(memoryInfo);
             
             return memoryInfo.lowMemory;
         }
     }
     ```
  
  2. **创建性能监控工具** (`utils/PerformanceMonitor.java`)
     ```java
     public class PerformanceMonitor {
         private static final String TAG = "PerformanceMonitor";
         
         private long startTime;
         private long memoryStart;
         
         public void startMonitoring() {
             startTime = System.currentTimeMillis();
             memoryStart = getMemoryUsage();
             Log.d(TAG, "Performance monitoring started");
         }
         
         public void logPerformanceMetrics(String operation) {
             long endTime = System.currentTimeMillis();
             long memoryEnd = getMemoryUsage();
             
             long duration = endTime - startTime;
             long memoryUsed = memoryEnd - memoryStart;
             
             Log.d(TAG, String.format("Operation: %s, Duration: %dms, Memory: %dKB",
                     operation, duration, memoryUsed / 1024));
         }
         
         private long getMemoryUsage() {
             Runtime runtime = Runtime.getRuntime();
             return runtime.totalMemory() - runtime.freeMemory();
         }
         
         public static void logAppStartupTime() {
             // 记录应用启动时间
             Log.d(TAG, "App startup completed");
         }
         
         public static void logTranscriptionTime(long startTime, long endTime) {
             long duration = endTime - startTime;
             Log.d(TAG, "Transcription completed in " + duration + "ms");
             
             // 验证是否满足性能要求（5秒内完成30秒音频转录）
             if (duration > 5000) {
                 Log.w(TAG, "Transcription time exceeds target (5s)");
             }
         }
     }
     ```
  
  **验收标准**: 应用启动时间<3秒，转录响应时间<5秒，内存使用合理
  
  _需求映射: 5.1, 5.2, 5.5_

### 阶段八：测试和质量保证

- [ ]* **17. 单元测试实现**
  
  **目标**: 为核心业务逻辑创建单元测试
  
  **实施步骤**:
  
  1. **配置测试环境** (`app/build.gradle`)
     ```gradle
     dependencies {
         testImplementation 'junit:junit:4.13.2'
         testImplementation 'org.mockito:mockito-core:4.6.1'
         testImplementation 'androidx.arch.core:core-testing:2.2.0'
         
         androidTestImplementation 'androidx.test.ext:junit:1.1.5'
         androidTestImplementation 'androidx.test:runner:1.5.2'
         androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
     }
     ```
  
  2. **创建数据模型测试** (`test/java/.../TranscriptionRecordTest.java`)
     ```java
     @RunWith(JUnit4.class)
     public class TranscriptionRecordTest {
         
         @Test
         public void testTranscriptionRecordCreation() {
             String text = "测试转录文本";
             long timestamp = System.currentTimeMillis();
             
             TranscriptionRecord record = new TranscriptionRecord(text, timestamp, 1000, 0.9f);
             
             assertNotNull(record);
             assertEquals(text, record.getTranscriptionText());
             assertEquals(timestamp, record.getCreatedAt());
         }
         
         @Test
         public void testAudioDataProcessing() {
             byte[] testData = new byte[1024];
             AudioData audioData = new AudioData(testData, 16000, 1);
             
             assertNotNull(audioData);
             assertEquals(16000, audioData.getSampleRate());
             assertTrue(audioData.getDurationMs() > 0);
         }
     }
     ```
  
  3. **创建音频处理测试** (`test/java/.../AudioProcessorTest.java`)
     ```java
     public class AudioProcessorTest {
         
         @Test
         public void testAudioFormatConversion() {
             byte[] testAudioData = generateTestAudioData();
             AudioData result = AudioProcessor.convertToWhisperFormat(testAudioData, 44100);
             
             assertNotNull(result);
             assertEquals(16000, result.getSampleRate());
             assertEquals(1, result.getChannels());
         }
         
         @Test
         public void testVoiceActivityDetection() {
             byte[] silenceData = new byte[1024]; // 静音数据
             byte[] voiceData = generateVoiceData(); // 语音数据
             
             assertFalse(AudioProcessor.detectVoiceActivity(silenceData, 0.1f));
             assertTrue(AudioProcessor.detectVoiceActivity(voiceData, 0.1f));
         }
         
         private byte[] generateTestAudioData() {
             // 生成测试音频数据
             return new byte[1024];
         }
         
         private byte[] generateVoiceData() {
             // 生成模拟语音数据
             byte[] data = new byte[1024];
             for (int i = 0; i < data.length; i++) {
                 data[i] = (byte) (Math.sin(i * 0.1) * 127);
             }
             return data;
         }
     }
     ```
  
  **验收标准**: 单元测试覆盖率>80%，所有测试用例通过
  
  _需求映射: 1.4, 2.3, 3.4, 4.5_

- [ ]* **18. 集成测试和UI测试**
  
  **目标**: 测试模块间协作和用户界面交互
  
  **实施步骤**:
  
  1. **创建端到端测试** (`androidTest/java/.../MainActivityTest.java`)
     ```java
     @RunWith(AndroidJUnit4.class)
     public class MainActivityTest {
         
         @Rule
         public ActivityTestRule<MainActivity> activityRule = 
             new ActivityTestRule<>(MainActivity.class);
         
         @Test
         public void testRecordButtonInteraction() {
             // 测试录音按钮点击
             onView(withId(R.id.recordButton))
                 .check(matches(isDisplayed()))
                 .perform(click());
             
             // 验证状态变化
             onView(withId(R.id.statusText))
                 .check(matches(withText(containsString("录音"))));
         }
         
         @Test
         public void testTranscriptionResultDisplay() {
             // 测试转录结果显示
             onView(withId(R.id.transcriptionText))
                 .check(matches(isDisplayed()));
         }
     }
     ```
  
  **验收标准**: 所有集成测试通过，UI交互正常
  
  _需求映射: 1.1, 1.2, 4.2-4.5, 5.5_

## 📊 项目完成检查清单

### 功能完整性检查
- [ ] 录音功能正常工作
- [ ] 转录功能准确可靠
- [ ] 实时转录流畅运行
- [ ] 历史记录管理完整
- [ ] 离线模式可用
- [ ] 设置功能有效

### 性能指标验证
- [ ] 应用启动时间 < 3秒
- [ ] 转录响应时间 < 5秒（30秒音频）
- [ ] 支持Android 7.0及以上版本
- [ ] 应用大小 < 500MB
- [ ] 界面响应流畅

### 质量保证
- [ ] 所有权限正确申请
- [ ] 错误处理完善
- [ ] 内存使用合理
- [ ] 无明显bug和崩溃
- [ ] 用户体验友好

## 🎯 开发建议

1. **按阶段开发**: 严格按照阶段顺序进行，确保每个阶段完成后再进入下一阶段
2. **频繁测试**: 每完成一个功能模块就进行测试，及早发现问题
3. **代码规范**: 遵循Java编码规范，保持代码整洁和可维护性
4. **性能监控**: 在开发过程中持续监控性能指标
5. **用户体验**: 重视用户界面设计和交互体验

## 📝 注意事项

- 本实施计划中的Whisper集成部分使用了模拟实现，实际项目中需要集成真实的Whisper C++库
- 模型下载功能需要根据实际的模型服务器地址进行配置
- 某些功能可能需要根据具体的Android版本和设备进行适配调整
- 建议在真实设备上进行测试，确保功能在不同设备上的兼容性
#
# 🔧 详细开发指南

### Android Studio项目配置详解

#### 1. 项目创建详细步骤
```
1. 打开Android Studio
2. 选择 "Create New Project"
3. 选择 "Empty Activity" 模板
4. 配置项目信息：
   - Name: Cantonese Voice Recognition
   - Package name: com.example.cantonesevoicerecognition
   - Save location: [选择合适的目录]
   - Language: Java
   - Minimum SDK: API 24 (Android 7.0)
5. 点击 "Finish" 创建项目
```

#### 2. NDK和CMake配置步骤
```
1. 在Android Studio中：
   - File → Settings → Appearance & Behavior → System Settings → Android SDK
   - 选择 "SDK Tools" 标签
   - 勾选 "NDK (Side by side)" 和 "CMake"
   - 点击 "Apply" 下载安装

2. 在项目的 app/build.gradle 中添加NDK配置：
   android {
       ...
       externalNativeBuild {
           cmake {
               path "src/main/cpp/CMakeLists.txt"
               version "3.22.1"
           }
       }
   }

3. 创建 app/src/main/cpp 目录
4. 在该目录下创建 CMakeLists.txt 文件
```

### 关键文件和目录结构

#### 完整的包结构创建
```
app/src/main/java/com/example/cantonesevoicerecognition/
├── data/
│   ├── model/
│   │   ├── TranscriptionRecord.java
│   │   ├── AudioData.java
│   │   ├── TranscriptionResult.java
│   │   └── WordSegment.java
│   ├── dao/
│   │   └── TranscriptionDao.java
│   ├── repository/
│   │   ├── TranscriptionRepository.java
│   │   └── RepositoryCallback.java
│   └── AppDatabase.java
├── engine/
│   ├── WhisperJNI.java
│   ├── WhisperEngine.java
│   ├── TranscriptionCallback.java
│   └── OfflineModeManager.java
├── audio/
│   ├── AudioRecorderManager.java
│   ├── AudioProcessor.java
│   └── AudioStreamListener.java
├── service/
│   └── TranscriptionService.java
├── ui/
│   ├── main/
│   │   └── MainActivity.java
│   ├── history/
│   │   ├── HistoryFragment.java
│   │   └── HistoryAdapter.java
│   └── settings/
│       └── SettingsFragment.java
└── utils/
    ├── PermissionManager.java
    ├── ErrorHandler.java
    ├── TranscriptionError.java
    ├── PerformanceOptimizer.java
    └── PerformanceMonitor.java
```

#### 资源文件结构
```
app/src/main/res/
├── layout/
│   ├── activity_main.xml
│   ├── fragment_history.xml
│   ├── fragment_settings.xml
│   └── item_history.xml
├── drawable/
│   ├── ic_mic.xml
│   ├── ic_stop.xml
│   └── transcription_background.xml
├── values/
│   ├── strings.xml
│   ├── colors.xml
│   └── styles.xml
└── menu/
    └── bottom_navigation.xml
```

### 依赖库详细说明

#### build.gradle (Module: app) 完整配置
```gradle
android {
    compileSdk 34
    ndkVersion "25.1.8937393"
    
    defaultConfig {
        applicationId "com.example.cantonesevoicerecognition"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            testCoverageEnabled true
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.22.1"
        }
    }
}

dependencies {
    // 核心Android库
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.core:core:1.12.0'
    
    // Material Design
    implementation 'com.google.android.material:material:1.10.0'
    
    // Room数据库
    implementation "androidx.room:room-runtime:2.5.0"
    annotationProcessor "androidx.room:room-compiler:2.5.0"
    
    // ViewModel和LiveData
    implementation "androidx.lifecycle:lifecycle-viewmodel:2.7.0"
    implementation "androidx.lifecycle:lifecycle-livedata:2.7.0"
    
    // Fragment和Navigation
    implementation "androidx.fragment:fragment:1.6.2"
    implementation "androidx.navigation:navigation-fragment:2.7.5"
    implementation "androidx.navigation:navigation-ui:2.7.5"
    
    // RecyclerView
    implementation "androidx.recyclerview:recyclerview:1.3.2"
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.6.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
    testImplementation 'org.robolectric:robolectric:4.9'
    
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'
    androidTestImplementation 'androidx.test.espresso:espresso-contrib:3.5.1'
    androidTestImplementation 'androidx.room:room-testing:2.5.0'
}
```

### 关键配置文件

#### strings.xml
```xml
<resources>
    <string name="app_name">粤语语音识别</string>
    <string name="record_button_start">开始录音</string>
    <string name="record_button_stop">停止录音</string>
    <string name="transcription_placeholder">转录结果将在这里显示...</string>
    <string name="permission_audio_title">需要录音权限</string>
    <string name="permission_audio_message">应用需要录音权限来录制您的语音</string>
    <string name="offline_mode">离线模式</string>
    <string name="audio_quality">音频质量</string>
    <string name="model_management">模型管理</string>
    <string name="search_hint">搜索转录记录</string>
</resources>
```

#### colors.xml
```xml
<resources>
    <color name="primary">#2196F3</color>
    <color name="primary_dark">#1976D2</color>
    <color name="accent">#FF4081</color>
    <color name="background">#FAFAFA</color>
    <color name="surface">#FFFFFF</color>
    <color name="error">#F44336</color>
    <color name="text_primary">#212121</color>
    <color name="text_secondary">#757575</color>
</resources>
```

### 开发调试技巧

#### 1. 日志调试
```java
// 在每个类中添加TAG常量
private static final String TAG = "ClassName";

// 使用分级日志
Log.d(TAG, "Debug message");
Log.i(TAG, "Info message");
Log.w(TAG, "Warning message");
Log.e(TAG, "Error message");
```

#### 2. 性能监控
```java
// 在关键方法中添加性能监控
long startTime = System.currentTimeMillis();
// 执行操作
long endTime = System.currentTimeMillis();
Log.d(TAG, "Operation took: " + (endTime - startTime) + "ms");
```

#### 3. 内存监控
```java
// 监控内存使用
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
Log.d(TAG, "Memory usage: " + (usedMemory / 1024 / 1024) + "MB");
```

### 常见问题解决方案

#### 1. NDK编译问题
```
问题：CMake找不到或编译失败
解决：
1. 确保NDK和CMake已正确安装
2. 检查CMakeLists.txt语法
3. 清理项目：Build → Clean Project
4. 重新构建：Build → Rebuild Project
```

#### 2. 权限问题
```
问题：录音权限被拒绝
解决：
1. 检查AndroidManifest.xml中权限声明
2. 确保运行时权限申请代码正确
3. 在设备设置中手动授权（测试时）
```

#### 3. 音频录制问题
```
问题：AudioRecord初始化失败
解决：
1. 检查音频参数配置
2. 确保设备支持指定的音频格式
3. 检查是否有其他应用占用麦克风
```

### 测试策略详解

#### 1. 单元测试最佳实践
```java
// 使用@Before设置测试环境
@Before
public void setUp() {
    // 初始化测试对象
}

// 使用@After清理资源
@After
public void tearDown() {
    // 清理测试资源
}

// 测试方法命名规范：should_ExpectedBehavior_When_StateUnderTest
@Test
public void should_ReturnTrue_When_AudioDataIsValid() {
    // 测试逻辑
}
```

#### 2. 集成测试策略
```java
// 使用TestRule管理测试生命周期
@Rule
public ActivityTestRule<MainActivity> activityRule = 
    new ActivityTestRule<>(MainActivity.class);

// 使用Espresso进行UI测试
@Test
public void should_ShowTranscriptionResult_When_RecordingCompletes() {
    // UI测试逻辑
}
```

### 发布准备清单

#### 1. 代码质量检查
- [ ] 所有TODO和FIXME已处理
- [ ] 代码符合Java编码规范
- [ ] 移除调试日志和测试代码
- [ ] 添加必要的代码注释

#### 2. 性能优化检查
- [ ] 内存泄漏检查完成
- [ ] 启动时间符合要求
- [ ] 转录性能达标
- [ ] 电池消耗合理

#### 3. 兼容性测试
- [ ] 不同Android版本测试
- [ ] 不同屏幕尺寸测试
- [ ] 不同设备型号测试
- [ ] 网络环境测试

#### 4. 安全性检查
- [ ] 权限使用合规
- [ ] 数据存储安全
- [ ] 网络通信加密
- [ ] 用户隐私保护