# 实施计划

## 开发指南

本实施计划提供了详细的开发步骤，每个任务都包含具体的实现指导。你可以：
- **自主开发**: 按照详细说明手动实现每个任务
- **使用Kiro**: 点击任务旁的"开始任务"按钮让Kiro自动实施

---

- [ ] 1. 项目初始化和基础架构搭建
  
  **目标**: 创建Android项目基础结构，配置开发环境和核心依赖
  
  **详细步骤**:
  1. **创建Android项目**
     - 使用Android Studio创建新项目，选择API 24+ (Android 7.0)
     - 项目名称: CantoneseVoiceRecognition
     - 包名: com.yourcompany.cantonese.voice
     - 语言: Java
  
  2. **配置Gradle依赖** (app/build.gradle)
     ```gradle
     dependencies {
         // Room数据库
         implementation "androidx.room:room-runtime:2.5.0"
         annotationProcessor "androidx.room:room-compiler:2.5.0"
         
         // ONNX Runtime (Whisper模型)
         implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.0'
         
         // 音频处理
         implementation 'androidx.media:media:1.6.0'
         
         // 异步处理
         implementation 'io.reactivex.rxjava3:rxjava:3.1.6'
         implementation 'io.reactivex.rxjava3:rxandroid:3.0.2'
         
         // UI组件
         implementation 'androidx.recyclerview:recyclerview:1.3.0'
         implementation 'com.google.android.material:material:1.9.0'
     }
     ```
  
  3. **配置JNI和CMake** (app/build.gradle)
     ```gradle
     android {
         ndkVersion "25.1.8937393"
         
         defaultConfig {
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
     ```
  
  4. **创建包结构**
     ```
     com.yourcompany.cantonese.voice/
     ├── ui/                    # UI层
     │   ├── main/             # 主界面
     │   ├── history/          # 历史记录
     │   └── settings/         # 设置界面
     ├── service/              # 服务层
     ├── engine/               # 语音引擎
     ├── data/                 # 数据层
     │   ├── model/           # 数据模型
     │   ├── database/        # 数据库
     │   └── repository/      # 数据仓库
     ├── audio/               # 音频处理
     ├── utils/               # 工具类
     └── native/              # JNI接口
     ```
  
  5. **创建核心接口**
     - 在各包下创建基础接口文件
     - 定义回调接口和常量类
  
  **验收标准**: 项目能够成功编译，包结构清晰，依赖配置正确
  
  _需求: 5.3, 5.4_

- [ ] 2. 数据模型和数据库实现

- [ ] 2.1 实现核心数据模型类
  
  **目标**: 创建应用的核心数据结构，支持转录记录的存储和管理
  
  **详细步骤**:
  1. **创建TranscriptionRecord类** (data/model/TranscriptionRecord.java)
     ```java
     @Entity(tableName = "transcriptions")
     public class TranscriptionRecord {
         @PrimaryKey(autoGenerate = true)
         private long id;
         
         @ColumnInfo(name = "original_text")
         private String originalText;
         
         @ColumnInfo(name = "edited_text")
         private String editedText;
         
         @ColumnInfo(name = "timestamp")
         private long timestamp;
         
         @ColumnInfo(name = "audio_file_path")
         private String audioFilePath;
         
         @ColumnInfo(name = "duration")
         private int duration; // 毫秒
         
         @ColumnInfo(name = "confidence")
         private float confidence;
         
         @ColumnInfo(name = "is_real_time")
         private boolean isRealTime;
         
         // 构造函数、getter和setter方法
     }
     ```
  
  2. **创建AudioData类** (data/model/AudioData.java)
     ```java
     public class AudioData {
         private byte[] rawData;
         private int sampleRate = 16000; // 16kHz
         private int channels = 1; // 单声道
         private int bitDepth = 16; // 16位
         private long duration;
         
         // 音频格式验证方法
         public boolean isValidFormat() {
             return sampleRate > 0 && channels > 0 && 
                    bitDepth > 0 && rawData != null;
         }
     }
     ```
  
  3. **创建TranscriptionResult类** (data/model/TranscriptionResult.java)
     ```java
     public class TranscriptionResult {
         private String text;
         private float confidence;
         private long processingTime;
         private List<WordSegment> segments;
         private boolean isComplete;
         private TranscriptionError error;
         
         // 结果验证和格式化方法
     }
     ```
  
  4. **创建WordSegment类** (data/model/WordSegment.java)
     ```java
     public class WordSegment {
         private String word;
         private float startTime;
         private float endTime;
         private float confidence;
     }
     ```
  
  **验收标准**: 所有数据模型类创建完成，包含必要的验证方法
  
  _需求: 4.1, 4.2_

- [ ] 2.2 实现Room数据库和DAO
  
  **目标**: 设置本地数据库存储，实现数据持久化
  
  **详细步骤**:
  1. **创建DAO接口** (data/database/TranscriptionDao.java)
     ```java
     @Dao
     public interface TranscriptionDao {
         @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
         List<TranscriptionRecord> getAllTranscriptions();
         
         @Query("SELECT * FROM transcriptions WHERE id = :id")
         TranscriptionRecord getTranscriptionById(long id);
         
         @Query("SELECT * FROM transcriptions WHERE original_text LIKE :query OR edited_text LIKE :query")
         List<TranscriptionRecord> searchTranscriptions(String query);
         
         @Insert
         long insertTranscription(TranscriptionRecord record);
         
         @Update
         void updateTranscription(TranscriptionRecord record);
         
         @Delete
         void deleteTranscription(TranscriptionRecord record);
         
         @Query("DELETE FROM transcriptions WHERE id = :id")
         void deleteTranscriptionById(long id);
     }
     ```
  
  2. **创建数据库类** (data/database/AppDatabase.java)
     ```java
     @Database(
         entities = {TranscriptionRecord.class},
         version = 1,
         exportSchema = false
     )
     public abstract class AppDatabase extends RoomDatabase {
         public abstract TranscriptionDao transcriptionDao();
         
         private static volatile AppDatabase INSTANCE;
         
         public static AppDatabase getDatabase(final Context context) {
             if (INSTANCE == null) {
                 synchronized (AppDatabase.class) {
                     if (INSTANCE == null) {
                         INSTANCE = Room.databaseBuilder(
                             context.getApplicationContext(),
                             AppDatabase.class,
                             "transcription_database"
                         ).build();
                     }
                 }
             }
             return INSTANCE;
         }
     }
     ```
  
  3. **实现数据库迁移策略**
     - 为未来版本升级准备Migration类
     - 定义数据库版本管理策略
  
  **验收标准**: 数据库能够正常创建，DAO方法功能完整
  
  _需求: 4.1, 4.2, 4.5_

- [ ] 2.3 实现TranscriptionRepository
  
  **目标**: 创建数据访问层，封装数据库操作和业务逻辑
  
  **详细步骤**:
  1. **创建Repository类** (data/repository/TranscriptionRepository.java)
     ```java
     public class TranscriptionRepository {
         private TranscriptionDao transcriptionDao;
         private ExecutorService executor;
         
         public TranscriptionRepository(Application application) {
             AppDatabase db = AppDatabase.getDatabase(application);
             transcriptionDao = db.transcriptionDao();
             executor = Executors.newFixedThreadPool(4);
         }
         
         // 异步保存转录记录
         public void saveTranscription(TranscriptionRecord record, 
                                     RepositoryCallback<Long> callback) {
             executor.execute(() -> {
                 try {
                     long id = transcriptionDao.insertTranscription(record);
                     callback.onSuccess(id);
                 } catch (Exception e) {
                     callback.onError(e);
                 }
             });
         }
         
         // 获取所有转录记录
         public void getAllTranscriptions(RepositoryCallback<List<TranscriptionRecord>> callback) {
             executor.execute(() -> {
                 try {
                     List<TranscriptionRecord> records = transcriptionDao.getAllTranscriptions();
                     callback.onSuccess(records);
                 } catch (Exception e) {
                     callback.onError(e);
                 }
             });
         }
         
         // 搜索转录记录
         public void searchTranscriptions(String query, 
                                        RepositoryCallback<List<TranscriptionRecord>> callback) {
             executor.execute(() -> {
                 try {
                     String searchQuery = "%" + query + "%";
                     List<TranscriptionRecord> records = 
                         transcriptionDao.searchTranscriptions(searchQuery);
                     callback.onSuccess(records);
                 } catch (Exception e) {
                     callback.onError(e);
                 }
             });
         }
     }
     ```
  
  2. **创建回调接口** (data/repository/RepositoryCallback.java)
     ```java
     public interface RepositoryCallback<T> {
         void onSuccess(T result);
         void onError(Exception error);
     }
     ```
  
  **验收标准**: Repository能够正确执行所有数据库操作，支持异步处理
  
  _需求: 4.2, 4.3, 4.4, 4.5_

- [ ]* 2.4 编写数据层单元测试
  
  **目标**: 验证数据模型和数据库操作的正确性
  
  **详细步骤**:
  1. 创建数据模型验证测试
  2. 编写数据库CRUD操作测试
  3. 测试搜索和排序功能
  
  _需求: 4.1, 4.2, 4.5_

- [ ] 3. 音频录制和处理模块

- [ ] 3.1 实现AudioRecorderManager
  
  **目标**: 创建音频录制管理器，处理设备麦克风输入和音频数据采集
  
  **详细步骤**:
  1. **创建AudioRecorderManager类** (audio/AudioRecorderManager.java)
     ```java
     public class AudioRecorderManager {
         private static final int SAMPLE_RATE = 16000;
         private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
         private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
         
         private AudioRecord audioRecord;
         private boolean isRecording = false;
         private AudioStreamListener listener;
         private ExecutorService recordingExecutor;
         
         public boolean startRecording() {
             if (!checkPermissions()) {
                 return false;
             }
             
             int bufferSize = AudioRecord.getMinBufferSize(
                 SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
             
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
             
             isRecording = true;
             audioRecord.startRecording();
             
             recordingExecutor.execute(this::recordingLoop);
             
             if (listener != null) {
                 listener.onRecordingStarted();
             }
             
             return true;
         }
         
         private void recordingLoop() {
             byte[] buffer = new byte[1024];
             
             while (isRecording) {
                 int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                 
                 if (bytesRead > 0 && listener != null) {
                     byte[] audioData = Arrays.copyOf(buffer, bytesRead);
                     listener.onAudioDataAvailable(audioData);
                 }
             }
         }
         
         public void stopRecording() {
             isRecording = false;
             
             if (audioRecord != null) {
                 audioRecord.stop();
                 audioRecord.release();
                 audioRecord = null;
             }
             
             if (listener != null) {
                 listener.onRecordingStopped();
             }
         }
         
         private boolean checkPermissions() {
             // 检查录音权限
             return ContextCompat.checkSelfPermission(context, 
                 Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
         }
     }
     ```
  
  2. **创建AudioStreamListener接口** (audio/AudioStreamListener.java)
     ```java
     public interface AudioStreamListener {
         void onAudioDataAvailable(byte[] audioData);
         void onRecordingStarted();
         void onRecordingStopped();
         void onRecordingError(AudioError error);
     }
     ```
  
  3. **添加权限检查和申请**
     - 在AndroidManifest.xml中添加录音权限
     - 实现运行时权限申请逻辑
  
  **验收标准**: 能够成功录制音频，正确处理权限，提供音频数据回调
  
  _需求: 1.1, 1.2, 2.1, 2.5_

- [ ] 3.2 实现音频数据处理工具
  
  **目标**: 创建音频处理工具类，进行格式转换和预处理
  
  **详细步骤**:
  1. **创建AudioProcessor类** (audio/AudioProcessor.java)
     ```java
     public class AudioProcessor {
         
         // 音频格式转换
         public static AudioData convertToWhisperFormat(byte[] rawAudio, 
                                                       int sampleRate) {
             // 确保音频格式符合Whisper要求 (16kHz, 16-bit, mono)
             byte[] processedAudio = rawAudio;
             
             if (sampleRate != 16000) {
                 processedAudio = resampleAudio(rawAudio, sampleRate, 16000);
             }
             
             // 归一化音频数据
             processedAudio = normalizeAudio(processedAudio);
             
             AudioData audioData = new AudioData();
             audioData.setRawData(processedAudio);
             audioData.setSampleRate(16000);
             audioData.setChannels(1);
             audioData.setBitDepth(16);
             audioData.setDuration(calculateDuration(processedAudio.length));
             
             return audioData;
         }
         
         // 音频重采样
         private static byte[] resampleAudio(byte[] input, int inputRate, int outputRate) {
             // 实现音频重采样算法
             double ratio = (double) outputRate / inputRate;
             int outputLength = (int) (input.length * ratio);
             byte[] output = new byte[outputLength];
             
             // 简单的线性插值重采样
             for (int i = 0; i < outputLength; i += 2) {
                 int inputIndex = (int) (i / ratio);
                 if (inputIndex < input.length - 1) {
                     output[i] = input[inputIndex];
                     output[i + 1] = input[inputIndex + 1];
                 }
             }
             
             return output;
         }
         
         // 音频归一化
         private static byte[] normalizeAudio(byte[] audio) {
             // 找到最大振幅
             short maxAmplitude = 0;
             for (int i = 0; i < audio.length; i += 2) {
                 short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
                 maxAmplitude = (short) Math.max(maxAmplitude, Math.abs(sample));
             }
             
             if (maxAmplitude == 0) return audio;
             
             // 归一化到合适的音量
             float normalizationFactor = 16384.0f / maxAmplitude; // 50% of max
             byte[] normalized = new byte[audio.length];
             
             for (int i = 0; i < audio.length; i += 2) {
                 short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
                 sample = (short) (sample * normalizationFactor);
                 
                 normalized[i] = (byte) (sample & 0xFF);
                 normalized[i + 1] = (byte) ((sample >> 8) & 0xFF);
             }
             
             return normalized;
         }
         
         // 语音活动检测 (VAD)
         public static boolean detectVoiceActivity(byte[] audioData, float threshold) {
             double energy = calculateAudioEnergy(audioData);
             return energy > threshold;
         }
         
         private static double calculateAudioEnergy(byte[] audioData) {
             double sum = 0;
             for (int i = 0; i < audioData.length; i += 2) {
                 short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                 sum += sample * sample;
             }
             return sum / (audioData.length / 2);
         }
     }
     ```
  
  2. **创建音频缓冲管理器** (audio/AudioBuffer.java)
     ```java
     public class AudioBuffer {
         private final Queue<byte[]> audioQueue = new LinkedList<>();
         private final int maxBufferSize;
         private int currentSize = 0;
         
         public synchronized void addAudioData(byte[] data) {
             audioQueue.offer(data);
             currentSize += data.length;
             
             // 限制缓冲区大小
             while (currentSize > maxBufferSize && !audioQueue.isEmpty()) {
                 byte[] removed = audioQueue.poll();
                 currentSize -= removed.length;
             }
         }
         
         public synchronized byte[] getBufferedAudio() {
             if (audioQueue.isEmpty()) return null;
             
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             while (!audioQueue.isEmpty()) {
                 try {
                     outputStream.write(audioQueue.poll());
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
             currentSize = 0;
             return outputStream.toByteArray();
         }
     }
     ```
  
  **验收标准**: 音频数据能够正确转换为Whisper所需格式，VAD功能正常工作
  
  _需求: 1.3, 2.2, 3.3_

- [ ]* 3.3 编写音频模块单元测试
  
  **目标**: 验证音频录制和处理功能的正确性
  
  **详细步骤**:
  1. 测试AudioRecorderManager的录音功能
  2. 验证音频格式转换的准确性
  3. 测试VAD和音频缓冲功能
  
  _需求: 1.1, 1.4, 2.1_

- [ ] 4. Whisper模型集成

- [ ] 4.1 创建JNI接口和native代码
  
  **目标**: 通过JNI集成Whisper C++库，实现Java与native代码的桥接
  
  **详细步骤**:
  1. **创建CMakeLists.txt** (src/main/cpp/CMakeLists.txt)
     ```cmake
     cmake_minimum_required(VERSION 3.22.1)
     project("cantonese_voice")
     
     # 添加ONNX Runtime库
     set(ONNXRUNTIME_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/onnxruntime)
     set(ONNXRUNTIME_INCLUDE_DIRS ${ONNXRUNTIME_ROOT_PATH}/include)
     set(ONNXRUNTIME_LIB ${ONNXRUNTIME_ROOT_PATH}/lib/${ANDROID_ABI}/libonnxruntime.so)
     
     # 包含头文件
     include_directories(${ONNXRUNTIME_INCLUDE_DIRS})
     
     # 创建native库
     add_library(cantonese_voice SHARED
         whisper_jni.cpp
         whisper_engine.cpp
         audio_utils.cpp
     )
     
     # 链接库
     target_link_libraries(cantonese_voice
         ${ONNXRUNTIME_LIB}
         android
         log
     )
     ```
  
  2. **创建JNI接口** (src/main/cpp/whisper_jni.cpp)
     ```cpp
     #include <jni.h>
     #include <android/log.h>
     #include "whisper_engine.h"
     
     #define LOG_TAG "WhisperJNI"
     #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
     #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
     
     static WhisperEngine* g_whisper_engine = nullptr;
     
     extern "C" JNIEXPORT jboolean JNICALL
     Java_com_yourcompany_cantonese_voice_native_WhisperJNI_initializeModel(
         JNIEnv *env, jobject thiz, jstring model_path) {
         
         const char* path = env->GetStringUTFChars(model_path, 0);
         
         if (g_whisper_engine == nullptr) {
             g_whisper_engine = new WhisperEngine();
         }
         
         bool result = g_whisper_engine->initialize(path);
         
         env->ReleaseStringUTFChars(model_path, path);
         return result;
     }
     
     extern "C" JNIEXPORT jstring JNICALL
     Java_com_yourcompany_cantonese_voice_native_WhisperJNI_transcribeAudio(
         JNIEnv *env, jobject thiz, jbyteArray audio_data) {
         
         if (g_whisper_engine == nullptr) {
             return env->NewStringUTF("");
         }
         
         jbyte* audio_bytes = env->GetByteArrayElements(audio_data, 0);
         jsize audio_length = env->GetArrayLength(audio_data);
         
         std::string result = g_whisper_engine->transcribe(
             reinterpret_cast<float*>(audio_bytes), audio_length / sizeof(float));
         
         env->ReleaseByteArrayElements(audio_data, audio_bytes, 0);
         
         return env->NewStringUTF(result.c_str());
     }
     
     extern "C" JNIEXPORT void JNICALL
     Java_com_yourcompany_cantonese_voice_native_WhisperJNI_releaseModel(
         JNIEnv *env, jobject thiz) {
         
         if (g_whisper_engine != nullptr) {
             delete g_whisper_engine;
             g_whisper_engine = nullptr;
         }
     }
     ```
  
  3. **创建Whisper引擎C++类** (src/main/cpp/whisper_engine.h)
     ```cpp
     #ifndef WHISPER_ENGINE_H
     #define WHISPER_ENGINE_H
     
     #include <string>
     #include <vector>
     #include <onnxruntime_cxx_api.h>
     
     class WhisperEngine {
     public:
         WhisperEngine();
         ~WhisperEngine();
         
         bool initialize(const std::string& model_path);
         std::string transcribe(const float* audio_data, size_t length);
         bool isInitialized() const { return initialized_; }
         
     private:
         bool initialized_;
         std::unique_ptr<Ort::Session> session_;
         std::unique_ptr<Ort::Env> env_;
         std::vector<const char*> input_names_;
         std::vector<const char*> output_names_;
         
         std::vector<float> preprocessAudio(const float* audio_data, size_t length);
         std::string postprocessOutput(const std::vector<float>& output);
     };
     
     #endif // WHISPER_ENGINE_H
     ```
  
  4. **实现Whisper引擎** (src/main/cpp/whisper_engine.cpp)
     ```cpp
     #include "whisper_engine.h"
     #include <android/log.h>
     
     #define LOG_TAG "WhisperEngine"
     #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
     #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
     
     WhisperEngine::WhisperEngine() : initialized_(false) {
         env_ = std::make_unique<Ort::Env>(ORT_LOGGING_LEVEL_WARNING, "WhisperEngine");
     }
     
     bool WhisperEngine::initialize(const std::string& model_path) {
         try {
             Ort::SessionOptions session_options;
             session_options.SetIntraOpNumThreads(4);
             session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
             
             session_ = std::make_unique<Ort::Session>(*env_, model_path.c_str(), session_options);
             
             // 获取输入输出名称
             Ort::AllocatorWithDefaultOptions allocator;
             
             size_t num_input_nodes = session_->GetInputCount();
             input_names_.reserve(num_input_nodes);
             for (size_t i = 0; i < num_input_nodes; i++) {
                 char* input_name = session_->GetInputName(i, allocator);
                 input_names_.push_back(input_name);
             }
             
             size_t num_output_nodes = session_->GetOutputCount();
             output_names_.reserve(num_output_nodes);
             for (size_t i = 0; i < num_output_nodes; i++) {
                 char* output_name = session_->GetOutputName(i, allocator);
                 output_names_.push_back(output_name);
             }
             
             initialized_ = true;
             LOGI("Whisper model initialized successfully");
             return true;
             
         } catch (const std::exception& e) {
             LOGE("Failed to initialize Whisper model: %s", e.what());
             return false;
         }
     }
     
     std::string WhisperEngine::transcribe(const float* audio_data, size_t length) {
         if (!initialized_) {
             return "";
         }
         
         try {
             // 预处理音频数据
             std::vector<float> processed_audio = preprocessAudio(audio_data, length);
             
             // 创建输入tensor
             std::vector<int64_t> input_shape = {1, static_cast<int64_t>(processed_audio.size())};
             Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
             
             Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                 memory_info, processed_audio.data(), processed_audio.size(),
                 input_shape.data(), input_shape.size());
             
             // 运行推理
             auto output_tensors = session_->Run(Ort::RunOptions{nullptr},
                 input_names_.data(), &input_tensor, 1,
                 output_names_.data(), output_names_.size());
             
             // 后处理输出
             float* output_data = output_tensors[0].GetTensorMutableData<float>();
             size_t output_size = output_tensors[0].GetTensorTypeAndShapeInfo().GetElementCount();
             
             std::vector<float> output_vector(output_data, output_data + output_size);
             return postprocessOutput(output_vector);
             
         } catch (const std::exception& e) {
             LOGE("Transcription failed: %s", e.what());
             return "";
         }
     }
     ```
  
  **验收标准**: JNI接口能够正确加载模型，音频数据能够在Java和C++间正确传递
  
  _需求: 1.4, 3.3, 3.4, 5.2_

- [ ] 4.2 实现WhisperEngine类
  
  **目标**: 创建Java层的Whisper引擎封装，提供易用的转录接口
  
  **详细步骤**:
  1. **创建WhisperJNI接口类** (native/WhisperJNI.java)
     ```java
     public class WhisperJNI {
         static {
             System.loadLibrary("cantonese_voice");
         }
         
         public native boolean initializeModel(String modelPath);
         public native String transcribeAudio(byte[] audioData);
         public native void releaseModel();
     }
     ```
  
  2. **创建WhisperEngine类** (engine/WhisperEngine.java)
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
                 File modelFile = new File(modelPath);
                 if (!modelFile.exists()) {
                     Log.e("WhisperEngine", "Model file not found: " + modelPath);
                     return false;
                 }
                 
                 isModelLoaded = whisperJNI.initializeModel(modelPath);
                 Log.i("WhisperEngine", "Model loaded: " + isModelLoaded);
                 return isModelLoaded;
                 
             } catch (Exception e) {
                 Log.e("WhisperEngine", "Failed to initialize model", e);
                 return false;
             }
         }
         
         public void transcribe(AudioData audioData, TranscriptionCallback callback) {
             if (!isModelLoaded) {
                 callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
                 return;
             }
             
             executorService.execute(() -> {
                 try {
                     callback.onTranscriptionStarted();
                     
                     long startTime = System.currentTimeMillis();
                     
                     // 转换音频数据为Whisper所需格式
                     byte[] processedAudio = AudioProcessor.convertToWhisperFormat(
                         audioData.getRawData(), audioData.getSampleRate()).getRawData();
                     
                     String result = whisperJNI.transcribeAudio(processedAudio);
                     
                     long processingTime = System.currentTimeMillis() - startTime;
                     
                     TranscriptionResult transcriptionResult = new TranscriptionResult();
                     transcriptionResult.setText(result);
                     transcriptionResult.setProcessingTime(processingTime);
                     transcriptionResult.setComplete(true);
                     transcriptionResult.setConfidence(0.85f); // 默认置信度
                     
                     callback.onTranscriptionCompleted(transcriptionResult);
                     
                 } catch (Exception e) {
                     Log.e("WhisperEngine", "Transcription failed", e);
                     callback.onTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                 }
             });
         }
         
         public void transcribeRealTime(AudioStream audioStream, TranscriptionCallback callback) {
             if (!isModelLoaded) {
                 callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
                 return;
             }
             
             // 实时转录实现
             executorService.execute(() -> {
                 AudioBuffer buffer = new AudioBuffer(8000); // 8秒缓冲
                 
                 audioStream.setListener(new AudioStreamListener() {
                     @Override
                     public void onAudioDataAvailable(byte[] audioData) {
                         buffer.addAudioData(audioData);
                         
                         // 每2秒处理一次缓冲的音频
                         if (buffer.getCurrentSize() >= 32000) { // 2秒的16kHz音频
                             byte[] bufferedAudio = buffer.getBufferedAudio();
                             
                             try {
                                 String partialResult = whisperJNI.transcribeAudio(bufferedAudio);
                                 if (!partialResult.isEmpty()) {
                                     callback.onPartialResult(partialResult);
                                 }
                             } catch (Exception e) {
                                 Log.e("WhisperEngine", "Real-time transcription error", e);
                             }
                         }
                     }
                 });
             });
         }
         
         public boolean isModelLoaded() {
             return isModelLoaded;
         }
         
         public void releaseModel() {
             if (isModelLoaded) {
                 whisperJNI.releaseModel();
                 isModelLoaded = false;
             }
             
             if (executorService != null) {
                 executorService.shutdown();
             }
         }
     }
     ```
  
  3. **创建错误类型枚举** (engine/TranscriptionError.java)
     ```java
     public enum TranscriptionError {
         MODEL_NOT_LOADED("语音模型未加载"),
         AUDIO_FORMAT_UNSUPPORTED("不支持的音频格式"),
         INSUFFICIENT_STORAGE("存储空间不足"),
         PERMISSION_DENIED("权限被拒绝"),
         NETWORK_ERROR("网络错误"),
         MODEL_CORRUPTED("模型文件损坏"),
         TRANSCRIPTION_FAILED("转录失败");
         
         private final String message;
         
         TranscriptionError(String message) {
             this.message = message;
         }
         
         public String getMessage() {
             return message;
         }
     }
     ```
  
  **验收标准**: WhisperEngine能够正确加载模型，执行转录任务，处理错误情况
  
  _需求: 1.3, 1.4, 2.3, 3.3, 3.4_

- [ ] 4.3 实现OfflineModeManager
  
  **目标**: 管理离线模式，处理模型文件下载和本地存储
  
  **详细步骤**:
  1. **创建OfflineModeManager类** (engine/OfflineModeManager.java)
     ```java
     public class OfflineModeManager {
         private static final String MODEL_FILENAME = "whisper_cantonese.onnx";
         private static final String MODEL_URL = "https://your-server.com/models/whisper_cantonese.onnx";
         
         private Context context;
         private SharedPreferences preferences;
         private boolean isOfflineModeEnabled = false;
         
         public OfflineModeManager(Context context) {
             this.context = context;
             this.preferences = context.getSharedPreferences("offline_mode", Context.MODE_PRIVATE);
             this.isOfflineModeEnabled = preferences.getBoolean("offline_enabled", false);
         }
         
         public boolean isOfflineModeAvailable() {
             File modelFile = new File(getModelPath());
             return modelFile.exists() && modelFile.length() > 0;
         }
         
         public void enableOfflineMode() {
             isOfflineModeEnabled = true;
             preferences.edit().putBoolean("offline_enabled", true).apply();
         }
         
         public void disableOfflineMode() {
             isOfflineModeEnabled = false;
             preferences.edit().putBoolean("offline_enabled", false).apply();
         }
         
         public boolean isOfflineModeEnabled() {
             return isOfflineModeEnabled;
         }
         
         public void downloadModel(ModelDownloadCallback callback) {
             if (isOfflineModeAvailable()) {
                 callback.onDownloadCompleted(getModelPath());
                 return;
             }
             
             ExecutorService executor = Executors.newSingleThreadExecutor();
             executor.execute(() -> {
                 try {
                     callback.onDownloadStarted();
                     
                     URL url = new URL(MODEL_URL);
                     HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                     connection.connect();
                     
                     int fileLength = connection.getContentLength();
                     
                     InputStream input = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream output = new FileOutputStream(getModelPath());
                     
                     byte[] data = new byte[1024];
                     long total = 0;
                     int count;
                     
                     while ((count = input.read(data)) != -1) {
                         total += count;
                         output.write(data, 0, count);
                         
                         float progress = (float) total / fileLength;
                         callback.onDownloadProgress(progress);
                     }
                     
                     output.flush();
                     output.close();
                     input.close();
                     
                     // 验证下载的模型文件
                     if (validateModelFile(getModelPath())) {
                         callback.onDownloadCompleted(getModelPath());
                     } else {
                         callback.onDownloadError(new Exception("模型文件验证失败"));
                     }
                     
                 } catch (Exception e) {
                     callback.onDownloadError(e);
                 }
             });
         }
         
         private String getModelPath() {
             File modelsDir = new File(context.getFilesDir(), "models");
             if (!modelsDir.exists()) {
                 modelsDir.mkdirs();
             }
             return new File(modelsDir, MODEL_FILENAME).getAbsolutePath();
         }
         
         private boolean validateModelFile(String modelPath) {
             File modelFile = new File(modelPath);
             
             // 检查文件大小（模型文件应该大于10MB）
             if (modelFile.length() < 10 * 1024 * 1024) {
                 return false;
             }
             
             // 可以添加更多验证逻辑，如文件头检查等
             return true;
         }
         
         public float getModelDownloadProgress() {
             // 返回当前下载进度，可以从SharedPreferences中获取
             return preferences.getFloat("download_progress", 0.0f);
         }
     }
     ```
  
  2. **创建下载回调接口** (engine/ModelDownloadCallback.java)
     ```java
     public interface ModelDownloadCallback {
         void onDownloadStarted();
         void onDownloadProgress(float progress);
         void onDownloadCompleted(String modelPath);
         void onDownloadError(Exception error);
     }
     ```
  
  **验收标准**: 能够检测离线模式可用性，正确下载和验证模型文件
  
  _需求: 3.1, 3.2, 3.4, 5.4_

- [ ]* 4.4 编写Whisper集成测试
  
  **目标**: 验证Whisper模型集成的正确性和稳定性
  
  **详细步骤**:
  1. 测试模型加载和释放功能
  2. 验证音频转录的准确性
  3. 测试离线模式的工作状态
  4. 验证错误处理机制
  
  _需求: 1.4, 3.3, 3.4_

- [ ] 5. 转录服务实现

- [ ] 5.1 实现TranscriptionService后台服务
  
  **目标**: 创建Android后台服务，处理长时间的转录任务
  
  **详细步骤**:
  1. **创建TranscriptionService类** (service/TranscriptionService.java)
     ```java
     public class TranscriptionService extends Service {
         private static final int NOTIFICATION_ID = 1001;
         private static final String CHANNEL_ID = "transcription_channel";
         
         private WhisperEngine whisperEngine;
         private AudioRecorderManager audioRecorder;
         private TranscriptionRepository repository;
         private boolean isRealTimeMode = false;
         
         private final IBinder binder = new TranscriptionBinder();
         
         public class TranscriptionBinder extends Binder {
             TranscriptionService getService() {
                 return TranscriptionService.this;
             }
         }
         
         @Override
         public void onCreate() {
             super.onCreate();
             
             whisperEngine = new WhisperEngine();
             audioRecorder = new AudioRecorderManager(this);
             repository = new TranscriptionRepository(getApplication());
             
             createNotificationChannel();
             
             // 初始化Whisper模型
             OfflineModeManager offlineManager = new OfflineModeManager(this);
             if (offlineManager.isOfflineModeAvailable()) {
                 whisperEngine.initializeModel(offlineManager.getModelPath());
             }
         }
         
         @Override
         public int onStartCommand(Intent intent, int flags, int startId) {
             String action = intent.getAction();
             
             if ("START_REAL_TIME".equals(action)) {
                 startRealTimeTranscription();
             } else if ("STOP_REAL_TIME".equals(action)) {
                 stopRealTimeTranscription();
             } else if ("TRANSCRIBE_FILE".equals(action)) {
                 String filePath = intent.getStringExtra("file_path");
                 transcribeAudioFile(filePath);
             }
             
             return START_STICKY;
         }
         
         public void startRealTimeTranscription() {
             if (isRealTimeMode) return;
             
             isRealTimeMode = true;
             startForeground(NOTIFICATION_ID, createNotification("实时转录进行中..."));
             
             audioRecorder.setAudioStreamListener(new AudioStreamListener() {
                 private AudioBuffer audioBuffer = new AudioBuffer(16000); // 1秒缓冲
                 
                 @Override
                 public void onAudioDataAvailable(byte[] audioData) {
                     audioBuffer.addAudioData(audioData);
                     
                     // 检测语音活动
                     if (AudioProcessor.detectVoiceActivity(audioData, 1000.0f)) {
                         processRealTimeAudio();
                     }
                 }
                 
                 private void processRealTimeAudio() {
                     byte[] bufferedAudio = audioBuffer.getBufferedAudio();
                     if (bufferedAudio != null && bufferedAudio.length > 0) {
                         
                         AudioData audioData = AudioProcessor.convertToWhisperFormat(
                             bufferedAudio, 16000);
                         
                         whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                             @Override
                             public void onPartialResult(String partialText) {
                                 // 发送实时结果广播
                                 sendRealTimeResult(partialText, false);
                             }
                             
                             @Override
                             public void onTranscriptionCompleted(TranscriptionResult result) {
                                 // 保存完整转录结果
                                 saveTranscriptionResult(result, true);
                                 sendRealTimeResult(result.getText(), true);
                             }
                             
                             @Override
                             public void onTranscriptionError(TranscriptionError error) {
                                 sendTranscriptionError(error);
                             }
                         });
                     }
                 }
             });
             
             audioRecorder.startRecording();
         }
         
         public void stopRealTimeTranscription() {
             if (!isRealTimeMode) return;
             
             isRealTimeMode = false;
             audioRecorder.stopRecording();
             stopForeground(true);
         }
         
         private void transcribeAudioFile(String filePath) {
             startForeground(NOTIFICATION_ID, createNotification("转录音频文件..."));
             
             ExecutorService executor = Executors.newSingleThreadExecutor();
             executor.execute(() -> {
                 try {
                     // 读取音频文件
                     AudioData audioData = AudioFileReader.readAudioFile(filePath);
                     
                     whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                         @Override
                         public void onTranscriptionCompleted(TranscriptionResult result) {
                             saveTranscriptionResult(result, false);
                             sendTranscriptionCompleted(result);
                             stopForeground(true);
                         }
                         
                         @Override
                         public void onTranscriptionError(TranscriptionError error) {
                             sendTranscriptionError(error);
                             stopForeground(true);
                         }
                     });
                     
                 } catch (Exception e) {
                     sendTranscriptionError(TranscriptionError.TRANSCRIPTION_FAILED);
                     stopForeground(true);
                 }
             });
         }
         
         private void saveTranscriptionResult(TranscriptionResult result, boolean isRealTime) {
             TranscriptionRecord record = new TranscriptionRecord();
             record.setOriginalText(result.getText());
             record.setEditedText(result.getText());
             record.setTimestamp(System.currentTimeMillis());
             record.setConfidence(result.getConfidence());
             record.setRealTime(isRealTime);
             record.setDuration((int) result.getProcessingTime());
             
             repository.saveTranscription(record, new RepositoryCallback<Long>() {
                 @Override
                 public void onSuccess(Long id) {
                     Log.i("TranscriptionService", "Transcription saved with ID: " + id);
                 }
                 
                 @Override
                 public void onError(Exception error) {
                     Log.e("TranscriptionService", "Failed to save transcription", error);
                 }
             });
         }
         
         private Notification createNotification(String content) {
             return new NotificationCompat.Builder(this, CHANNEL_ID)
                 .setContentTitle("粤语语音识别")
                 .setContentText(content)
                 .setSmallIcon(R.drawable.ic_mic)
                 .setOngoing(true)
                 .build();
         }
         
         private void createNotificationChannel() {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 NotificationChannel channel = new NotificationChannel(
                     CHANNEL_ID,
                     "转录服务",
                     NotificationManager.IMPORTANCE_LOW
                 );
                 
                 NotificationManager manager = getSystemService(NotificationManager.class);
                 manager.createNotificationChannel(channel);
             }
         }
     }
     ```
  
  **验收标准**: 后台服务能够正常运行，支持前台服务通知，正确处理转录任务
  
  _需求: 1.3, 2.1, 2.2, 2.3_

- [ ] 5.2 实现实时转录功能
  
  **目标**: 集成音频录制和语音识别，实现流式实时转录
  
  **详细步骤**:
  1. **创建RealTimeTranscriber类** (service/RealTimeTranscriber.java)
     ```java
     public class RealTimeTranscriber {
         private static final int BUFFER_SIZE_MS = 2000; // 2秒缓冲
         private static final float VAD_THRESHOLD = 1500.0f;
         
         private WhisperEngine whisperEngine;
         private AudioBuffer audioBuffer;
         private TranscriptionCallback callback;
         private boolean isActive = false;
         
         private Timer vadTimer;
         private long lastVoiceActivity = 0;
         private StringBuilder currentSentence = new StringBuilder();
         
         public RealTimeTranscriber(WhisperEngine engine) {
             this.whisperEngine = engine;
             this.audioBuffer = new AudioBuffer(BUFFER_SIZE_MS * 16); // 16kHz采样率
         }
         
         public void start(TranscriptionCallback callback) {
             this.callback = callback;
             this.isActive = true;
             
             // 启动VAD定时器
             vadTimer = new Timer();
             vadTimer.scheduleAtFixedRate(new TimerTask() {
                 @Override
                 public void run() {
                     checkSentenceCompletion();
                 }
             }, 500, 500); // 每500ms检查一次
             
             callback.onTranscriptionStarted();
         }
         
         public void stop() {
             isActive = false;
             
             if (vadTimer != null) {
                 vadTimer.cancel();
                 vadTimer = null;
             }
             
             // 处理最后的音频缓冲
             processRemainingAudio();
         }
         
         public void processAudioData(byte[] audioData) {
             if (!isActive) return;
             
             audioBuffer.addAudioData(audioData);
             
             // 检测语音活动
             boolean hasVoice = AudioProcessor.detectVoiceActivity(audioData, VAD_THRESHOLD);
             
             if (hasVoice) {
                 lastVoiceActivity = System.currentTimeMillis();
                 
                 // 如果缓冲区有足够的数据，进行转录
                 if (audioBuffer.getCurrentSize() >= BUFFER_SIZE_MS * 32) { // 32 bytes per ms for 16kHz 16-bit
                     processBufferedAudio();
                 }
             }
         }
         
         private void processBufferedAudio() {
             byte[] bufferedAudio = audioBuffer.getPartialAudio(0.5f); // 获取50%的缓冲数据
             
             if (bufferedAudio != null && bufferedAudio.length > 0) {
                 AudioData audioData = AudioProcessor.convertToWhisperFormat(bufferedAudio, 16000);
                 
                 whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                     @Override
                     public void onTranscriptionCompleted(TranscriptionResult result) {
                         String text = result.getText().trim();
                         
                         if (!text.isEmpty()) {
                             // 处理部分结果
                             processPartialResult(text);
                         }
                     }
                     
                     @Override
                     public void onTranscriptionError(TranscriptionError error) {
                         if (callback != null) {
                             callback.onTranscriptionError(error);
                         }
                     }
                 });
             }
         }
         
         private void processPartialResult(String text) {
             // 简单的句子分割逻辑
             if (text.endsWith("。") || text.endsWith("！") || text.endsWith("？")) {
                 // 完整句子
                 currentSentence.append(text);
                 String completeSentence = currentSentence.toString();
                 
                 if (callback != null) {
                     TranscriptionResult result = new TranscriptionResult();
                     result.setText(completeSentence);
                     result.setComplete(true);
                     result.setConfidence(0.85f);
                     
                     callback.onTranscriptionCompleted(result);
                 }
                 
                 currentSentence.setLength(0); // 清空缓冲
             } else {
                 // 部分结果
                 currentSentence.append(text);
                 
                 if (callback != null) {
                     callback.onPartialResult(currentSentence.toString());
                 }
             }
         }
         
         private void checkSentenceCompletion() {
             long timeSinceLastVoice = System.currentTimeMillis() - lastVoiceActivity;
             
             // 如果2秒内没有语音活动，认为句子结束
             if (timeSinceLastVoice > 2000 && currentSentence.length() > 0) {
                 String completeSentence = currentSentence.toString();
                 
                 if (callback != null) {
                     TranscriptionResult result = new TranscriptionResult();
                     result.setText(completeSentence);
                     result.setComplete(true);
                     result.setConfidence(0.80f); // 稍低的置信度，因为可能不完整
                     
                     callback.onTranscriptionCompleted(result);
                 }
                 
                 currentSentence.setLength(0);
             }
         }
         
         private void processRemainingAudio() {
             if (currentSentence.length() > 0) {
                 String remainingText = currentSentence.toString();
                 
                 if (callback != null) {
                     TranscriptionResult result = new TranscriptionResult();
                     result.setText(remainingText);
                     result.setComplete(true);
                     result.setConfidence(0.75f);
                     
                     callback.onTranscriptionCompleted(result);
                 }
             }
         }
     }
     ```
  
  **验收标准**: 实时转录功能正常工作，能够检测语音活动，正确分割句子
  
  _需求: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 5.3 实现转录结果处理
  
  **目标**: 处理转录完成后的数据保存和结果格式化
  
  **详细步骤**:
  1. **创建TranscriptionProcessor类** (service/TranscriptionProcessor.java)
     ```java
     public class TranscriptionProcessor {
         
         public static TranscriptionRecord createTranscriptionRecord(
             TranscriptionResult result, boolean isRealTime, String audioFilePath) {
             
             TranscriptionRecord record = new TranscriptionRecord();
             record.setOriginalText(result.getText());
             record.setEditedText(result.getText());
             record.setTimestamp(System.currentTimeMillis());
             record.setConfidence(result.getConfidence());
             record.setRealTime(isRealTime);
             record.setAudioFilePath(audioFilePath);
             record.setDuration((int) result.getProcessingTime());
             
             return record;
         }
         
         public static String formatTranscriptionText(String rawText) {
             if (rawText == null || rawText.trim().isEmpty()) {
                 return "";
             }
             
             // 基本的文本格式化
             String formatted = rawText.trim();
             
             // 移除多余的空格
             formatted = formatted.replaceAll("\\s+", " ");
             
             // 确保句子以标点符号结尾
             if (!formatted.matches(".*[。！？]$")) {
                 formatted += "。";
             }
             
             // 首字母大写（如果包含英文）
             formatted = capitalizeFirstLetter(formatted);
             
             return formatted;
         }
         
         private static String capitalizeFirstLetter(String text) {
             if (text.length() > 0) {
                 return text.substring(0, 1).toUpperCase() + text.substring(1);
             }
             return text;
         }
         
         public static void postProcessTranscription(TranscriptionRecord record, 
                                                   PostProcessCallback callback) {
             
             ExecutorService executor = Executors.newSingleThreadExecutor();
             executor.execute(() -> {
                 try {
                     // 格式化文本
                     String formattedText = formatTranscriptionText(record.getOriginalText());
                     record.setEditedText(formattedText);
                     
                     // 计算文本统计信息
                     int wordCount = countWords(formattedText);
                     int charCount = formattedText.length();
                     
                     // 可以添加更多后处理逻辑，如：
                     // - 语法检查
                     // - 专有名词识别
                     // - 情感分析等
                     
                     callback.onPostProcessCompleted(record, wordCount, charCount);
                     
                 } catch (Exception e) {
                     callback.onPostProcessError(e);
                 }
             });
         }
         
         private static int countWords(String text) {
             if (text == null || text.trim().isEmpty()) {
                 return 0;
             }
             
             // 简单的中文分词计数
             return text.replaceAll("[\\s\\p{Punct}]", "").length();
         }
     }
     ```
  
  2. **创建后处理回调接口** (service/PostProcessCallback.java)
     ```java
     public interface PostProcessCallback {
         void onPostProcessCompleted(TranscriptionRecord record, int wordCount, int charCount);
         void onPostProcessError(Exception error);
     }
     ```
  
  **验收标准**: 转录结果能够正确保存，文本格式化功能正常工作
  
  _需求: 1.3, 1.5, 4.1_

- [ ]* 5.4 编写转录服务测试
  
  **目标**: 验证转录服务的功能和性能
  
  **详细步骤**:
  1. 测试后台服务的启动和停止
  2. 验证实时转录的准确性和响应时间
  3. 测试转录结果的保存和格式化
  4. 验证错误处理和恢复机制
  
  _需求: 1.4, 2.3, 2.4_

- [ ] 6. 用户界面实现

- [ ] 6.1 创建主界面Activity
  
  **目标**: 实现应用主界面，提供录音控制和转录结果显示功能
  
  **详细步骤**:
  1. **创建主界面布局** (res/layout/activity_main.xml)
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical"
         android:padding="16dp">
         
         <!-- 转录结果显示区域 -->
         <ScrollView
             android:layout_width="match_parent"
             android:layout_height="0dp"
             android:layout_weight="1"
             android:background="@drawable/transcription_background">
             
             <TextView
                 android:id="@+id/tv_transcription_result"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:padding="16dp"
                 android:textSize="16sp"
                 android:textColor="@color/text_primary"
                 android:hint="转录结果将显示在这里..."
                 android:gravity="top" />
         </ScrollView>
         
         <!-- 实时转录状态 -->
         <LinearLayout
             android:id="@+id/ll_realtime_status"
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:orientation="horizontal"
             android:padding="8dp"
             android:visibility="gone">
             
             <ProgressBar
                 android:layout_width="24dp"
                 android:layout_height="24dp"
                 android:layout_marginEnd="8dp" />
             
             <TextView
                 android:id="@+id/tv_realtime_status"
                 android:layout_width="wrap_content"
                 android:layout_height="wrap_content"
                 android:text="实时转录中..."
                 android:textColor="@color/accent_color" />
         </LinearLayout>
         
         <!-- 录音控制区域 -->
         <LinearLayout
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:orientation="vertical"
             android:gravity="center">
             
             <!-- 录音按钮 -->
             <com.google.android.material.floatingactionbutton.FloatingActionButton
                 android:id="@+id/fab_record"
                 android:layout_width="wrap_content"
                 android:layout_height="wrap_content"
                 android:layout_margin="16dp"
                 android:src="@drawable/ic_mic"
                 android:contentDescription="开始录音" />
             
             <!-- 录音状态指示器 -->
             <LinearLayout
                 android:layout_width="wrap_content"
                 android:layout_height="wrap_content"
                 android:orientation="horizontal"
                 android:gravity="center">
                 
                 <ImageView
                     android:id="@+id/iv_recording_indicator"
                     android:layout_width="12dp"
                     android:layout_height="12dp"
                     android:src="@drawable/ic_recording_dot"
                     android:visibility="gone" />
                 
                 <TextView
                     android:id="@+id/tv_recording_time"
                     android:layout_width="wrap_content"
                     android:layout_height="wrap_content"
                     android:layout_marginStart="8dp"
                     android:text="00:00"
                     android:textColor="@color/text_secondary" />
             </LinearLayout>
             
             <!-- 控制按钮组 -->
             <LinearLayout
                 android:layout_width="wrap_content"
                 android:layout_height="wrap_content"
                 android:orientation="horizontal"
                 android:layout_marginTop="16dp">
                 
                 <Button
                     android:id="@+id/btn_realtime_mode"
                     android:layout_width="wrap_content"
                     android:layout_height="wrap_content"
                     android:text="实时模式"
                     android:layout_marginEnd="8dp"
                     style="@style/Widget.MaterialComponents.Button.OutlinedButton" />
                 
                 <Button
                     android:id="@+id/btn_history"
                     android:layout_width="wrap_content"
                     android:layout_height="wrap_content"
                     android:text="历史记录"
                     android:layout_marginStart="8dp"
                     style="@style/Widget.MaterialComponents.Button.OutlinedButton" />
             </LinearLayout>
         </LinearLayout>
     </LinearLayout>
     ```
  
  2. **创建MainActivity类** (ui/main/MainActivity.java)
     ```java
     public class MainActivity extends AppCompatActivity {
         private FloatingActionButton fabRecord;
         private TextView tvTranscriptionResult;
         private TextView tvRecordingTime;
         private ImageView ivRecordingIndicator;
         private LinearLayout llRealtimeStatus;
         private Button btnRealtimeMode;
         private Button btnHistory;
         
         private TranscriptionService transcriptionService;
         private boolean isServiceBound = false;
         private boolean isRecording = false;
         private boolean isRealtimeMode = false;
         
         private Timer recordingTimer;
         private long recordingStartTime;
         
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_main);
             
             initViews();
             setupClickListeners();
             checkPermissions();
             bindTranscriptionService();
         }
         
         private void initViews() {
             fabRecord = findViewById(R.id.fab_record);
             tvTranscriptionResult = findViewById(R.id.tv_transcription_result);
             tvRecordingTime = findViewById(R.id.tv_recording_time);
             ivRecordingIndicator = findViewById(R.id.iv_recording_indicator);
             llRealtimeStatus = findViewById(R.id.ll_realtime_status);
             btnRealtimeMode = findViewById(R.id.btn_realtime_mode);
             btnHistory = findViewById(R.id.btn_history);
         }
         
         private void setupClickListeners() {
             fabRecord.setOnClickListener(v -> toggleRecording());
             btnRealtimeMode.setOnClickListener(v -> toggleRealtimeMode());
             btnHistory.setOnClickListener(v -> openHistoryActivity());
         }
         
         private void toggleRecording() {
             if (isRealtimeMode) {
                 toggleRealtimeRecording();
             } else {
                 toggleSingleRecording();
             }
         }
         
         private void toggleSingleRecording() {
             if (!isRecording) {
                 startSingleRecording();
             } else {
                 stopSingleRecording();
             }
         }
         
         private void startSingleRecording() {
             if (transcriptionService != null) {
                 isRecording = true;
                 updateRecordingUI(true);
                 startRecordingTimer();
                 
                 // 开始录音
                 transcriptionService.startSingleRecording(new TranscriptionCallback() {
                     @Override
                     public void onTranscriptionCompleted(TranscriptionResult result) {
                         runOnUiThread(() -> {
                             displayTranscriptionResult(result.getText());
                             stopSingleRecording();
                         });
                     }
                     
                     @Override
                     public void onTranscriptionError(TranscriptionError error) {
                         runOnUiThread(() -> {
                             showError(error.getMessage());
                             stopSingleRecording();
                         });
                     }
                 });
             }
         }
         
         private void stopSingleRecording() {
             if (transcriptionService != null && isRecording) {
                 transcriptionService.stopSingleRecording();
                 isRecording = false;
                 updateRecordingUI(false);
                 stopRecordingTimer();
             }
         }
         
         private void toggleRealtimeMode() {
             if (!isRealtimeMode) {
                 startRealtimeMode();
             } else {
                 stopRealtimeMode();
             }
         }
         
         private void startRealtimeMode() {
             if (transcriptionService != null) {
                 isRealtimeMode = true;
                 llRealtimeStatus.setVisibility(View.VISIBLE);
                 btnRealtimeMode.setText("停止实时模式");
                 
                 transcriptionService.startRealTimeTranscription(new TranscriptionCallback() {
                     @Override
                     public void onPartialResult(String partialText) {
                         runOnUiThread(() -> displayPartialResult(partialText));
                     }
                     
                     @Override
                     public void onTranscriptionCompleted(TranscriptionResult result) {
                         runOnUiThread(() -> appendTranscriptionResult(result.getText()));
                     }
                     
                     @Override
                     public void onTranscriptionError(TranscriptionError error) {
                         runOnUiThread(() -> showError(error.getMessage()));
                     }
                 });
             }
         }
         
         private void stopRealtimeMode() {
             if (transcriptionService != null && isRealtimeMode) {
                 transcriptionService.stopRealTimeTranscription();
                 isRealtimeMode = false;
                 llRealtimeStatus.setVisibility(View.GONE);
                 btnRealtimeMode.setText("实时模式");
             }
         }
         
         private void updateRecordingUI(boolean recording) {
             if (recording) {
                 fabRecord.setImageResource(R.drawable.ic_stop);
                 ivRecordingIndicator.setVisibility(View.VISIBLE);
                 // 添加录音动画
                 startRecordingAnimation();
             } else {
                 fabRecord.setImageResource(R.drawable.ic_mic);
                 ivRecordingIndicator.setVisibility(View.GONE);
                 stopRecordingAnimation();
             }
         }
         
         private void startRecordingTimer() {
             recordingStartTime = System.currentTimeMillis();
             recordingTimer = new Timer();
             recordingTimer.scheduleAtFixedRate(new TimerTask() {
                 @Override
                 public void run() {
                     long elapsed = System.currentTimeMillis() - recordingStartTime;
                     runOnUiThread(() -> updateRecordingTime(elapsed));
                 }
             }, 0, 1000);
         }
         
         private void stopRecordingTimer() {
             if (recordingTimer != null) {
                 recordingTimer.cancel();
                 recordingTimer = null;
             }
             tvRecordingTime.setText("00:00");
         }
         
         private void updateRecordingTime(long elapsedMs) {
             long seconds = elapsedMs / 1000;
             long minutes = seconds / 60;
             seconds = seconds % 60;
             
             String timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
             tvRecordingTime.setText(timeText);
         }
         
         private void displayTranscriptionResult(String text) {
             tvTranscriptionResult.setText(text);
         }
         
         private void displayPartialResult(String text) {
             // 显示部分结果，可以用不同颜色或样式
             SpannableString spannable = new SpannableString(text);
             spannable.setSpan(new ForegroundColorSpan(Color.GRAY), 0, text.length(), 
                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
             tvTranscriptionResult.setText(spannable);
         }
         
         private void appendTranscriptionResult(String text) {
             String currentText = tvTranscriptionResult.getText().toString();
             String newText = currentText + "\n" + text;
             tvTranscriptionResult.setText(newText);
         }
     }
     ```
  
  **验收标准**: 主界面功能完整，录音控制正常，转录结果正确显示
  
  _需求: 1.1, 1.2, 1.5, 2.1, 2.5_

- [ ] 6.2 实现历史记录界面
  
  **目标**: 创建转录历史管理界面，支持查看、编辑、删除和搜索功能
  
  **详细步骤**:
  1. **创建历史记录布局** (res/layout/activity_history.xml)
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical">
         
         <!-- 搜索栏 -->
         <com.google.android.material.textfield.TextInputLayout
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:layout_margin="16dp"
             style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">
             
             <com.google.android.material.textfield.TextInputEditText
                 android:id="@+id/et_search"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:hint="搜索转录记录..."
                 android:drawableStart="@drawable/ic_search"
                 android:drawablePadding="8dp" />
         </com.google.android.material.textfield.TextInputLayout>
         
         <!-- 转录记录列表 -->
         <androidx.recyclerview.widget.RecyclerView
             android:id="@+id/rv_transcriptions"
             android:layout_width="match_parent"
             android:layout_height="0dp"
             android:layout_weight="1"
             android:padding="8dp" />
         
         <!-- 空状态提示 -->
         <LinearLayout
             android:id="@+id/ll_empty_state"
             android:layout_width="match_parent"
             android:layout_height="0dp"
             android:layout_weight="1"
             android:orientation="vertical"
             android:gravity="center"
             android:visibility="gone">
             
             <ImageView
                 android:layout_width="64dp"
                 android:layout_height="64dp"
                 android:src="@drawable/ic_empty_history"
                 android:alpha="0.5" />
             
             <TextView
                 android:layout_width="wrap_content"
                 android:layout_height="wrap_content"
                 android:text="暂无转录记录"
                 android:textSize="16sp"
                 android:textColor="@color/text_secondary"
                 android:layout_marginTop="16dp" />
         </LinearLayout>
     </LinearLayout>
     ```
  
  2. **创建记录项布局** (res/layout/item_transcription.xml)
     ```xml
     <com.google.android.material.card.MaterialCardView 
         xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="wrap_content"
         android:layout_margin="4dp"
         android:clickable="true"
         android:focusable="true">
         
         <LinearLayout
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:orientation="vertical"
             android:padding="16dp">
             
             <!-- 转录文本 -->
             <TextView
                 android:id="@+id/tv_transcription_text"
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:textSize="14sp"
                 android:textColor="@color/text_primary"
                 android:maxLines="3"
                 android:ellipsize="end" />
             
             <!-- 元信息 -->
             <LinearLayout
                 android:layout_width="match_parent"
                 android:layout_height="wrap_content"
                 android:orientation="horizontal"
                 android:layout_marginTop="8dp">
                 
                 <TextView
                     android:id="@+id/tv_timestamp"
                     android:layout_width="0dp"
                     android:layout_height="wrap_content"
                     android:layout_weight="1"
                     android:textSize="12sp"
                     android:textColor="@color/text_secondary" />
                 
                 <TextView
                     android:id="@+id/tv_duration"
                     android:layout_width="wrap_content"
                     android:layout_height="wrap_content"
                     android:textSize="12sp"
                     android:textColor="@color/text_secondary"
                     android:layout_marginStart="8dp" />
                 
                 <ImageView
                     android:id="@+id/iv_realtime_indicator"
                     android:layout_width="16dp"
                     android:layout_height="16dp"
                     android:src="@drawable/ic_realtime"
                     android:layout_marginStart="8dp"
                     android:visibility="gone" />
             </LinearLayout>
         </LinearLayout>
     </com.google.android.material.card.MaterialCardView>
     ```
  
  3. **创建HistoryActivity类** (ui/history/HistoryActivity.java)
     ```java
     public class HistoryActivity extends AppCompatActivity {
         private RecyclerView rvTranscriptions;
         private TextInputEditText etSearch;
         private LinearLayout llEmptyState;
         
         private TranscriptionAdapter adapter;
         private TranscriptionRepository repository;
         private List<TranscriptionRecord> allTranscriptions = new ArrayList<>();
         
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_history);
             
             initViews();
             setupRecyclerView();
             setupSearch();
             loadTranscriptions();
         }
         
         private void initViews() {
             rvTranscriptions = findViewById(R.id.rv_transcriptions);
             etSearch = findViewById(R.id.et_search);
             llEmptyState = findViewById(R.id.ll_empty_state);
             
             repository = new TranscriptionRepository(getApplication());
         }
         
         private void setupRecyclerView() {
             adapter = new TranscriptionAdapter(new ArrayList<>());
             adapter.setOnItemClickListener(this::onTranscriptionClick);
             adapter.setOnItemLongClickListener(this::onTranscriptionLongClick);
             
             rvTranscriptions.setLayoutManager(new LinearLayoutManager(this));
             rvTranscriptions.setAdapter(adapter);
         }
         
         private void setupSearch() {
             etSearch.addTextChangedListener(new TextWatcher() {
                 @Override
                 public void afterTextChanged(Editable s) {
                     filterTranscriptions(s.toString());
                 }
             });
         }
         
         private void loadTranscriptions() {
             repository.getAllTranscriptions(new RepositoryCallback<List<TranscriptionRecord>>() {
                 @Override
                 public void onSuccess(List<TranscriptionRecord> records) {
                     runOnUiThread(() -> {
                         allTranscriptions.clear();
                         allTranscriptions.addAll(records);
                         adapter.updateData(records);
                         updateEmptyState(records.isEmpty());
                     });
                 }
                 
                 @Override
                 public void onError(Exception error) {
                     runOnUiThread(() -> {
                         Toast.makeText(HistoryActivity.this, 
                             "加载历史记录失败: " + error.getMessage(), 
                             Toast.LENGTH_SHORT).show();
                     });
                 }
             });
         }
         
         private void filterTranscriptions(String query) {
             if (query.trim().isEmpty()) {
                 adapter.updateData(allTranscriptions);
             } else {
                 repository.searchTranscriptions(query, new RepositoryCallback<List<TranscriptionRecord>>() {
                     @Override
                     public void onSuccess(List<TranscriptionRecord> records) {
                         runOnUiThread(() -> adapter.updateData(records));
                     }
                     
                     @Override
                     public void onError(Exception error) {
                         // 搜索失败时显示所有记录
                         runOnUiThread(() -> adapter.updateData(allTranscriptions));
                     }
                 });
             }
         }
         
         private void onTranscriptionClick(TranscriptionRecord record) {
             // 打开详情页面进行编辑
             Intent intent = new Intent(this, TranscriptionDetailActivity.class);
             intent.putExtra("transcription_id", record.getId());
             startActivity(intent);
         }
         
         private void onTranscriptionLongClick(TranscriptionRecord record) {
             // 显示操作菜单
             showTranscriptionMenu(record);
         }
         
         private void showTranscriptionMenu(TranscriptionRecord record) {
             String[] options = {"编辑", "分享", "删除"};
             
             new AlertDialog.Builder(this)
                 .setTitle("选择操作")
                 .setItems(options, (dialog, which) -> {
                     switch (which) {
                         case 0: // 编辑
                             onTranscriptionClick(record);
                             break;
                         case 1: // 分享
                             shareTranscription(record);
                             break;
                         case 2: // 删除
                             confirmDeleteTranscription(record);
                             break;
                     }
                 })
                 .show();
         }
         
         private void shareTranscription(TranscriptionRecord record) {
             Intent shareIntent = new Intent(Intent.ACTION_SEND);
             shareIntent.setType("text/plain");
             shareIntent.putExtra(Intent.EXTRA_TEXT, record.getEditedText());
             shareIntent.putExtra(Intent.EXTRA_SUBJECT, "转录内容分享");
             
             startActivity(Intent.createChooser(shareIntent, "分享转录内容"));
         }
         
         private void confirmDeleteTranscription(TranscriptionRecord record) {
             new AlertDialog.Builder(this)
                 .setTitle("确认删除")
                 .setMessage("确定要删除这条转录记录吗？")
                 .setPositiveButton("删除", (dialog, which) -> deleteTranscription(record))
                 .setNegativeButton("取消", null)
                 .show();
         }
         
         private void deleteTranscription(TranscriptionRecord record) {
             repository.deleteTranscription(record.getId(), new RepositoryCallback<Void>() {
                 @Override
                 public void onSuccess(Void result) {
                     runOnUiThread(() -> {
                         allTranscriptions.remove(record);
                         adapter.removeItem(record);
                         updateEmptyState(allTranscriptions.isEmpty());
                         Toast.makeText(HistoryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                     });
                 }
                 
                 @Override
                 public void onError(Exception error) {
                     runOnUiThread(() -> {
                         Toast.makeText(HistoryActivity.this, 
                             "删除失败: " + error.getMessage(), 
                             Toast.LENGTH_SHORT).show();
                     });
                 }
             });
         }
         
         private void updateEmptyState(boolean isEmpty) {
             if (isEmpty) {
                 rvTranscriptions.setVisibility(View.GONE);
                 llEmptyState.setVisibility(View.VISIBLE);
             } else {
                 rvTranscriptions.setVisibility(View.VISIBLE);
                 llEmptyState.setVisibility(View.GONE);
             }
         }
     }
     ```
  
  **验收标准**: 历史记录界面功能完整，支持搜索、编辑、删除操作
  
  _需求: 4.2, 4.3, 4.4, 4.5_

- [ ] 6.3 实现设置界面
  
  **目标**: 创建应用设置页面，管理离线模式和其他配置选项
  
  **详细步骤**:
  1. **创建设置界面** (ui/settings/SettingsActivity.java)
     ```java
     public class SettingsActivity extends AppCompatActivity {
         private Switch switchOfflineMode;
         private TextView tvModelStatus;
         private Button btnDownloadModel;
         private ProgressBar progressModelDownload;
         
         private OfflineModeManager offlineModeManager;
         
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_settings);
             
             initViews();
             setupClickListeners();
             updateUI();
         }
         
         private void initViews() {
             switchOfflineMode = findViewById(R.id.switch_offline_mode);
             tvModelStatus = findViewById(R.id.tv_model_status);
             btnDownloadModel = findViewById(R.id.btn_download_model);
             progressModelDownload = findViewById(R.id.progress_model_download);
             
             offlineModeManager = new OfflineModeManager(this);
         }
         
         private void setupClickListeners() {
             switchOfflineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                 if (isChecked) {
                     enableOfflineMode();
                 } else {
                     disableOfflineMode();
                 }
             });
             
             btnDownloadModel.setOnClickListener(v -> downloadModel());
         }
         
         private void updateUI() {
             boolean isOfflineAvailable = offlineModeManager.isOfflineModeAvailable();
             boolean isOfflineEnabled = offlineModeManager.isOfflineModeEnabled();
             
             switchOfflineMode.setChecked(isOfflineEnabled);
             switchOfflineMode.setEnabled(isOfflineAvailable);
             
             if (isOfflineAvailable) {
                 tvModelStatus.setText("离线模型已就绪");
                 tvModelStatus.setTextColor(getColor(R.color.success_color));
                 btnDownloadModel.setText("重新下载模型");
             } else {
                 tvModelStatus.setText("离线模型未下载");
                 tvModelStatus.setTextColor(getColor(R.color.warning_color));
                 btnDownloadModel.setText("下载离线模型");
             }
         }
         
         private void downloadModel() {
             btnDownloadModel.setEnabled(false);
             progressModelDownload.setVisibility(View.VISIBLE);
             
             offlineModeManager.downloadModel(new ModelDownloadCallback() {
                 @Override
                 public void onDownloadStarted() {
                     runOnUiThread(() -> {
                         tvModelStatus.setText("正在下载模型...");
                         tvModelStatus.setTextColor(getColor(R.color.accent_color));
                     });
                 }
                 
                 @Override
                 public void onDownloadProgress(float progress) {
                     runOnUiThread(() -> {
                         int progressPercent = (int) (progress * 100);
                         tvModelStatus.setText("下载进度: " + progressPercent + "%");
                     });
                 }
                 
                 @Override
                 public void onDownloadCompleted(String modelPath) {
                     runOnUiThread(() -> {
                         btnDownloadModel.setEnabled(true);
                         progressModelDownload.setVisibility(View.GONE);
                         updateUI();
                         Toast.makeText(SettingsActivity.this, "模型下载完成", Toast.LENGTH_SHORT).show();
                     });
                 }
                 
                 @Override
                 public void onDownloadError(Exception error) {
                     runOnUiThread(() -> {
                         btnDownloadModel.setEnabled(true);
                         progressModelDownload.setVisibility(View.GONE);
                         tvModelStatus.setText("下载失败: " + error.getMessage());
                         tvModelStatus.setTextColor(getColor(R.color.error_color));
                     });
                 }
             });
         }
     }
     ```
  
  **验收标准**: 设置界面功能正常，离线模式管理工作正确
  
  _需求: 3.1, 3.2, 5.4_

- [ ] 6.4 实现权限管理和引导界面
  
  **目标**: 处理应用权限申请和用户引导流程
  
  **详细步骤**:
  1. **创建权限管理器** (utils/PermissionManager.java)
     ```java
     public class PermissionManager {
         private static final int REQUEST_RECORD_AUDIO = 1001;
         
         public static boolean hasRecordAudioPermission(Context context) {
             return ContextCompat.checkSelfPermission(context, 
                 Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
         }
         
         public static void requestRecordAudioPermission(Activity activity) {
             ActivityCompat.requestPermissions(activity,
                 new String[]{Manifest.permission.RECORD_AUDIO},
                 REQUEST_RECORD_AUDIO);
         }
         
         public static boolean shouldShowRationale(Activity activity) {
             return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                 Manifest.permission.RECORD_AUDIO);
         }
     }
     ```
  
  2. **创建引导界面** (ui/guide/GuideActivity.java)
     ```java
     public class GuideActivity extends AppCompatActivity {
         private ViewPager2 viewPager;
         private Button btnNext;
         private Button btnSkip;
         
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_guide);
             
             initViews();
             setupViewPager();
         }
         
         private void setupViewPager() {
             GuideAdapter adapter = new GuideAdapter(this);
             viewPager.setAdapter(adapter);
             
             viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                 @Override
                 public void onPageSelected(int position) {
                     if (position == adapter.getItemCount() - 1) {
                         btnNext.setText("开始使用");
                     } else {
                         btnNext.setText("下一步");
                     }
                 }
             });
         }
     }
     ```
  
  **验收标准**: 权限申请流程正常，用户引导界面友好
  
  _需求: 1.1, 5.1_

- [ ]* 6.5 编写UI测试
  
  **目标**: 验证用户界面的功能和交互流程
  
  **详细步骤**:
  1. 创建主界面交互测试
  2. 测试录音功能和状态显示
  3. 验证历史记录管理功能
  4. 测试设置界面的各项功能
  
  _需求: 1.1, 1.5, 4.2, 4.3_

- [ ] 7. 错误处理和性能优化

- [ ] 7.1 实现全局错误处理机制
  
  **目标**: 建立完善的错误处理体系，提供用户友好的错误反馈
  
  **详细步骤**:
  1. **创建全局错误处理器** (utils/ErrorHandler.java)
     ```java
     public class ErrorHandler {
         private static final String TAG = "ErrorHandler";
         private Context context;
         
         public ErrorHandler(Context context) {
             this.context = context;
         }
         
         public void handleTranscriptionError(TranscriptionError error, ErrorCallback callback) {
             Log.e(TAG, "Transcription error: " + error.getMessage());
             
             switch (error) {
                 case MODEL_NOT_LOADED:
                     handleModelNotLoadedError(callback);
                     break;
                 case PERMISSION_DENIED:
                     handlePermissionDeniedError(callback);
                     break;
                 case INSUFFICIENT_STORAGE:
                     handleInsufficientStorageError(callback);
                     break;
                 case NETWORK_ERROR:
                     handleNetworkError(callback);
                     break;
                 case MODEL_CORRUPTED:
                     handleModelCorruptedError(callback);
                     break;
                 default:
                     handleGenericError(error, callback);
             }
         }
         
         private void handleModelNotLoadedError(ErrorCallback callback) {
             showErrorDialog(
                 "模型未加载",
                 "语音识别模型未正确加载，请检查离线模式设置或重新下载模型。",
                 "前往设置",
                 () -> {
                     Intent intent = new Intent(context, SettingsActivity.class);
                     context.startActivity(intent);
                     callback.onErrorHandled();
                 },
                 "取消",
                 callback::onErrorHandled
             );
         }
         
         private void handlePermissionDeniedError(ErrorCallback callback) {
             showErrorDialog(
                 "权限不足",
                 "应用需要麦克风权限才能进行语音录制，请在设置中授予权限。",
                 "前往设置",
                 () -> {
                     Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                     Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                     intent.setData(uri);
                     context.startActivity(intent);
                     callback.onErrorHandled();
                 },
                 "取消",
                 callback::onErrorHandled
             );
         }
         
         private void handleInsufficientStorageError(ErrorCallback callback) {
             showErrorDialog(
                 "存储空间不足",
                 "设备存储空间不足，请清理一些文件后重试。",
                 "清理历史记录",
                 () -> {
                     Intent intent = new Intent(context, HistoryActivity.class);
                     intent.putExtra("show_cleanup_dialog", true);
                     context.startActivity(intent);
                     callback.onErrorHandled();
                 },
                 "取消",
                 callback::onErrorHandled
             );
         }
         
         private void handleNetworkError(ErrorCallback callback) {
             showErrorDialog(
                 "网络连接问题",
                 "网络连接不稳定，已自动切换到离线模式。如需在线功能，请检查网络连接。",
                 "重试",
                 () -> {
                     // 重试网络连接
                     callback.onRetryRequested();
                 },
                 "继续离线使用",
                 callback::onErrorHandled
             );
         }
         
         private void handleModelCorruptedError(ErrorCallback callback) {
             showErrorDialog(
                 "模型文件损坏",
                 "语音识别模型文件可能已损坏，建议重新下载。",
                 "重新下载",
                 () -> {
                     Intent intent = new Intent(context, SettingsActivity.class);
                     intent.putExtra("auto_download_model", true);
                     context.startActivity(intent);
                     callback.onErrorHandled();
                 },
                 "取消",
                 callback::onErrorHandled
             );
         }
         
         private void handleGenericError(TranscriptionError error, ErrorCallback callback) {
             showErrorDialog(
                 "操作失败",
                 "发生了未知错误：" + error.getMessage() + "\n请稍后重试。",
                 "重试",
                 callback::onRetryRequested,
                 "取消",
                 callback::onErrorHandled
             );
         }
         
         private void showErrorDialog(String title, String message, 
                                    String positiveText, Runnable positiveAction,
                                    String negativeText, Runnable negativeAction) {
             
             if (context instanceof Activity) {
                 Activity activity = (Activity) context;
                 
                 new AlertDialog.Builder(activity)
                     .setTitle(title)
                     .setMessage(message)
                     .setPositiveButton(positiveText, (dialog, which) -> positiveAction.run())
                     .setNegativeButton(negativeText, (dialog, which) -> negativeAction.run())
                     .setCancelable(false)
                     .show();
             }
         }
         
         public interface ErrorCallback {
             void onErrorHandled();
             void onRetryRequested();
         }
     }
     ```
  
  2. **创建崩溃日志收集器** (utils/CrashReporter.java)
     ```java
     public class CrashReporter implements Thread.UncaughtExceptionHandler {
         private static final String TAG = "CrashReporter";
         private Context context;
         private Thread.UncaughtExceptionHandler defaultHandler;
         
         public CrashReporter(Context context) {
             this.context = context;
             this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
         }
         
         @Override
         public void uncaughtException(Thread thread, Throwable throwable) {
             try {
                 // 记录崩溃信息
                 logCrash(throwable);
                 
                 // 保存崩溃报告到本地
                 saveCrashReport(throwable);
                 
             } catch (Exception e) {
                 Log.e(TAG, "Error while handling crash", e);
             } finally {
                 // 调用默认处理器
                 if (defaultHandler != null) {
                     defaultHandler.uncaughtException(thread, throwable);
                 }
             }
         }
         
         private void logCrash(Throwable throwable) {
             Log.e(TAG, "Application crashed", throwable);
         }
         
         private void saveCrashReport(Throwable throwable) {
             try {
                 File crashDir = new File(context.getFilesDir(), "crashes");
                 if (!crashDir.exists()) {
                     crashDir.mkdirs();
                 }
                 
                 String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", 
                     Locale.getDefault()).format(new Date());
                 File crashFile = new File(crashDir, "crash_" + timestamp + ".txt");
                 
                 FileWriter writer = new FileWriter(crashFile);
                 writer.write("Crash Report\n");
                 writer.write("Timestamp: " + new Date().toString() + "\n");
                 writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
                 writer.write("Android Version: " + Build.VERSION.RELEASE + "\n");
                 writer.write("App Version: " + getAppVersion() + "\n\n");
                 
                 writer.write("Stack Trace:\n");
                 StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw);
                 throwable.printStackTrace(pw);
                 writer.write(sw.toString());
                 
                 writer.close();
                 
             } catch (IOException e) {
                 Log.e(TAG, "Failed to save crash report", e);
             }
         }
         
         private String getAppVersion() {
             try {
                 PackageInfo packageInfo = context.getPackageManager()
                     .getPackageInfo(context.getPackageName(), 0);
                 return packageInfo.versionName;
             } catch (PackageManager.NameNotFoundException e) {
                 return "Unknown";
             }
         }
     }
     ```
  
  **验收标准**: 错误处理机制完善，用户能够获得清晰的错误提示和解决方案
  
  _需求: 1.4, 2.4, 3.4, 5.2_

- [ ] 7.2 实现性能监控和优化
  
  **目标**: 监控应用性能，优化内存使用和电池消耗
  
  **详细步骤**:
  1. **创建性能监控器** (utils/PerformanceMonitor.java)
     ```java
     public class PerformanceMonitor {
         private static final String TAG = "PerformanceMonitor";
         
         private long transcriptionStartTime;
         private Runtime runtime;
         private ActivityManager activityManager;
         
         public PerformanceMonitor(Context context) {
             runtime = Runtime.getRuntime();
             activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
         }
         
         public void startTranscriptionTiming() {
             transcriptionStartTime = System.currentTimeMillis();
         }
         
         public long endTranscriptionTiming() {
             long duration = System.currentTimeMillis() - transcriptionStartTime;
             Log.i(TAG, "Transcription completed in: " + duration + "ms");
             return duration;
         }
         
         public MemoryInfo getMemoryUsage() {
             MemoryInfo memoryInfo = new MemoryInfo();
             
             // 获取应用内存使用情况
             long usedMemory = runtime.totalMemory() - runtime.freeMemory();
             long maxMemory = runtime.maxMemory();
             
             memoryInfo.usedMemoryMB = usedMemory / (1024 * 1024);
             memoryInfo.maxMemoryMB = maxMemory / (1024 * 1024);
             memoryInfo.usagePercentage = (float) usedMemory / maxMemory * 100;
             
             // 获取系统内存信息
             ActivityManager.MemoryInfo systemMemInfo = new ActivityManager.MemoryInfo();
             activityManager.getMemoryInfo(systemMemInfo);
             memoryInfo.availableSystemMemoryMB = systemMemInfo.availMem / (1024 * 1024);
             
             Log.i(TAG, String.format("Memory usage: %.1fMB/%.1fMB (%.1f%%)", 
                 memoryInfo.usedMemoryMB, memoryInfo.maxMemoryMB, memoryInfo.usagePercentage));
             
             return memoryInfo;
         }
         
         public void logCpuUsage() {
             // 获取CPU使用情况（简化版本）
             try {
                 RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
                 String load = reader.readLine();
                 reader.close();
                 
                 String[] toks = load.split(" +");
                 long idle1 = Long.parseLong(toks[4]);
                 long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + 
                            Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + 
                            Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                 
                 // 等待一段时间后再次测量
                 Thread.sleep(360);
                 
                 reader = new RandomAccessFile("/proc/stat", "r");
                 load = reader.readLine();
                 reader.close();
                 
                 toks = load.split(" +");
                 long idle2 = Long.parseLong(toks[4]);
                 long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + 
                            Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + 
                            Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                 
                 float cpuUsage = (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)) * 100;
                 Log.i(TAG, "CPU usage: " + cpuUsage + "%");
                 
             } catch (Exception e) {
                 Log.e(TAG, "Failed to get CPU usage", e);
             }
         }
         
         public static class MemoryInfo {
             public long usedMemoryMB;
             public long maxMemoryMB;
             public long availableSystemMemoryMB;
             public float usagePercentage;
         }
     }
     ```
  
  2. **创建电池优化管理器** (utils/BatteryOptimizer.java)
     ```java
     public class BatteryOptimizer {
         private static final String TAG = "BatteryOptimizer";
         
         private Context context;
         private PowerManager powerManager;
         private WakeLock wakeLock;
         
         public BatteryOptimizer(Context context) {
             this.context = context;
             this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
         }
         
         public void optimizeForRecording() {
             // 在录音时获取部分唤醒锁，防止CPU休眠
             if (wakeLock == null) {
                 wakeLock = powerManager.newWakeLock(
                     PowerManager.PARTIAL_WAKE_LOCK, 
                     "CantoneseVoice::RecordingWakeLock"
                 );
             }
             
             if (!wakeLock.isHeld()) {
                 wakeLock.acquire(10 * 60 * 1000L); // 最多10分钟
                 Log.i(TAG, "Acquired wake lock for recording");
             }
         }
         
         public void releaseOptimization() {
             if (wakeLock != null && wakeLock.isHeld()) {
                 wakeLock.release();
                 Log.i(TAG, "Released wake lock");
             }
         }
         
         public boolean isIgnoringBatteryOptimizations() {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
             }
             return true;
         }
         
         public void requestIgnoreBatteryOptimizations(Activity activity) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 if (!isIgnoringBatteryOptimizations()) {
                     Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                     intent.setData(Uri.parse("package:" + context.getPackageName()));
                     activity.startActivity(intent);
                 }
             }
         }
     }
     ```
  
  3. **实现音频处理优化** (audio/AudioOptimizer.java)
     ```java
     public class AudioOptimizer {
         private static final String TAG = "AudioOptimizer";
         
         // 优化音频缓冲区大小
         public static int getOptimalBufferSize(int sampleRate, int channelConfig, int audioFormat) {
             int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
             
             // 使用2-4倍的最小缓冲区大小以减少音频丢失
             int optimalBufferSize = minBufferSize * 3;
             
             Log.i(TAG, "Optimal buffer size: " + optimalBufferSize + " bytes");
             return optimalBufferSize;
         }
         
         // 优化音频处理线程优先级
         public static void optimizeAudioThread() {
             Thread currentThread = Thread.currentThread();
             currentThread.setPriority(Thread.MAX_PRIORITY);
             
             // 设置线程为音频线程（如果支持）
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                 try {
                     Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                     Log.i(TAG, "Set audio thread priority");
                 } catch (Exception e) {
                     Log.w(TAG, "Failed to set audio thread priority", e);
                 }
             }
         }
         
         // 检测并处理音频焦点
         public static void requestAudioFocus(Context context, AudioManager.OnAudioFocusChangeListener listener) {
             AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
             
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                     .setAudioAttributes(new AudioAttributes.Builder()
                         .setUsage(AudioAttributes.USAGE_MEDIA)
                         .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                         .build())
                     .setOnAudioFocusChangeListener(listener)
                     .build();
                 
                 int result = audioManager.requestAudioFocus(focusRequest);
                 Log.i(TAG, "Audio focus request result: " + result);
             } else {
                 int result = audioManager.requestAudioFocus(listener, 
                     AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                 Log.i(TAG, "Audio focus request result: " + result);
             }
         }
     }
     ```
  
  **验收标准**: 性能监控正常工作，应用内存和电池使用得到优化
  
  _需求: 5.1, 5.2, 5.5_

- [ ]* 7.3 编写性能测试
  
  **目标**: 验证应用性能指标，确保满足需求
  
  **详细步骤**:
  1. 创建转录速度基准测试
  2. 测试内存使用情况和泄漏检测
  3. 验证电池消耗水平
  4. 测试不同设备上的性能表现
  
  _需求: 5.1, 5.2, 5.5_

- [ ] 8. 应用集成和最终测试

- [ ] 8.1 集成所有模块并进行端到端测试
  
  **目标**: 整合所有功能模块，验证完整的用户流程
  
  **详细步骤**:
  1. **创建应用入口类** (CantoneseVoiceApplication.java)
     ```java
     public class CantoneseVoiceApplication extends Application {
         
         @Override
         public void onCreate() {
             super.onCreate();
             
             // 初始化崩溃报告
             Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(this));
             
             // 初始化离线模式管理器
             OfflineModeManager offlineManager = new OfflineModeManager(this);
             if (!offlineManager.isOfflineModeAvailable()) {
                 // 首次启动时提示下载模型
                 SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                 if (!prefs.getBoolean("model_download_prompted", false)) {
                     // 标记已提示过
                     prefs.edit().putBoolean("model_download_prompted", true).apply();
                 }
             }
             
             Log.i("CantoneseVoiceApp", "Application initialized");
         }
     }
     ```
  
  2. **创建集成测试用例** (test/IntegrationTest.java)
     ```java
     @RunWith(AndroidJUnit4.class)
     public class IntegrationTest {
         
         @Test
         public void testCompleteTranscriptionFlow() {
             // 测试完整的转录流程：录音 -> 转录 -> 保存
             
             // 1. 初始化组件
             Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
             WhisperEngine engine = new WhisperEngine();
             TranscriptionRepository repository = new TranscriptionRepository((Application) context.getApplicationContext());
             
             // 2. 加载模型
             OfflineModeManager offlineManager = new OfflineModeManager(context);
             assertTrue("Offline model should be available", offlineManager.isOfflineModeAvailable());
             
             boolean modelLoaded = engine.initializeModel(offlineManager.getModelPath());
             assertTrue("Model should load successfully", modelLoaded);
             
             // 3. 创建测试音频数据
             byte[] testAudioData = createTestAudioData();
             AudioData audioData = AudioProcessor.convertToWhisperFormat(testAudioData, 16000);
             
             // 4. 执行转录
             CountDownLatch latch = new CountDownLatch(1);
             AtomicReference<TranscriptionResult> resultRef = new AtomicReference<>();
             
             engine.transcribe(audioData, new TranscriptionCallback() {
                 @Override
                 public void onTranscriptionCompleted(TranscriptionResult result) {
                     resultRef.set(result);
                     latch.countDown();
                 }
                 
                 @Override
                 public void onTranscriptionError(TranscriptionError error) {
                     fail("Transcription should not fail: " + error.getMessage());
                     latch.countDown();
                 }
             });
             
             // 5. 等待转录完成
             try {
                 assertTrue("Transcription should complete within 10 seconds", 
                     latch.await(10, TimeUnit.SECONDS));
             } catch (InterruptedException e) {
                 fail("Test interrupted");
             }
             
             // 6. 验证结果
             TranscriptionResult result = resultRef.get();
             assertNotNull("Transcription result should not be null", result);
             assertFalse("Transcription text should not be empty", result.getText().isEmpty());
             assertTrue("Processing time should be reasonable", result.getProcessingTime() < 5000);
             
             // 7. 保存到数据库
             TranscriptionRecord record = TranscriptionProcessor.createTranscriptionRecord(
                 result, false, null);
             
             CountDownLatch saveLatch = new CountDownLatch(1);
             AtomicLong savedIdRef = new AtomicLong(-1);
             
             repository.saveTranscription(record, new RepositoryCallback<Long>() {
                 @Override
                 public void onSuccess(Long id) {
                     savedIdRef.set(id);
                     saveLatch.countDown();
                 }
                 
                 @Override
                 public void onError(Exception error) {
                     fail("Save should not fail: " + error.getMessage());
                     saveLatch.countDown();
                 }
             });
             
             try {
                 assertTrue("Save should complete within 5 seconds", 
                     saveLatch.await(5, TimeUnit.SECONDS));
             } catch (InterruptedException e) {
                 fail("Test interrupted");
             }
             
             // 8. 验证保存结果
             assertTrue("Record should be saved with valid ID", savedIdRef.get() > 0);
             
             // 清理
             engine.releaseModel();
         }
         
         private byte[] createTestAudioData() {
             // 创建1秒的16kHz 16-bit单声道测试音频
             int sampleRate = 16000;
             int duration = 1; // 1秒
             int samples = sampleRate * duration;
             byte[] audioData = new byte[samples * 2]; // 16-bit = 2 bytes per sample
             
             // 生成简单的正弦波测试音频
             for (int i = 0; i < samples; i++) {
                 double angle = 2.0 * Math.PI * i * 440.0 / sampleRate; // 440Hz音调
                 short sample = (short) (Math.sin(angle) * 16384); // 50%音量
                 
                 audioData[i * 2] = (byte) (sample & 0xFF);
                 audioData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
             }
             
             return audioData;
         }
     }
     ```
  
  **验收标准**: 所有模块正确集成，端到端流程测试通过
  
  _需求: 1.1, 1.3, 1.5, 2.1, 2.3, 3.3, 3.4_

- [ ] 8.2 进行兼容性和稳定性测试
  
  **目标**: 验证应用在不同环境下的兼容性和稳定性
  
  **详细步骤**:
  1. **创建兼容性测试矩阵**
     - Android 7.0 (API 24) - 最低支持版本
     - Android 8.0 (API 26) - 主流版本
     - Android 10.0 (API 29) - 中等版本
     - Android 12.0 (API 31) - 较新版本
     - Android 13.0 (API 33) - 最新版本
  
  2. **设备兼容性测试**
     - 低端设备：2GB RAM, 四核CPU
     - 中端设备：4GB RAM, 八核CPU
     - 高端设备：8GB+ RAM, 旗舰CPU
  
  3. **稳定性测试场景**
     ```java
     @Test
     public void testLongRunningStability() {
         // 测试长时间运行的稳定性
         for (int i = 0; i < 100; i++) {
             // 执行转录操作
             performTranscription();
             
             // 检查内存使用
             PerformanceMonitor monitor = new PerformanceMonitor(context);
             PerformanceMonitor.MemoryInfo memInfo = monitor.getMemoryUsage();
             
             // 确保内存使用不超过阈值
             assertTrue("Memory usage should not exceed 80%", 
                 memInfo.usagePercentage < 80.0f);
             
             // 短暂休息
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {
                 break;
             }
         }
     }
     ```
  
  **验收标准**: 应用在各种设备和Android版本上稳定运行
  
  _需求: 5.3, 5.4, 5.5_

- [ ] 8.3 优化用户体验和界面细节
  
  **目标**: 完善用户界面和交互体验，提升应用质量
  
  **详细步骤**:
  1. **界面优化清单**
     - 添加加载动画和过渡效果
     - 优化按钮点击反馈
     - 完善错误状态显示
     - 添加空状态插图
     - 优化深色模式支持
  
  2. **性能优化**
     - 减少应用启动时间到3秒以内
     - 优化界面响应速度
     - 减少内存占用
  
  3. **用户引导优化**
     - 完善首次使用引导
     - 添加功能提示和帮助
     - 优化权限申请流程
  
  **验收标准**: 用户界面美观流畅，交互体验良好
  
  _需求: 5.1, 5.5_

- [ ]* 8.4 编写集成测试和文档
  
  **目标**: 完善测试覆盖和项目文档
  
  **详细步骤**:
  1. **创建测试文档**
     - 单元测试报告
     - 集成测试用例
     - 性能测试结果
     - 兼容性测试矩阵
  
  2. **编写用户文档**
     - 用户使用手册
     - 功能说明文档
     - 常见问题解答
     - 故障排除指南
  
  3. **开发文档**
     - 代码架构说明
     - API接口文档
     - 部署和构建指南
     - 维护和更新说明
  
  _需求: 所有需求_

---

## 总结

本实施计划提供了完整的开发路线图，从项目初始化到最终测试的每个步骤都有详细的代码示例和实施指导。你可以：

- **按顺序执行**: 从任务1开始，逐步完成每个任务
- **并行开发**: 某些独立的任务可以同时进行
- **使用Kiro**: 点击任务旁的"开始任务"按钮让Kiro自动实施
- **自主开发**: 参考详细步骤手动实现每个功能

每个任务都包含了具体的代码实现、验收标准和需求映射，确保开发过程的可追溯性和质量保证。