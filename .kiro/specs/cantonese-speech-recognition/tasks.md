# 粤语语音识别Android应用 - 详细实施计划

## 概述

基于需求文档和设计文档，本实施计划将粤语语音识别Android应用的开发分解为具体的编码任务。每个任务都包含详细的实施步骤和代码示例，方便开发者手动创建和执行。项目采用增量式开发方式，确保每个阶段都能产出可测试的功能模块。

## 项目目标
- 开发支持粤语语音识别的Android应用
- 集成OpenAI Whisper粤语微调模型
- 支持实时转录和离线使用
- 提供历史记录管理功能
- 确保85%以上的识别准确率

## 实施任务

### 阶段一：项目基础设施搭建

- [ ] **任务1: 项目结构搭建和核心接口定义**
  - **目标**: 建立项目基础架构，定义核心数据模型和接口
  - **预计时间**: 2-3天
  - **依赖**: 无
  - **验收标准**: 项目能够成功编译，核心接口定义完整
  - _对应需求: 需求1.1, 1.2, 1.3, 5.3_

- [ ] **1.1 创建Android项目基础结构**
  - **实施步骤**:
    1. **创建新的Android项目**
       - 使用Android Studio创建新项目
       - 选择API Level 24 (Android 7.0)以上
       - 选择Java语言
    
    2. **配置build.gradle文件**
       ```gradle
       android {
           compileSdk 34
           defaultConfig {
               minSdk 24
               targetSdk 34
           }
           compileOptions {
               sourceCompatibility JavaVersion.VERSION_1_8
               targetCompatibility JavaVersion.VERSION_1_8
           }
       }
       
       dependencies {
           implementation 'androidx.appcompat:appcompat:1.6.1'
           implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
           implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.15.1'
           implementation 'androidx.room:room-runtime:2.5.0'
           implementation 'androidx.room:room-rxjava3:2.5.0'
           implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
           implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
           implementation 'com.squareup.okhttp3:okhttp:4.11.0'
           annotationProcessor 'androidx.room:room-compiler:2.5.0'
           
           // 测试依赖
           testImplementation 'junit:junit:4.13.2'
           testImplementation 'org.mockito:mockito-core:4.6.1'
           androidTestImplementation 'androidx.test.ext:junit:1.1.5'
           androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
       }
       ```
    
    3. **创建包结构**
       - 在`src/main/java/com/app/cantonesespeech/`下创建以下包：
       ```
       com.app.cantonesespeech/
       ├── audio/          // 音频录制和处理
       │   ├── AudioRecorderManager.java
       │   ├── AudioData.java
       │   └── AudioStreamListener.java
       ├── transcription/  // 语音转录核心
       │   ├── WhisperEngine.java
       │   ├── TranscriptionResult.java
       │   └── WordSegment.java
       ├── data/          // 数据访问层
       │   ├── TranscriptionRepository.java
       │   ├── TranscriptionRecord.java
       │   ├── TranscriptionDao.java
       │   └── AppDatabase.java
       ├── ui/            // 用户界面
       │   ├── MainActivity.java
       │   ├── RecordingFragment.java
       │   └── HistoryFragment.java
       ├── service/       // 后台服务
       │   └── TranscriptionService.java
       ├── native/        // JNI接口
       │   └── WhisperJNI.java
       ├── utils/         // 工具类
       │   ├── ErrorHandler.java
       │   ├── OfflineModeManager.java
       │   └── NetworkUtils.java
       └── callback/      // 回调接口
           └── TranscriptionCallback.java
       ```
    
    4. **配置AndroidManifest.xml**
       ```xml
       <uses-permission android:name="android.permission.RECORD_AUDIO" />
       <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
       <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
       <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
       <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
       <uses-permission android:name="android.permission.INTERNET" />
       
       <application
           android:allowBackup="true"
           android:icon="@mipmap/ic_launcher"
           android:label="@string/app_name"
           android:theme="@style/Theme.CantoneseRecognition">
           
           <service android:name=".service.TranscriptionService"
                   android:enabled="true"
                   android:exported="false" />
       </application>
       ```
  
  - **验收标准**: 项目结构创建完成，能够成功编译
  - **预计时间**: 0.5天

- [ ] **1.2 定义核心数据模型**
  - **实施步骤**:
    1. **创建TranscriptionRecord实体类** (`data/TranscriptionRecord.java`)
       ```java
       @Entity(tableName = "transcription_records")
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
           private int duration; // 录音时长(秒)
           
           @ColumnInfo(name = "confidence")
           private float confidence; // 识别置信度
           
           @ColumnInfo(name = "is_real_time")
           private boolean isRealTime; // 是否实时转录
           
           // 构造函数
           public TranscriptionRecord() {}
           
           public TranscriptionRecord(String originalText, long timestamp, 
                                    String audioFilePath, int duration, 
                                    float confidence, boolean isRealTime) {
               this.originalText = originalText;
               this.editedText = originalText; // 初始时编辑文本等于原始文本
               this.timestamp = timestamp;
               this.audioFilePath = audioFilePath;
               this.duration = duration;
               this.confidence = confidence;
               this.isRealTime = isRealTime;
           }
           
           // 完整的getter和setter方法
           // ... (省略具体实现)
       }
       ```
    
    2. **创建AudioData类** (`audio/AudioData.java`)
       ```java
       public class AudioData {
           private byte[] rawData;
           private int sampleRate = 16000; // 采样率
           private int channels = 1;       // 声道数
           private int bitDepth = 16;      // 位深度
           private long duration;          // 时长(毫秒)
           
           public AudioData(byte[] rawData, int sampleRate, int channels, 
                           int bitDepth, long duration) {
               this.rawData = rawData;
               this.sampleRate = sampleRate;
               this.channels = channels;
               this.bitDepth = bitDepth;
               this.duration = duration;
           }
           
           // PCM格式转换方法
           public byte[] toPCM16() {
               if (bitDepth == 16) {
                   return rawData; // 已经是16位PCM
               }
               // 实现8位到16位的转换逻辑
               byte[] pcm16Data = new byte[rawData.length * 2];
               for (int i = 0; i < rawData.length; i++) {
                   short sample = (short) ((rawData[i] & 0xFF) - 128);
                   sample *= 256; // 转换为16位
                   pcm16Data[i * 2] = (byte) (sample & 0xFF);
                   pcm16Data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
               }
               return pcm16Data;
           }
           
           // 获取音频时长(秒)
           public double getDurationInSeconds() {
               return (double) rawData.length / (sampleRate * channels * (bitDepth / 8));
           }
           
           // getter和setter方法
           // ... (省略具体实现)
       }
       ```
    
    3. **创建TranscriptionResult类** (`transcription/TranscriptionResult.java`)
       ```java
       public class TranscriptionResult {
           private String text;                    // 转录文本
           private float confidence;               // 整体置信度
           private long processingTime;            // 处理时间(毫秒)
           private List<WordSegment> segments;     // 词级别分段
           private boolean isComplete;             // 是否完整转录
           private long timestamp;                 // 转录时间戳
           
           public TranscriptionResult() {
               this.segments = new ArrayList<>();
               this.timestamp = System.currentTimeMillis();
           }
           
           public TranscriptionResult(String text, float confidence, 
                                    long processingTime, boolean isComplete) {
               this();
               this.text = text;
               this.confidence = confidence;
               this.processingTime = processingTime;
               this.isComplete = isComplete;
           }
           
           // 添加词段
           public void addSegment(WordSegment segment) {
               if (segments == null) {
                   segments = new ArrayList<>();
               }
               segments.add(segment);
           }
           
           // getter和setter方法
           // ... (省略具体实现)
       }
       ```
    
    4. **创建WordSegment类** (`transcription/WordSegment.java`)
       ```java
       public class WordSegment {
           private String word;        // 词语
           private float startTime;    // 开始时间(秒)
           private float endTime;      // 结束时间(秒)
           private float confidence;   // 词语置信度
           
           public WordSegment() {}
           
           public WordSegment(String word, float startTime, 
                            float endTime, float confidence) {
               this.word = word;
               this.startTime = startTime;
               this.endTime = endTime;
               this.confidence = confidence;
           }
           
           // 获取词语时长
           public float getDuration() {
               return endTime - startTime;
           }
           
           // getter和setter方法
           // ... (省略具体实现)
       }
       ```
  
  - **验收标准**: 所有数据模型类创建完成，包含完整的属性和方法
  - **预计时间**: 1天

- [ ] **1.3 定义核心接口和枚举**
  - **实施步骤**:
    1. **创建TranscriptionCallback接口** (`callback/TranscriptionCallback.java`)
       ```java
       public interface TranscriptionCallback {
           /**
            * 转录开始时调用
            */
           void onTranscriptionStarted();
           
           /**
            * 实时转录时返回部分结果
            * @param partialText 部分转录文本
            */
           void onPartialResult(String partialText);
           
           /**
            * 转录完成时调用
            * @param result 完整的转录结果
            */
           void onTranscriptionCompleted(TranscriptionResult result);
           
           /**
            * 转录出错时调用
            * @param error 错误信息
            */
           void onTranscriptionError(TranscriptionError error);
           
           /**
            * 转录进度更新
            * @param progress 进度百分比 (0.0 - 1.0)
            */
           void onProgressUpdate(float progress);
       }
       ```
    
    2. **创建AudioStreamListener接口** (`audio/AudioStreamListener.java`)
       ```java
       public interface AudioStreamListener {
           /**
            * 音频数据可用时调用
            * @param audioData 音频数据
            * @param length 数据长度
            */
           void onAudioDataAvailable(byte[] audioData, int length);
           
           /**
            * 录音开始时调用
            */
           void onRecordingStarted();
           
           /**
            * 录音停止时调用
            */
           void onRecordingStopped();
           
           /**
            * 录音出错时调用
            * @param error 音频错误信息
            */
           void onRecordingError(AudioError error);
           
           /**
            * 音量变化时调用
            * @param volume 音量级别 (0.0 - 1.0)
            */
           void onVolumeChanged(float volume);
       }
       ```
    
    3. **定义TranscriptionError枚举** (`utils/TranscriptionError.java`)
       ```java
       public enum TranscriptionError {
           MODEL_NOT_LOADED(1001, "语音模型未加载"),
           AUDIO_FORMAT_UNSUPPORTED(1002, "不支持的音频格式"),
           INSUFFICIENT_STORAGE(1003, "存储空间不足"),
           PERMISSION_DENIED(1004, "权限被拒绝"),
           NETWORK_ERROR(1005, "网络错误"),
           MODEL_CORRUPTED(1006, "模型文件损坏"),
           AUDIO_RECORDING_FAILED(1007, "音频录制失败"),
           TRANSCRIPTION_TIMEOUT(1008, "转录超时"),
           INITIALIZATION_FAILED(1009, "初始化失败"),
           GENERAL_ERROR(1999, "未知错误");
           
           private final int code;
           private final String message;
           
           TranscriptionError(int code, String message) {
               this.code = code;
               this.message = message;
           }
           
           public int getCode() {
               return code;
           }
           
           public String getMessage() {
               return message;
           }
           
           @Override
           public String toString() {
               return String.format("[%d] %s", code, message);
           }
       }
       ```
    
    4. **定义AudioError枚举** (`audio/AudioError.java`)
       ```java
       public enum AudioError {
           PERMISSION_DENIED(2001, "录音权限被拒绝"),
           DEVICE_BUSY(2002, "音频设备忙碌"),
           INITIALIZATION_FAILED(2003, "音频初始化失败"),
           RECORDING_FAILED(2004, "录音失败"),
           INVALID_FORMAT(2005, "无效的音频格式"),
           BUFFER_OVERFLOW(2006, "音频缓冲区溢出");
           
           private final int code;
           private final String message;
           
           AudioError(int code, String message) {
               this.code = code;
               this.message = message;
           }
           
           public int getCode() {
               return code;
           }
           
           public String getMessage() {
               return message;
           }
           
           @Override
           public String toString() {
               return String.format("[%d] %s", code, message);
           }
       }
       ```
  
  - **验收标准**: 所有核心接口和枚举定义完成，包含完整的方法签名和错误类型
  - **预计时间**: 0.5天

- [ ]* 1.4 编写数据模型单元测试
  - 为TranscriptionRecord类编写验证测试
   ```java
   @RunWith(JUnit4.class)
   public class TranscriptionRecordTest {
    
    @Test
    public void testTranscriptionRecordCreation() {
        // 测试转录记录创建
        String originalText = "你好世界";
        long timestamp = System.currentTimeMillis();
        String audioPath = "/path/to/audio.wav";
        int duration = 5;
        float confidence = 0.95f;
        boolean isRealTime = true;
        
        TranscriptionRecord record = new TranscriptionRecord(
                originalText, timestamp, audioPath, duration, confidence, isRealTime);
            
            // 验证所有属性设置正确
            assertEquals(originalText, record.getOriginalText());
            assertEquals(originalText, record.getEditedText()); // 初始时相等
            assertEquals(timestamp, record.getTimestamp());
            assertEquals(audioPath, record.getAudioFilePath());
            assertEquals(duration, record.getDuration());
            assertEquals(confidence, record.getConfidence(), 0.001f);
            assertTrue(record.isRealTime());
        }
        
        @Test
        public void testTextEditing() {
            // 测试文本编辑功能
            TranscriptionRecord record = new TranscriptionRecord();
            record.setOriginalText("原始文本");
            record.setEditedText("编辑后文本");
            
            assertEquals("原始文本", record.getOriginalText());
            assertEquals("编辑后文本", record.getEditedText());
        }
    }

   ```
  - 为AudioData类编写格式转换测试
   ```java
    @RunWith(JUnit4.class)
    public class AudioDataTest {
        
        @Test
        public void testAudioDataCreation() {
            // 测试音频数据创建
            byte[] testData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
            int sampleRate = 16000;
            int channels = 1;
            int bitDepth = 16;
            long duration = 1000;
            
            AudioData audioData = new AudioData(testData, sampleRate, 
                                            channels, bitDepth, duration);
            
            // 验证所有属性
            assertArrayEquals(testData, audioData.getRawData());
            assertEquals(sampleRate, audioData.getSampleRate());
            assertEquals(channels, audioData.getChannels());
            assertEquals(bitDepth, audioData.getBitDepth());
            assertEquals(duration, audioData.getDuration());
        }
        
        @Test
        public void testPCM16Conversion() {
            // 测试PCM16格式转换
            byte[] testData8bit = new byte[]{0, 64, 128, 192, 255};
            AudioData audioData = new AudioData(testData8bit, 16000, 1, 8, 1000);
            
            byte[] pcm16Data = audioData.toPCM16();
            assertNotNull(pcm16Data);
            assertEquals(testData8bit.length * 2, pcm16Data.length);
        }
        
        @Test
        public void testDurationCalculation() {
            // 测试时长计算
            byte[] testData = new byte[32000]; // 1秒的16位单声道数据
            AudioData audioData = new AudioData(testData, 16000, 1, 16, 1000);
            
            double duration = audioData.getDurationInSeconds();
            assertEquals(1.0, duration, 0.1);
        }
    }
   ```
  - 为TranscriptionResult类编写结果解析测试
   ```java
        @RunWith(JUnit4.class)
        public class TranscriptionResultTest {
            
            @Test
            public void testTranscriptionResultCreation() {
                // 测试转录结果创建
                String text = "测试转录文本";
                float confidence = 0.9f;
                long processingTime = 1500;
                boolean isComplete = true;
                
                TranscriptionResult result = new TranscriptionResult(
                    text, confidence, processingTime, isComplete);
                
                assertEquals(text, result.getText());
                assertEquals(confidence, result.getConfidence(), 0.001f);
                assertEquals(processingTime, result.getProcessingTime());
                assertTrue(result.isComplete());
                assertNotNull(result.getSegments());
            }
            
            @Test
            public void testSegmentManagement() {
                // 测试词段管理
                TranscriptionResult result = new TranscriptionResult();
                
                WordSegment segment1 = new WordSegment("你好", 0.0f, 0.5f, 0.95f);
                WordSegment segment2 = new WordSegment("世界", 0.5f, 1.0f, 0.90f);
                
                result.addSegment(segment1);
                result.addSegment(segment2);
                
                assertEquals(2, result.getSegments().size());
                assertEquals("你好", result.getSegments().get(0).getWord());
                assertEquals("世界", result.getSegments().get(1).getWord());
            }
        }
     ```

  - _需求: 1.3, 1.5_

- [ ] 2. 音频录制和处理模块实现
  - 实现AudioRecorderManager类，处理音频录制
  - 集成音频格式转换和预处理功能
  - 实现实时音频流处理
  - _需求: 1.1, 1.2, 2.1, 2.2_

- [-] 2.1 实现AudioRecorderManager核心功能

  - 编写startRecording()方法：
    ```java
    public void startRecording() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, 
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, 
            AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();
        isRecording = true;
        startAudioThread();
    }
    ```
  - 实现stopRecording()、pauseRecording()方法，包含状态管理
  - 实现权限检查：使用ContextCompat.checkSelfPermission()
  - 创建音频录制线程，持续读取音频数据
  - _需求: 1.1, 1.2_

- [ ] 2.2 实现音频数据处理
  - 编写getAudioData()方法：
    ```java
    public AudioData getAudioData() {
        if (audioBuffer == null || audioBuffer.isEmpty()) {
            return null;
        }
        byte[] audioBytes = new byte[audioBuffer.size()];
        for (int i = 0; i < audioBuffer.size(); i++) {
            audioBytes[i] = audioBuffer.get(i);
        }
        return new AudioData(audioBytes, SAMPLE_RATE, 1, 16, 
                            (long)(audioBytes.length / (SAMPLE_RATE * 2.0) * 1000));
    }
    ```
  - 实现setAudioFormat()方法：
    ```java
    public void setAudioFormat(int sampleRate, int channels, int bitDepth) {
        this.SAMPLE_RATE = sampleRate;
        this.channels = channels;
        this.bitDepth = bitDepth;
        // 重新计算缓冲区大小
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, 
            channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
            bitDepth == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
    }
    ```
  - 实现音频质量检测和噪音过滤算法
  - _需求: 1.1, 1.4, 2.3_

- [ ] 2.3 实现实时音频流处理
  - 创建音频数据缓冲区管理
  - 实现AudioStreamListener回调机制
  - 添加VAD（语音活动检测）功能
  - 实现音频数据分块处理逻辑
  - _需求: 2.1, 2.2, 2.3_

- [ ]* 2.4 编写音频模块单元测试
  - 测试AudioRecorderManager的录制功能
  - 测试音频格式转换的准确性
  - 测试实时音频流的数据完整性
  - _需求: 1.1, 2.1, 2.2_

- [ ] 3. Whisper模型集成和JNI接口
  - 创建WhisperEngine类，封装模型调用
  - 实现JNI接口，连接Java和C++代码
  - 集成ONNX Runtime，加载Whisper模型
  - _需求: 1.3, 1.4, 3.1, 3.3_

- [ ] 3.1 创建JNI接口层
  - 编写native方法声明：
    ```java
    public class WhisperJNI {
        static {
            System.loadLibrary("whisper-jni");
        }
        
        public native boolean initModel(String modelPath);
        public native String transcribe(byte[] audioData, int length);
        public native String transcribeRealTime(byte[] audioData, int length);
        public native void releaseModel();
        public native boolean isModelLoaded();
    }
    ```
  - 创建对应的C++实现文件whisper-jni.cpp：
    ```cpp
    extern "C" JNIEXPORT jboolean JNICALL
    Java_com_app_cantonesespeech_native_WhisperJNI_initModel(
        JNIEnv *env, jobject thiz, jstring model_path) {
        // 实现模型初始化逻辑
    }
    ```
  - 实现音频数据的jbyteArray到C++数组的转换
  - 添加异常处理和内存清理逻辑
  - _需求: 1.4, 3.3_

- [ ] 3.2 实现WhisperEngine核心功能
  - 编写initializeModel()方法：
    ```java
    public boolean initializeModel(String modelPath) {
        try {
            if (whisperJNI == null) {
                whisperJNI = new WhisperJNI();
            }
            boolean success = whisperJNI.initModel(modelPath);
            if (success) {
                this.modelPath = modelPath;
                isModelInitialized = true;
                Log.i(TAG, "Whisper模型初始化成功: " + modelPath);
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "模型初始化失败", e);
            return false;
        }
    }
    ```
  - 实现transcribe()方法：
    ```java
    public TranscriptionResult transcribe(AudioData audioData) {
        if (!isModelLoaded()) {
            throw new IllegalStateException("模型未加载");
        }
        long startTime = System.currentTimeMillis();
        String result = whisperJNI.transcribe(audioData.getRawData(), audioData.getRawData().length);
        long processingTime = System.currentTimeMillis() - startTime;
        
        return new TranscriptionResult(result, 0.9f, processingTime, parseWordSegments(result), true);
    }
    ```
  - 实现transcribeRealTime()和模型管理方法
  - _需求: 1.3, 1.4, 2.3, 3.3_

- [ ] 3.3 集成ONNX Runtime
  - 配置ONNX Runtime Android依赖
  - 实现模型文件加载和验证逻辑
  - 添加模型推理的错误处理机制
  - 实现GPU加速支持（如果设备支持）
  - _需求: 1.4, 3.1, 3.3, 5.2_

- [ ]* 3.4 编写Whisper引擎单元测试
  - 测试模型加载和释放功能
  - 测试音频转录的准确性
  - 测试错误情况的处理逻辑
  - _需求: 1.4, 3.3_

- [ ] 4. 转录服务和业务逻辑实现
  - 创建TranscriptionService后台服务
  - 实现转录业务逻辑和状态管理
  - 集成实时转录和批量转录功能
  - _需求: 1.3, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 4.1 实现TranscriptionService基础框架
  - 创建Service类：
    ```java
    public class TranscriptionService extends Service {
        private final IBinder binder = new TranscriptionBinder();
        private NotificationManager notificationManager;
        private static final int NOTIFICATION_ID = 1001;
        
        public class TranscriptionBinder extends Binder {
            TranscriptionService getService() {
                return TranscriptionService.this;
            }
        }
        
        @Override
        public IBinder onBind(Intent intent) {
            return binder;
        }
    }
    ```
  - 实现服务生命周期：
    ```java
    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    ```
  - 创建前台服务通知，显示"正在转录..."状态
  - 实现服务与Activity的双向通信机制
  - _需求: 1.5, 2.1, 2.5_

- [ ] 4.2 实现转录业务逻辑
  - 编写startRealTimeTranscription()方法，启动实时转录
  - 实现stopRealTimeTranscription()方法，停止转录服务
  - 编写processAudioFile()方法，处理音频文件转录
  - 实现setTranscriptionCallback()方法，设置结果回调
  - _需求: 1.3, 1.5, 2.1, 2.3, 2.4_

- [ ] 4.3 实现转录状态管理
  - 创建转录状态枚举和状态机
  - 实现转录进度跟踪和报告
  - 添加转录任务队列管理
  - 实现转录结果缓存机制
  - _需求: 1.2, 2.2, 2.4_

- [ ]* 4.4 编写转录服务单元测试
  - 测试服务启动和停止功能
  - 测试实时转录的状态管理
  - 测试转录结果的回调机制
  - _需求: 2.1, 2.3, 2.4_

- [ ] 5. 数据存储和历史记录管理
  - 实现TranscriptionRepository数据访问层
  - 创建SQLite数据库和表结构
  - 实现转录记录的CRUD操作
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 5.1 创建数据库结构
  - 使用Room数据库，创建Database类：
    ```java
    @Database(entities = {TranscriptionRecord.class}, version = 1)
    @TypeConverters({Converters.class})
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
  - 创建DAO接口：
    ```java
    @Dao
    public interface TranscriptionDao {
        @Query("SELECT * FROM transcription_records ORDER BY timestamp DESC")
        LiveData<List<TranscriptionRecord>> getAllTranscriptions();
        
        @Insert
        void insert(TranscriptionRecord record);
        
        @Update
        void update(TranscriptionRecord record);
        
        @Delete
        void delete(TranscriptionRecord record);
    }
    ```
  - 添加全文搜索索引和时间戳索引
  - _需求: 4.1, 4.2_

- [ ] 5.2 实现TranscriptionRepository
  - 编写saveTranscription()方法：
    ```java
    public class TranscriptionRepository {
        private TranscriptionDao transcriptionDao;
        private LiveData<List<TranscriptionRecord>> allTranscriptions;
        
        public TranscriptionRepository(Application application) {
            AppDatabase db = AppDatabase.getDatabase(application);
            transcriptionDao = db.transcriptionDao();
            allTranscriptions = transcriptionDao.getAllTranscriptions();
        }
        
        public void saveTranscription(TranscriptionRecord record) {
            new Thread(() -> {
                try {
                    transcriptionDao.insert(record);
                    Log.i(TAG, "转录记录保存成功: " + record.getId());
                } catch (Exception e) {
                    Log.e(TAG, "保存转录记录失败", e);
                }
            }).start();
        }
    }
    ```
  - 实现getAllTranscriptions()、getTranscriptionById()等CRUD方法
  - 添加异步操作和错误处理
  - _需求: 4.1, 4.2, 4.3, 4.4_

- [ ] 5.3 实现搜索和查询功能
  - 编写searchTranscriptions()方法，支持全文搜索
  - 实现按时间范围查询功能
  - 添加按置信度筛选功能
  - 实现分页查询，优化大数据量性能
  - _需求: 4.2, 4.5_

- [ ]* 5.4 编写数据访问层单元测试
  - 测试数据库CRUD操作的正确性
  - 测试搜索功能的准确性
  - 测试数据完整性和约束验证
  - _需求: 4.1, 4.2, 4.5_

- [ ] 6. 离线模式和模型管理
  - 实现OfflineModeManager离线模式管理
  - 创建模型文件下载和更新机制
  - 实现网络状态检测和自动切换
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 6.1 实现OfflineModeManager
  - 实现离线模式检查：
    ```java
    public boolean isOfflineModeAvailable() {
        File modelFile = new File(getModelPath());
        return modelFile.exists() && modelFile.length() > 0;
    }
    
    public void enableOfflineMode() {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("offline_mode_enabled", true).apply();
        // 停止网络相关服务
    }
    ```
  - 创建网络状态监听器：
    ```java
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            if (isOfflineModeEnabled()) {
                // 可选择切换到在线模式
            }
        }
        
        @Override
        public void onLost(Network network) {
            enableOfflineMode(); // 自动切换到离线模式
        }
    }
    ```
  - 使用SharedPreferences持久化离线模式状态
  - _需求: 3.1, 3.2, 3.3_

- [ ] 6.2 实现模型文件管理
  - 编写downloadModel()方法：
    ```java
    public boolean downloadModel() {
        try {
            String modelUrl = "https://example.com/whisper-cantonese-model.onnx";
            String localPath = getModelPath();
            
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(modelUrl).build();
            
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                FileOutputStream fos = new FileOutputStream(localPath);
                InputStream is = response.body().byteStream();
                
                byte[] buffer = new byte[8192];
                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                    updateDownloadProgress((float)downloadedBytes / totalBytes);
                }
                
                fos.close();
                is.close();
                return verifyModelIntegrity(localPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "模型下载失败", e);
        }
        return false;
    }
    ```
  - 实现模型完整性验证：使用MD5或SHA256校验
  - 实现版本管理和增量更新逻辑
  - _需求: 3.1, 3.4_

- [ ] 6.3 实现模型存储和加载优化
  - 优化模型文件存储位置和压缩
  - 实现模型预加载和缓存机制
  - 添加模型文件损坏检测和修复
  - 实现模型切换的无缝过渡
  - _需求: 3.1, 3.3, 3.5, 5.4_

- [ ]* 6.4 编写离线模式单元测试
  - 测试离线模式的切换逻辑
  - 测试模型下载和验证功能
  - 测试网络状态变化的响应
  - _需求: 3.1, 3.2, 3.3_

- [ ] 7. 用户界面实现
  - 创建主界面Activity和Fragment
  - 实现录音控制界面
  - 创建转录结果显示界面
  - 实现历史记录管理界面
  - _需求: 1.1, 1.2, 1.5, 2.2, 2.5, 4.2, 4.3, 4.4, 4.5_

- [ ] 7.1 创建主界面Activity
  - 实现MainActivity：
    ```java
    public class MainActivity extends AppCompatActivity {
        private Button recordButton;
        private TextView statusText;
        private ProgressBar progressBar;
        private TranscriptionService transcriptionService;
        private boolean serviceBound = false;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            initViews();
            checkPermissions();
        }
        
        private void initViews() {
            recordButton = findViewById(R.id.record_button);
            statusText = findViewById(R.id.status_text);
            progressBar = findViewById(R.id.progress_bar);
            
            recordButton.setOnClickListener(v -> toggleRecording());
        }
    }
    ```
  - 创建activity_main.xml布局，包含录音按钮、状态显示、进度条
  - 实现权限申请：使用ActivityCompat.requestPermissions()
  - 添加菜单栏，包含设置、历史记录、帮助选项
  - _需求: 1.1, 1.2, 5.1_

- [ ] 7.2 实现录音控制界面
  - 创建录音控制Fragment
  - 实现录音按钮的状态管理（开始/停止/暂停）
  - 添加录音时长显示和音量指示器
  - 实现实时模式切换开关
  - _需求: 1.1, 1.2, 2.1, 2.5_

- [ ] 7.3 创建转录结果显示界面
  - 实现转录结果的实时显示
  - 创建文本编辑功能，支持结果修改
  - 添加转录置信度和处理时间显示
  - 实现结果分享和导出功能
  - _需求: 1.3, 1.5, 2.2, 2.3, 4.3_

- [ ] 7.4 实现历史记录管理界面
  - 创建历史记录列表Fragment
  - 实现记录的时间排序和分组显示
  - 添加搜索框和筛选功能
  - 实现记录的编辑和删除操作
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 7.5 编写UI组件单元测试
  - 测试录音按钮的响应和状态变化
  - 测试转录结果的显示和更新
  - 测试历史记录的列表操作
  - _需求: 1.1, 2.2, 4.2_

- [ ] 8. 错误处理和用户体验优化
  - 实现ErrorHandler错误处理机制
  - 创建用户友好的错误提示界面
  - 实现性能监控和优化
  - _需求: 1.4, 5.1, 5.2, 5.5_

- [ ] 8.1 实现ErrorHandler错误处理
  - 创建ErrorHandler类：
    ```java
    public class ErrorHandler {
        private static final String TAG = "ErrorHandler";
        private Context context;
        private int maxRetryCount = 3;
        
        public ErrorHandler(Context context) {
            this.context = context;
        }
        
        public void handleTranscriptionError(TranscriptionError error) {
            Log.e(TAG, "转录错误: " + error.getMessage());
            
            switch (error) {
                case MODEL_NOT_LOADED:
                    // 尝试重新加载模型
                    retryModelInitialization();
                    break;
                case PERMISSION_DENIED:
                    // 引导用户重新授权
                    showPermissionDialog();
                    break;
                case INSUFFICIENT_STORAGE:
                    // 清理存储空间
                    cleanupOldRecords();
                    break;
                case NETWORK_ERROR:
                    // 切换到离线模式
                    enableOfflineMode();
                    break;
                default:
                    showGenericErrorDialog(error.getMessage());
            }
            
            // 记录错误日志
            logError(error);
        }
        
        private void retryModelInitialization() {
            // 实现重试逻辑，最多重试3次
        }
    }
    ```
  - 实现错误恢复机制和重试逻辑
  - 添加错误日志记录和上报功能
  - _需求: 1.4_

- [ ] 8.2 创建用户友好的错误界面
  - 设计错误提示对话框和Toast消息
  - 实现权限申请引导界面
  - 创建网络错误和离线模式提示
  - 添加模型下载失败的处理界面
  - _需求: 1.4, 5.1_

- [ ] 8.3 实现性能监控和优化
  - 添加应用启动时间监控
  - 实现转录性能指标收集
  - 创建内存使用监控和优化
  - 实现电池使用优化策略
  - _需求: 5.1, 5.2, 5.4, 5.5_

- [ ]* 8.4 编写错误处理单元测试
  - 测试各种错误情况的处理逻辑
  - 测试错误恢复和重试机制
  - 测试用户界面的错误提示
  - _需求: 1.4_

- [ ] 9. 系统集成和最终优化
  - 集成所有模块，实现完整的应用流程
  - 进行端到端测试和性能调优
  - 实现应用配置和设置功能
  - _需求: 所有需求_

- [ ] 9.1 模块集成和应用流程实现
  - 连接UI层与Service层的数据流：
    ```java
    public class TranscriptionController {
        private AudioRecorderManager audioManager;
        private WhisperEngine whisperEngine;
        private TranscriptionService transcriptionService;
        private TranscriptionRepository repository;
        private OfflineModeManager offlineManager;
        
        public void startTranscription(boolean isRealTime) {
            try {
                // 检查离线模式
                if (!NetworkUtils.isConnected(context)) {
                    offlineManager.enableOfflineMode();
                }
                
                // 启动音频录制
                audioManager.startRecording();
                
                // 启动转录服务
                if (isRealTime) {
                    transcriptionService.startRealTimeTranscription();
                } else {
                    transcriptionService.startBatchTranscription();
                }
                
                // 更新UI状态
                updateUIState(TranscriptionState.RECORDING);
                
            } catch (Exception e) {
                errorHandler.handleTranscriptionError(TranscriptionError.GENERAL_ERROR);
            }
        }
        
        public void stopTranscription() {
            audioManager.stopRecording();
            AudioData audioData = audioManager.getAudioData();
            
            // 执行转录
            TranscriptionResult result = whisperEngine.transcribe(audioData);
            
            // 保存结果
            TranscriptionRecord record = new TranscriptionRecord(result);
            repository.saveTranscription(record);
            
            // 更新UI
            updateUIState(TranscriptionState.COMPLETED);
        }
    }
    ```
  - 实现完整的录音→转录→存储流程
  - 添加状态管理和错误处理
  - _需求: 1.1, 1.3, 1.5, 2.1, 2.3, 3.2_

- [ ] 9.2 实现应用设置和配置
  - 创建设置界面，支持音频质量配置
  - 实现转录语言和模型选择功能
  - 添加存储管理和清理功能
  - 实现应用主题和界面定制
  - _需求: 5.4, 3.4_

- [ ] 9.3 性能调优和最终优化
  - 优化应用启动速度和内存使用
  - 调整转录引擎的性能参数
  - 实现后台任务的电池优化
  - 添加应用崩溃监控和自动恢复
  - _需求: 5.1, 5.2, 5.4, 5.5_

- [ ]* 9.4 端到端集成测试
  - 测试完整的录音到转录流程
  - 测试实时转录和离线模式切换
  - 测试应用在不同设备上的兼容性
  - 进行性能基准测试和压力测试
  - _需求: 所有需求_

## 实施说明

### 开发顺序
1. **基础设施阶段** (任务1-2): 搭建项目结构和音频处理基础
2. **核心功能阶段** (任务3-5): 实现Whisper集成和转录服务
3. **数据管理阶段** (任务6): 实现离线模式和模型管理
4. **用户界面阶段** (任务7): 创建完整的用户交互界面
5. **优化完善阶段** (任务8-9): 错误处理和系统集成优化

### 关键依赖关系
- 任务3依赖任务1和2的完成（需要基础接口和音频处理）
- 任务4依赖任务3的完成（需要Whisper引擎）
- 任务7依赖任务4和5的完成（需要转录服务和数据存储）
- 任务9依赖所有前置任务的完成

### 测试策略
- 标记为"*"的任务为可选测试任务，专注于核心功能验证
- 每个主要模块完成后进行单元测试
- 系统集成阶段进行端到端测试
- 性能测试贯穿整个开发过程

### 质量保证
- 每个任务完成后进行代码审查
- 关键功能实现后进行功能验证
- 定期进行性能基准测试
- 持续集成和自动化测试- [ ] **
1.4 编写数据模型单元测试** (可选)
  - **实施步骤**:
    1. **创建TranscriptionRecord测试类** (`test/java/.../TranscriptionRecordTest.java`)
       ```java
       @RunWith(JUnit4.class)
       public class TranscriptionRecordTest {
           
           @Test
           public void testTranscriptionRecordCreation() {
               // 测试转录记录创建
               String originalText = "你好世界";
               long timestamp = System.currentTimeMillis();
               String audioPath = "/path/to/audio.wav";
               int duration = 5;
               float confidence = 0.95f;
               boolean isRealTime = true;
               
               TranscriptionRecord record = new TranscriptionRecord(
                   originalText, timestamp, audioPath, duration, confidence, isRealTime);
               
               assertEquals(originalText, record.getOriginalText());
               assertEquals(originalText, record.getEditedText()); // 初始时相等
               assertEquals(timestamp, record.getTimestamp());
               assertEquals(audioPath, record.getAudioFilePath());
               assertEquals(duration, record.getDuration());
               assertEquals(confidence, record.getConfidence(), 0.001f);
               assertTrue(record.isRealTime());
           }
           
           @Test
           public void testTextEditing() {
               // 测试文本编辑功能
               TranscriptionRecord record = new TranscriptionRecord();
               record.setOriginalText("原始文本");
               record.setEditedText("编辑后文本");
               
               assertEquals("原始文本", record.getOriginalText());
               assertEquals("编辑后文本", record.getEditedText());
           }
       }
       ```
    
    2. **创建AudioData测试类** (`test/java/.../AudioDataTest.java`)
       ```java
       @RunWith(JUnit4.class)
       public class AudioDataTest {
           
           @Test
           public void testAudioDataCreation() {
               // 测试音频数据创建
               byte[] testData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
               int sampleRate = 16000;
               int channels = 1;
               int bitDepth = 16;
               long duration = 1000;
               
               AudioData audioData = new AudioData(testData, sampleRate, 
                                                 channels, bitDepth, duration);
               
               assertArrayEquals(testData, audioData.getRawData());
               assertEquals(sampleRate, audioData.getSampleRate());
               assertEquals(channels, audioData.getChannels());
               assertEquals(bitDepth, audioData.getBitDepth());
               assertEquals(duration, audioData.getDuration());
           }
           
           @Test
           public void testPCM16Conversion() {
               // 测试PCM16格式转换
               byte[] testData8bit = new byte[]{0, 64, 128, 192, 255};
               AudioData audioData = new AudioData(testData8bit, 16000, 1, 8, 1000);
               
               byte[] pcm16Data = audioData.toPCM16();
               assertNotNull(pcm16Data);
               assertEquals(testData8bit.length * 2, pcm16Data.length);
           }
           
           @Test
           public void testDurationCalculation() {
               // 测试时长计算
               byte[] testData = new byte[32000]; // 1秒的16位单声道数据
               AudioData audioData = new AudioData(testData, 16000, 1, 16, 1000);
               
               double duration = audioData.getDurationInSeconds();
               assertEquals(1.0, duration, 0.1);
           }
       }
       ```
    
    3. **创建TranscriptionResult测试类** (`test/java/.../TranscriptionResultTest.java`)
       ```java
       @RunWith(JUnit4.class)
       public class TranscriptionResultTest {
           
           @Test
           public void testTranscriptionResultCreation() {
               // 测试转录结果创建
               String text = "测试转录文本";
               float confidence = 0.9f;
               long processingTime = 1500;
               boolean isComplete = true;
               
               TranscriptionResult result = new TranscriptionResult(
                   text, confidence, processingTime, isComplete);
               
               assertEquals(text, result.getText());
               assertEquals(confidence, result.getConfidence(), 0.001f);
               assertEquals(processingTime, result.getProcessingTime());
               assertTrue(result.isComplete());
               assertNotNull(result.getSegments());
           }
           
           @Test
           public void testSegmentManagement() {
               // 测试词段管理
               TranscriptionResult result = new TranscriptionResult();
               
               WordSegment segment1 = new WordSegment("你好", 0.0f, 0.5f, 0.95f);
               WordSegment segment2 = new WordSegment("世界", 0.5f, 1.0f, 0.90f);
               
               result.addSegment(segment1);
               result.addSegment(segment2);
               
               assertEquals(2, result.getSegments().size());
               assertEquals("你好", result.getSegments().get(0).getWord());
               assertEquals("世界", result.getSegments().get(1).getWord());
           }
       }
       ```
  
  - **验收标准**: 所有数据模型的单元测试通过，覆盖主要功能
  - **预计时间**: 1天
  - **注意**: 此任务为可选，可在后续开发中补充

### 阶段二：音频录制和处理模块

- [ ] **任务2: 音频录制和处理模块实现**
  - **目标**: 实现音频录制、格式转换和实时音频流处理
  - **预计时间**: 3-4天
  - **依赖**: 任务1完成
  - **验收标准**: 能够录制音频并进行格式转换，支持实时音频流处理
  - _对应需求: 需求1.1, 1.2, 2.1, 2.2, 2.3_

- [ ] **2.1 实现AudioRecorderManager核心功能**
  - **实施步骤**:
    1. **创建AudioRecorderManager类** (`audio/AudioRecorderManager.java`)
       ```java
       public class AudioRecorderManager {
           private static final String TAG = "AudioRecorderManager";
           private static final int SAMPLE_RATE = 16000;
           private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
           private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
           
           private AudioRecord audioRecord;
           private boolean isRecording = false;
           private boolean isPaused = false;
           private Thread recordingThread;
           private List<Byte> audioBuffer;
           private AudioStreamListener listener;
           private Context context;
           
           public AudioRecorderManager(Context context) {
               this.context = context;
               this.audioBuffer = new ArrayList<>();
           }
           
           public void setAudioStreamListener(AudioStreamListener listener) {
               this.listener = listener;
           }
           
           public void startRecording() {
               if (!checkAudioPermission()) {
                   requestAudioPermission();
                   return;
               }
               
               try {
                   int bufferSize = AudioRecord.getMinBufferSize(
                       SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                   
                   if (bufferSize == AudioRecord.ERROR_BAD_VALUE || 
                       bufferSize == AudioRecord.ERROR) {
                       if (listener != null) {
                           listener.onRecordingError(AudioError.INITIALIZATION_FAILED);
                       }
                       return;
                   }
                   
                   audioRecord = new AudioRecord(
                       MediaRecorder.AudioSource.MIC,
                       SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
                   
                   if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                       if (listener != null) {
                           listener.onRecordingError(AudioError.INITIALIZATION_FAILED);
                       }
                       return;
                   }
                   
                   audioBuffer.clear();
                   audioRecord.startRecording();
                   isRecording = true;
                   isPaused = false;
                   
                   if (listener != null) {
                       listener.onRecordingStarted();
                   }
                   
                   startAudioThread();
                   
               } catch (SecurityException e) {
                   Log.e(TAG, "录音权限被拒绝", e);
                   if (listener != null) {
                       listener.onRecordingError(AudioError.PERMISSION_DENIED);
                   }
               } catch (Exception e) {
                   Log.e(TAG, "录音启动失败", e);
                   if (listener != null) {
                       listener.onRecordingError(AudioError.RECORDING_FAILED);
                   }
               }
           }
           
           public void stopRecording() {
               if (audioRecord != null && isRecording) {
                   isRecording = false;
                   audioRecord.stop();
                   audioRecord.release();
                   audioRecord = null;
                   
                   if (recordingThread != null) {
                       try {
                           recordingThread.join();
                       } catch (InterruptedException e) {
                           Log.e(TAG, "等待录音线程结束时被中断", e);
                       }
                   }
                   
                   if (listener != null) {
                       listener.onRecordingStopped();
                   }
               }
           }
           
           public void pauseRecording() {
               if (isRecording && !isPaused) {
                   isPaused = true;
                   Log.i(TAG, "录音已暂停");
               }
           }
           
           public void resumeRecording() {
               if (isRecording && isPaused) {
                   isPaused = false;
                   Log.i(TAG, "录音已恢复");
               }
           }
           
           private void startAudioThread() {
               recordingThread = new Thread(() -> {
                   byte[] buffer = new byte[1024];
                   
                   while (isRecording) {
                       if (!isPaused && audioRecord != null) {
                           int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                           
                           if (bytesRead > 0) {
                               // 添加到缓冲区
                               for (int i = 0; i < bytesRead; i++) {
                                   audioBuffer.add(buffer[i]);
                               }
                               
                               // 通知监听器
                               if (listener != null) {
                                   byte[] audioData = new byte[bytesRead];
                                   System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                                   listener.onAudioDataAvailable(audioData, bytesRead);
                                   
                                   // 计算音量级别
                                   float volume = calculateVolume(audioData);
                                   listener.onVolumeChanged(volume);
                               }
                           } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                               Log.e(TAG, "录音操作无效");
                               if (listener != null) {
                                   listener.onRecordingError(AudioError.RECORDING_FAILED);
                               }
                               break;
                           }
                       }
                       
                       try {
                           Thread.sleep(10); // 短暂休眠避免过度占用CPU
                       } catch (InterruptedException e) {
                           break;
                       }
                   }
               });
               
               recordingThread.start();
           }
           
           private boolean checkAudioPermission() {
               return ContextCompat.checkSelfPermission(context, 
                   Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
           }
           
           private void requestAudioPermission() {
               if (context instanceof Activity) {
                   ActivityCompat.requestPermissions((Activity) context,
                       new String[]{Manifest.permission.RECORD_AUDIO}, 
                       REQUEST_AUDIO_PERMISSION);
               }
           }
           
           private float calculateVolume(byte[] audioData) {
               // 计算RMS音量
               long sum = 0;
               for (byte b : audioData) {
                   sum += b * b;
               }
               double rms = Math.sqrt(sum / audioData.length);
               return (float) Math.min(rms / 32768.0, 1.0); // 归一化到0-1
           }
           
           // getter方法
           public boolean isRecording() { return isRecording; }
           public boolean isPaused() { return isPaused; }
       }
       ```
  
  - **验收标准**: AudioRecorderManager能够成功录制音频，处理权限和错误情况
  - **预计时间**: 1.5天- 
[ ] **2.2 实现音频数据处理和格式转换**
  - **实施步骤**:
    1. **扩展AudioRecorderManager添加数据处理方法**
       ```java
       // 在AudioRecorderManager类中添加以下方法
       
       public AudioData getAudioData() {
           if (audioBuffer == null || audioBuffer.isEmpty()) {
               return null;
           }
           
           // 转换List<Byte>为byte[]
           byte[] audioBytes = new byte[audioBuffer.size()];
           for (int i = 0; i < audioBuffer.size(); i++) {
               audioBytes[i] = audioBuffer.get(i);
           }
           
           // 计算时长
           long duration = (long)(audioBytes.length / (SAMPLE_RATE * 2.0) * 1000);
           
           return new AudioData(audioBytes, SAMPLE_RATE, 1, 16, duration);
       }
       
       public void setAudioFormat(int sampleRate, int channels, int bitDepth) {
           if (isRecording) {
               Log.w(TAG, "无法在录音过程中更改音频格式");
               return;
           }
           
           this.SAMPLE_RATE = sampleRate;
           
           // 更新声道配置
           if (channels == 1) {
               this.CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
           } else if (channels == 2) {
               this.CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
           }
           
           // 更新音频格式
           if (bitDepth == 8) {
               this.AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
           } else if (bitDepth == 16) {
               this.AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
           }
           
           Log.i(TAG, String.format("音频格式已更新: %dHz, %d声道, %d位", 
                                   sampleRate, channels, bitDepth));
       }
       
       // 音频质量检测
       public float analyzeAudioQuality(byte[] audioData) {
           if (audioData == null || audioData.length == 0) {
               return 0.0f;
           }
           
           // 计算信噪比
           double signalPower = 0;
           double noisePower = 0;
           
           // 简单的信号功率计算
           for (int i = 0; i < audioData.length - 1; i += 2) {
               short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
               signalPower += sample * sample;
           }
           
           signalPower /= (audioData.length / 2);
           
           // 估算噪音功率（使用最小值作为噪音基线）
           noisePower = signalPower * 0.1; // 简化计算
           
           // 计算SNR
           double snr = 10 * Math.log10(signalPower / noisePower);
           
           // 归一化到0-1范围
           return (float) Math.max(0, Math.min(1, snr / 40.0));
       }
       
       // 噪音过滤
       public byte[] applyNoiseFilter(byte[] audioData) {
           if (audioData == null || audioData.length < 2) {
               return audioData;
           }
           
           byte[] filteredData = new byte[audioData.length];
           
           // 简单的低通滤波器
           for (int i = 0; i < audioData.length - 1; i += 2) {
               short currentSample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
               
               // 应用简单的噪音门限
               if (Math.abs(currentSample) < 500) { // 噪音门限
                   currentSample = 0;
               }
               
               filteredData[i] = (byte) (currentSample & 0xFF);
               filteredData[i + 1] = (byte) ((currentSample >> 8) & 0xFF);
           }
           
           return filteredData;
       }
       ```
    
    2. **创建音频工具类** (`utils/AudioUtils.java`)
       ```java
       public class AudioUtils {
           
           /**
            * 将音频数据保存为WAV文件
            */
           public static boolean saveAsWav(AudioData audioData, String filePath) {
               try {
                   FileOutputStream fos = new FileOutputStream(filePath);
                   
                   // 写入WAV文件头
                   writeWavHeader(fos, audioData);
                   
                   // 写入音频数据
                   fos.write(audioData.getRawData());
                   fos.close();
                   
                   return true;
               } catch (IOException e) {
                   Log.e("AudioUtils", "保存WAV文件失败", e);
                   return false;
               }
           }
           
           private static void writeWavHeader(FileOutputStream fos, AudioData audioData) 
                   throws IOException {
               int sampleRate = audioData.getSampleRate();
               int channels = audioData.getChannels();
               int bitDepth = audioData.getBitDepth();
               int dataLength = audioData.getRawData().length;
               
               // WAV文件头（44字节）
               fos.write("RIFF".getBytes());
               fos.write(intToByteArray(36 + dataLength), 0, 4);
               fos.write("WAVE".getBytes());
               fos.write("fmt ".getBytes());
               fos.write(intToByteArray(16), 0, 4); // fmt chunk size
               fos.write(shortToByteArray((short) 1), 0, 2); // PCM format
               fos.write(shortToByteArray((short) channels), 0, 2);
               fos.write(intToByteArray(sampleRate), 0, 4);
               fos.write(intToByteArray(sampleRate * channels * bitDepth / 8), 0, 4);
               fos.write(shortToByteArray((short) (channels * bitDepth / 8)), 0, 2);
               fos.write(shortToByteArray((short) bitDepth), 0, 2);
               fos.write("data".getBytes());
               fos.write(intToByteArray(dataLength), 0, 4);
           }
           
           private static byte[] intToByteArray(int value) {
               return new byte[] {
                   (byte) (value & 0xFF),
                   (byte) ((value >> 8) & 0xFF),
                   (byte) ((value >> 16) & 0xFF),
                   (byte) ((value >> 24) & 0xFF)
               };
           }
           
           private static byte[] shortToByteArray(short value) {
               return new byte[] {
                   (byte) (value & 0xFF),
                   (byte) ((value >> 8) & 0xFF)
               };
           }
           
           /**
            * 音频数据重采样
            */
           public static AudioData resample(AudioData originalData, int targetSampleRate) {
               if (originalData.getSampleRate() == targetSampleRate) {
                   return originalData;
               }
               
               // 简单的线性插值重采样
               byte[] originalBytes = originalData.getRawData();
               double ratio = (double) targetSampleRate / originalData.getSampleRate();
               int newLength = (int) (originalBytes.length * ratio);
               byte[] resampledBytes = new byte[newLength];
               
               for (int i = 0; i < newLength - 1; i += 2) {
                   double originalIndex = i / ratio;
                   int index = (int) originalIndex;
                   
                   if (index < originalBytes.length - 3) {
                       short sample1 = (short) ((originalBytes[index + 1] << 8) | 
                                              (originalBytes[index] & 0xFF));
                       short sample2 = (short) ((originalBytes[index + 3] << 8) | 
                                              (originalBytes[index + 2] & 0xFF));
                       
                       double fraction = originalIndex - index;
                       short interpolated = (short) (sample1 + fraction * (sample2 - sample1));
                       
                       resampledBytes[i] = (byte) (interpolated & 0xFF);
                       resampledBytes[i + 1] = (byte) ((interpolated >> 8) & 0xFF);
                   }
               }
               
               long newDuration = (long) (originalData.getDuration() * ratio);
               return new AudioData(resampledBytes, targetSampleRate, 
                                  originalData.getChannels(), originalData.getBitDepth(), 
                                  newDuration);
           }
       }
       ```
  
  - **验收标准**: 音频数据能够正确处理和格式转换，支持质量检测和噪音过滤
  - **预计时间**: 1天

- [ ] **2.3 实现实时音频流处理**
  - **实施步骤**:
    1. **创建实时音频处理器** (`audio/RealTimeAudioProcessor.java`)
       ```java
       public class RealTimeAudioProcessor {
           private static final String TAG = "RealTimeAudioProcessor";
           private static final int BUFFER_SIZE = 4096;
           private static final int VAD_THRESHOLD = 1000; // 语音活动检测阈值
           
           private Queue<byte[]> audioQueue;
           private boolean isProcessing = false;
           private Thread processingThread;
           private AudioStreamListener listener;
           private VoiceActivityDetector vad;
           
           public RealTimeAudioProcessor() {
               this.audioQueue = new LinkedList<>();
               this.vad = new VoiceActivityDetector();
           }
           
           public void setAudioStreamListener(AudioStreamListener listener) {
               this.listener = listener;
           }
           
           public void startProcessing() {
               if (isProcessing) {
                   return;
               }
               
               isProcessing = true;
               processingThread = new Thread(this::processAudioStream);
               processingThread.start();
           }
           
           public void stopProcessing() {
               isProcessing = false;
               if (processingThread != null) {
                   try {
                       processingThread.join();
                   } catch (InterruptedException e) {
                       Log.e(TAG, "停止音频处理线程时被中断", e);
                   }
               }
           }
           
           public void addAudioData(byte[] audioData) {
               synchronized (audioQueue) {
                   audioQueue.offer(audioData);
                   
                   // 限制队列大小，避免内存溢出
                   while (audioQueue.size() > 50) {
                       audioQueue.poll();
                   }
               }
           }
           
           private void processAudioStream() {
               byte[] buffer = new byte[BUFFER_SIZE];
               int bufferIndex = 0;
               
               while (isProcessing) {
                   byte[] audioData;
                   synchronized (audioQueue) {
                       audioData = audioQueue.poll();
                   }
                   
                   if (audioData != null) {
                       // 将数据添加到缓冲区
                       for (byte b : audioData) {
                           buffer[bufferIndex++] = b;
                           
                           if (bufferIndex >= BUFFER_SIZE) {
                               // 缓冲区满，处理数据
                               processAudioChunk(buffer);
                               bufferIndex = 0;
                           }
                       }
                   } else {
                       // 没有数据时短暂休眠
                       try {
                           Thread.sleep(10);
                       } catch (InterruptedException e) {
                           break;
                       }
                   }
               }
           }
           
           private void processAudioChunk(byte[] audioChunk) {
               // 语音活动检测
               boolean hasVoice = vad.detectVoiceActivity(audioChunk);
               
               if (hasVoice) {
                   // 有语音活动，通知监听器
                   if (listener != null) {
                       listener.onAudioDataAvailable(audioChunk, audioChunk.length);
                   }
               }
           }
           
           /**
            * 语音活动检测器
            */
           private static class VoiceActivityDetector {
               private double energyThreshold = 1000.0;
               private int consecutiveFrames = 0;
               private final int minConsecutiveFrames = 3;
               
               public boolean detectVoiceActivity(byte[] audioData) {
                   double energy = calculateEnergy(audioData);
                   
                   if (energy > energyThreshold) {
                       consecutiveFrames++;
                       return consecutiveFrames >= minConsecutiveFrames;
                   } else {
                       consecutiveFrames = 0;
                       return false;
                   }
               }
               
               private double calculateEnergy(byte[] audioData) {
                   double energy = 0;
                   for (int i = 0; i < audioData.length - 1; i += 2) {
                       short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                       energy += sample * sample;
                   }
                   return energy / (audioData.length / 2);
               }
           }
       }
       ```
    
    2. **创建音频缓冲区管理器** (`audio/AudioBufferManager.java`)
       ```java
       public class AudioBufferManager {
           private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1MB
           private CircularBuffer circularBuffer;
           private final Object lock = new Object();
           
           public AudioBufferManager() {
               this.circularBuffer = new CircularBuffer(MAX_BUFFER_SIZE);
           }
           
           public void addAudioData(byte[] data) {
               synchronized (lock) {
                   circularBuffer.write(data);
               }
           }
           
           public byte[] getAudioData(int length) {
               synchronized (lock) {
                   return circularBuffer.read(length);
               }
           }
           
           public void clear() {
               synchronized (lock) {
                   circularBuffer.clear();
               }
           }
           
           public int getAvailableBytes() {
               synchronized (lock) {
                   return circularBuffer.available();
               }
           }
           
           /**
            * 环形缓冲区实现
            */
           private static class CircularBuffer {
               private byte[] buffer;
               private int writeIndex = 0;
               private int readIndex = 0;
               private int size = 0;
               private final int capacity;
               
               public CircularBuffer(int capacity) {
                   this.capacity = capacity;
                   this.buffer = new byte[capacity];
               }
               
               public void write(byte[] data) {
                   for (byte b : data) {
                       buffer[writeIndex] = b;
                       writeIndex = (writeIndex + 1) % capacity;
                       
                       if (size < capacity) {
                           size++;
                       } else {
                           // 缓冲区满，移动读指针
                           readIndex = (readIndex + 1) % capacity;
                       }
                   }
               }
               
               public byte[] read(int length) {
                   int actualLength = Math.min(length, size);
                   byte[] result = new byte[actualLength];
                   
                   for (int i = 0; i < actualLength; i++) {
                       result[i] = buffer[readIndex];
                       readIndex = (readIndex + 1) % capacity;
                       size--;
                   }
                   
                   return result;
               }
               
               public void clear() {
                   writeIndex = 0;
                   readIndex = 0;
                   size = 0;
               }
               
               public int available() {
                   return size;
               }
           }
       }
       ```
  
  - **验收标准**: 实时音频流处理正常工作，支持VAD和缓冲区管理
  - **预计时间**: 1.5天

- [ ] **2.4 编写音频模块单元测试** (可选)
  - **实施步骤**:
    1. **创建AudioRecorderManager测试**
    2. **创建音频格式转换测试**
    3. **创建实时音频流测试**
  - **预计时间**: 1天
  - **注意**: 此任务为可选，可在后续开发中补充#
## 阶段三：Whisper模型集成和JNI接口

- [ ] **任务3: Whisper模型集成和JNI接口**
  - **目标**: 集成OpenAI Whisper粤语模型，实现JNI接口
  - **预计时间**: 4-5天
  - **依赖**: 任务1和任务2完成
  - **验收标准**: Whisper模型能够正常加载和执行转录，达到85%以上准确率
  - _对应需求: 需求1.4, 3.1, 3.3_

- [ ] **3.1 创建JNI接口层**
  - **实施步骤**:
    1. **创建WhisperJNI类** (`native/WhisperJNI.java`)
       ```java
       public class WhisperJNI {
           private static final String TAG = "WhisperJNI";
           
           static {
               try {
                   System.loadLibrary("whisper-jni");
                   Log.i(TAG, "Whisper JNI库加载成功");
               } catch (UnsatisfiedLinkError e) {
                   Log.e(TAG, "Whisper JNI库加载失败", e);
               }
           }
           
           /**
            * 初始化Whisper模型
            * @param modelPath 模型文件路径
            * @return 是否初始化成功
            */
           public native boolean initModel(String modelPath);
           
           /**
            * 执行音频转录
            * @param audioData 音频数据
            * @param length 数据长度
            * @return 转录结果JSON字符串
            */
           public native String transcribe(byte[] audioData, int length);
           
           /**
            * 实时转录
            * @param audioData 音频数据
            * @param length 数据长度
            * @return 部分转录结果
            */
           public native String transcribeRealTime(byte[] audioData, int length);
           
           /**
            * 释放模型资源
            */
           public native void releaseModel();
           
           /**
            * 检查模型是否已加载
            * @return 模型加载状态
            */
           public native boolean isModelLoaded();
           
           /**
            * 获取模型信息
            * @return 模型信息JSON字符串
            */
           public native String getModelInfo();
           
           /**
            * 设置模型参数
            * @param params 参数JSON字符串
            * @return 是否设置成功
            */
           public native boolean setModelParams(String params);
       }
       ```
    
    2. **创建C++实现文件** (`src/main/cpp/whisper-jni.cpp`)
       ```cpp
       #include <jni.h>
       #include <string>
       #include <vector>
       #include <android/log.h>
       #include "whisper.h" // Whisper库头文件
       
       #define LOG_TAG "WhisperJNI"
       #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
       #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
       
       static struct whisper_context* g_whisper_context = nullptr;
       
       extern "C" JNIEXPORT jboolean JNICALL
       Java_com_app_cantonesespeech_native_WhisperJNI_initModel(
           JNIEnv *env, jobject thiz, jstring model_path) {
           
           const char* path = env->GetStringUTFChars(model_path, 0);
           
           try {
               // 释放之前的模型
               if (g_whisper_context != nullptr) {
                   whisper_free(g_whisper_context);
                   g_whisper_context = nullptr;
               }
               
               // 加载新模型
               g_whisper_context = whisper_init_from_file(path);
               
               if (g_whisper_context == nullptr) {
                   LOGE("Failed to load model from %s", path);
                   env->ReleaseStringUTFChars(model_path, path);
                   return JNI_FALSE;
               }
               
               LOGI("Model loaded successfully from %s", path);
               env->ReleaseStringUTFChars(model_path, path);
               return JNI_TRUE;
               
           } catch (const std::exception& e) {
               LOGE("Exception in initModel: %s", e.what());
               env->ReleaseStringUTFChars(model_path, path);
               return JNI_FALSE;
           }
       }
       
       extern "C" JNIEXPORT jstring JNICALL
       Java_com_app_cantonesespeech_native_WhisperJNI_transcribe(
           JNIEnv *env, jobject thiz, jbyteArray audio_data, jint length) {
           
           if (g_whisper_context == nullptr) {
               LOGE("Model not initialized");
               return env->NewStringUTF("{\"error\":\"模型未初始化\"}");
           }
           
           // 获取音频数据
           jbyte* audio_bytes = env->GetByteArrayElements(audio_data, nullptr);
           if (audio_bytes == nullptr) {
               return env->NewStringUTF("{\"error\":\"音频数据无效\"}");
           }
           
           try {
               // 转换音频数据为float格式
               std::vector<float> audio_float(length / 2);
               for (int i = 0; i < length / 2; i++) {
                   int16_t sample = (int16_t)((audio_bytes[i * 2 + 1] << 8) | 
                                            (audio_bytes[i * 2] & 0xFF));
                   audio_float[i] = (float)sample / 32768.0f;
               }
               
               // 设置转录参数
               struct whisper_full_params params = whisper_full_default_params(
                   WHISPER_SAMPLING_GREEDY);
               params.language = "zh"; // 中文
               params.translate = false;
               params.print_progress = false;
               params.print_timestamps = true;
               
               // 执行转录
               int result = whisper_full(g_whisper_context, params, 
                                       audio_float.data(), audio_float.size());
               
               if (result != 0) {
                   LOGE("Transcription failed with code %d", result);
                   env->ReleaseByteArrayElements(audio_data, audio_bytes, JNI_ABORT);
                   return env->NewStringUTF("{\"error\":\"转录失败\"}");
               }
               
               // 获取转录结果
               std::string transcription_text;
               const int n_segments = whisper_full_n_segments(g_whisper_context);
               
               for (int i = 0; i < n_segments; i++) {
                   const char* text = whisper_full_get_segment_text(g_whisper_context, i);
                   transcription_text += text;
               }
               
               // 构建JSON结果
               std::string json_result = "{\"text\":\"" + transcription_text + 
                                       "\",\"confidence\":0.9,\"segments\":" + 
                                       std::to_string(n_segments) + "}";
               
               env->ReleaseByteArrayElements(audio_data, audio_bytes, JNI_ABORT);
               return env->NewStringUTF(json_result.c_str());
               
           } catch (const std::exception& e) {
               LOGE("Exception in transcribe: %s", e.what());
               env->ReleaseByteArrayElements(audio_data, audio_bytes, JNI_ABORT);
               return env->NewStringUTF("{\"error\":\"转录异常\"}");
           }
       }
       
       extern "C" JNIEXPORT void JNICALL
       Java_com_app_cantonesespeech_native_WhisperJNI_releaseModel(
           JNIEnv *env, jobject thiz) {
           
           if (g_whisper_context != nullptr) {
               whisper_free(g_whisper_context);
               g_whisper_context = nullptr;
               LOGI("Model released");
           }
       }
       
       extern "C" JNIEXPORT jboolean JNICALL
       Java_com_app_cantonesespeech_native_WhisperJNI_isModelLoaded(
           JNIEnv *env, jobject thiz) {
           return g_whisper_context != nullptr ? JNI_TRUE : JNI_FALSE;
       }
       ```
    
    3. **配置CMakeLists.txt**
       ```cmake
       cmake_minimum_required(VERSION 3.18.1)
       project("whisper-jni")
       
       # 设置C++标准
       set(CMAKE_CXX_STANDARD 17)
       
       # 添加Whisper库
       add_subdirectory(whisper)
       
       # 创建JNI库
       add_library(whisper-jni SHARED whisper-jni.cpp)
       
       # 链接库
       target_link_libraries(whisper-jni
           whisper
           android
           log)
       ```
    
    4. **配置build.gradle添加NDK支持**
       ```gradle
       android {
           ...
           externalNativeBuild {
               cmake {
                   path "src/main/cpp/CMakeLists.txt"
                   version "3.18.1"
               }
           }
           
           defaultConfig {
               ...
               ndk {
                   abiFilters 'arm64-v8a', 'armeabi-v7a'
               }
           }
       }
       ```
  
  - **验收标准**: JNI接口创建完成，能够成功编译和链接
  - **预计时间**: 2天

- [ ] **3.2 实现WhisperEngine核心功能**
  - **实施步骤**:
    1. **创建WhisperEngine类** (`transcription/WhisperEngine.java`)
       ```java
       public class WhisperEngine {
           private static final String TAG = "WhisperEngine";
           private WhisperJNI whisperJNI;
           private String modelPath;
           private boolean isModelInitialized = false;
           private ExecutorService executorService;
           
           public WhisperEngine() {
               this.whisperJNI = new WhisperJNI();
               this.executorService = Executors.newSingleThreadExecutor();
           }
           
           /**
            * 初始化Whisper模型
            */
           public boolean initializeModel(String modelPath) {
               try {
                   if (whisperJNI == null) {
                       whisperJNI = new WhisperJNI();
                   }
                   
                   // 检查模型文件是否存在
                   File modelFile = new File(modelPath);
                   if (!modelFile.exists()) {
                       Log.e(TAG, "模型文件不存在: " + modelPath);
                       return false;
                   }
                   
                   boolean success = whisperJNI.initModel(modelPath);
                   if (success) {
                       this.modelPath = modelPath;
                       isModelInitialized = true;
                       Log.i(TAG, "Whisper模型初始化成功: " + modelPath);
                   } else {
                       Log.e(TAG, "Whisper模型初始化失败");
                   }
                   
                   return success;
               } catch (Exception e) {
                   Log.e(TAG, "模型初始化异常", e);
                   return false;
               }
           }
           
           /**
            * 执行音频转录
            */
           public void transcribe(AudioData audioData, TranscriptionCallback callback) {
               if (!isModelLoaded()) {
                   if (callback != null) {
                       callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
                   }
                   return;
               }
               
               executorService.execute(() -> {
                   try {
                       if (callback != null) {
                           callback.onTranscriptionStarted();
                       }
                       
                       long startTime = System.currentTimeMillis();
                       
                       // 执行转录
                       String resultJson = whisperJNI.transcribe(
                           audioData.getRawData(), audioData.getRawData().length);
                       
                       long processingTime = System.currentTimeMillis() - startTime;
                       
                       // 解析结果
                       TranscriptionResult result = parseTranscriptionResult(
                           resultJson, processingTime);
                       
                       if (callback != null) {
                           if (result != null) {
                               callback.onTranscriptionCompleted(result);
                           } else {
                               callback.onTranscriptionError(TranscriptionError.GENERAL_ERROR);
                           }
                       }
                       
                   } catch (Exception e) {
                       Log.e(TAG, "转录过程异常", e);
                       if (callback != null) {
                           callback.onTranscriptionError(TranscriptionError.GENERAL_ERROR);
                       }
                   }
               });
           }
           
           /**
            * 实时转录
            */
           public void transcribeRealTime(AudioData audioData, TranscriptionCallback callback) {
               if (!isModelLoaded()) {
                   if (callback != null) {
                       callback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
                   }
                   return;
               }
               
               executorService.execute(() -> {
                   try {
                       String resultJson = whisperJNI.transcribeRealTime(
                           audioData.getRawData(), audioData.getRawData().length);
                       
                       // 解析部分结果
                       String partialText = parsePartialResult(resultJson);
                       
                       if (callback != null && partialText != null) {
                           callback.onPartialResult(partialText);
                       }
                       
                   } catch (Exception e) {
                       Log.e(TAG, "实时转录异常", e);
                       if (callback != null) {
                           callback.onTranscriptionError(TranscriptionError.GENERAL_ERROR);
                       }
                   }
               });
           }
           
           /**
            * 检查模型是否已加载
            */
           public boolean isModelLoaded() {
               return isModelInitialized && whisperJNI != null && whisperJNI.isModelLoaded();
           }
           
           /**
            * 释放模型资源
            */
           public void releaseModel() {
               if (whisperJNI != null) {
                   whisperJNI.releaseModel();
                   isModelInitialized = false;
                   Log.i(TAG, "Whisper模型资源已释放");
               }
           }
           
           /**
            * 解析转录结果JSON
            */
           private TranscriptionResult parseTranscriptionResult(String json, long processingTime) {
               try {
                   // 简单的JSON解析（实际项目中建议使用JSON库）
                   if (json.contains("\"error\"")) {
                       Log.e(TAG, "转录返回错误: " + json);
                       return null;
                   }
                   
                   // 提取文本
                   String text = extractJsonValue(json, "text");
                   String confidenceStr = extractJsonValue(json, "confidence");
                   float confidence = confidenceStr != null ? Float.parseFloat(confidenceStr) : 0.9f;
                   
                   TranscriptionResult result = new TranscriptionResult(
                       text, confidence, processingTime, true);
                   
                   // 解析词段信息（如果有）
                   parseWordSegments(json, result);
                   
                   return result;
                   
               } catch (Exception e) {
                   Log.e(TAG, "解析转录结果失败", e);
                   return null;
               }
           }
           
           private String parsePartialResult(String json) {
               try {
                   return extractJsonValue(json, "text");
               } catch (Exception e) {
                   Log.e(TAG, "解析部分结果失败", e);
                   return null;
               }
           }
           
           private String extractJsonValue(String json, String key) {
               String searchKey = "\"" + key + "\":\"";
               int startIndex = json.indexOf(searchKey);
               if (startIndex == -1) return null;
               
               startIndex += searchKey.length();
               int endIndex = json.indexOf("\"", startIndex);
               if (endIndex == -1) return null;
               
               return json.substring(startIndex, endIndex);
           }
           
           private void parseWordSegments(String json, TranscriptionResult result) {
               // 解析词段信息的实现
               // 这里简化处理，实际项目中需要完整的JSON解析
           }
           
           /**
            * 清理资源
            */
           public void cleanup() {
               releaseModel();
               if (executorService != null && !executorService.isShutdown()) {
                   executorService.shutdown();
               }
           }
       }
       ```
  
  - **验收标准**: WhisperEngine能够正常初始化模型并执行转录
  - **预计时间**: 1.5天

- [ ] **3.3 集成ONNX Runtime**
  - **实施步骤**:
    1. **配置ONNX Runtime依赖**
    2. **实现模型文件加载和验证**
    3. **添加GPU加速支持**
    4. **实现错误处理机制**
  - **预计时间**: 1.5天

- [ ] **3.4 编写Whisper引擎单元测试** (可选)
  - **预计时间**: 1天### 阶
段四：转录服务和业务逻辑

- [ ] **任务4: 转录服务和业务逻辑实现**
  - **目标**: 创建后台转录服务，实现完整的业务逻辑
  - **预计时间**: 3-4天
  - **依赖**: 任务3完成
  - **验收标准**: 转录服务能够在后台运行，支持实时和批量转录
  - _对应需求: 需求1.3, 1.5, 2.1-2.5_

- [ ] **4.1 实现TranscriptionService基础框架**
  - **实施步骤**:
    1. **创建TranscriptionService类** (`service/TranscriptionService.java`)
       ```java
       public class TranscriptionService extends Service {
           private static final String TAG = "TranscriptionService";
           private static final int NOTIFICATION_ID = 1001;
           private static final String CHANNEL_ID = "transcription_channel";
           
           private final IBinder binder = new TranscriptionBinder();
           private NotificationManager notificationManager;
           private WhisperEngine whisperEngine;
           private AudioRecorderManager audioRecorderManager;
           private RealTimeAudioProcessor audioProcessor;
           private TranscriptionCallback currentCallback;
           private boolean isRealTimeMode = false;
           
           public class TranscriptionBinder extends Binder {
               public TranscriptionService getService() {
                   return TranscriptionService.this;
               }
           }
           
           @Override
           public void onCreate() {
               super.onCreate();
               Log.i(TAG, "TranscriptionService创建");
               
               // 初始化组件
               whisperEngine = new WhisperEngine();
               audioRecorderManager = new AudioRecorderManager(this);
               audioProcessor = new RealTimeAudioProcessor();
               notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
               
               // 创建通知渠道
               createNotificationChannel();
               
               // 设置音频流监听器
               audioRecorderManager.setAudioStreamListener(new AudioStreamListener() {
                   @Override
                   public void onAudioDataAvailable(byte[] audioData, int length) {
                       if (isRealTimeMode) {
                           processRealTimeAudio(audioData);
                       }
                   }
                   
                   @Override
                   public void onRecordingStarted() {
                       Log.i(TAG, "录音开始");
                       updateNotification("正在录音...", true);
                   }
                   
                   @Override
                   public void onRecordingStopped() {
                       Log.i(TAG, "录音停止");
                       if (!isRealTimeMode) {
                           processRecordedAudio();
                       }
                   }
                   
                   @Override
                   public void onRecordingError(AudioError error) {
                       Log.e(TAG, "录音错误: " + error.getMessage());
                       if (currentCallback != null) {
                           currentCallback.onTranscriptionError(
                               TranscriptionError.AUDIO_RECORDING_FAILED);
                       }
                   }
                   
                   @Override
                   public void onVolumeChanged(float volume) {
                       // 可以用于UI显示音量指示器
                   }
               });
           }
           
           @Override
           public int onStartCommand(Intent intent, int flags, int startId) {
               Log.i(TAG, "TranscriptionService启动");
               
               // 启动前台服务
               startForeground(NOTIFICATION_ID, createNotification("转录服务已启动", false));
               
               return START_STICKY; // 服务被杀死后自动重启
           }
           
           @Override
           public IBinder onBind(Intent intent) {
               Log.i(TAG, "TranscriptionService绑定");
               return binder;
           }
           
           @Override
           public void onDestroy() {
               Log.i(TAG, "TranscriptionService销毁");
               
               // 停止录音
               if (audioRecorderManager != null) {
                   audioRecorderManager.stopRecording();
               }
               
               // 停止实时处理
               if (audioProcessor != null) {
                   audioProcessor.stopProcessing();
               }
               
               // 释放Whisper引擎
               if (whisperEngine != null) {
                   whisperEngine.cleanup();
               }
               
               super.onDestroy();
           }
           
           /**
            * 创建通知渠道
            */
           private void createNotificationChannel() {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                   NotificationChannel channel = new NotificationChannel(
                       CHANNEL_ID,
                       "语音转录服务",
                       NotificationManager.IMPORTANCE_LOW
                   );
                   channel.setDescription("粤语语音转录后台服务");
                   channel.setSound(null, null);
                   notificationManager.createNotificationChannel(channel);
               }
           }
           
           /**
            * 创建通知
            */
           private Notification createNotification(String content, boolean isRecording) {
               Intent notificationIntent = new Intent(this, MainActivity.class);
               PendingIntent pendingIntent = PendingIntent.getActivity(
                   this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
               
               NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                   .setContentTitle("粤语语音转录")
                   .setContentText(content)
                   .setSmallIcon(R.drawable.ic_mic)
                   .setContentIntent(pendingIntent)
                   .setOngoing(true)
                   .setPriority(NotificationCompat.PRIORITY_LOW);
               
               if (isRecording) {
                   builder.setColor(Color.RED);
                   // 添加停止录音按钮
                   Intent stopIntent = new Intent(this, TranscriptionService.class);
                   stopIntent.setAction("STOP_RECORDING");
                   PendingIntent stopPendingIntent = PendingIntent.getService(
                       this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
                   builder.addAction(R.drawable.ic_stop, "停止录音", stopPendingIntent);
               }
               
               return builder.build();
           }
           
           /**
            * 更新通知
            */
           private void updateNotification(String content, boolean isRecording) {
               Notification notification = createNotification(content, isRecording);
               notificationManager.notify(NOTIFICATION_ID, notification);
           }
       }
       ```
  
  - **验收标准**: TranscriptionService能够正常启动和绑定，前台通知正常显示
  - **预计时间**: 1天

- [ ] **4.2 实现转录业务逻辑**
  - **实施步骤**:
    1. **在TranscriptionService中添加业务逻辑方法**
       ```java
       // 在TranscriptionService类中添加以下方法
       
       /**
        * 设置转录回调
        */
       public void setTranscriptionCallback(TranscriptionCallback callback) {
           this.currentCallback = callback;
       }
       
       /**
        * 启动实时转录
        */
       public void startRealTimeTranscription() {
           if (!whisperEngine.isModelLoaded()) {
               // 尝试加载默认模型
               String modelPath = getDefaultModelPath();
               if (!whisperEngine.initializeModel(modelPath)) {
                   if (currentCallback != null) {
                       currentCallback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
                   }
                   return;
               }
           }
           
           isRealTimeMode = true;
           audioProcessor.startProcessing();
           audioRecorderManager.startRecording();
           
           updateNotification("实时转录中...", true);
           Log.i(TAG, "实时转录已启动");
       }
       
       /**
        * 停止实时转录
        */
       public void stopRealTimeTranscription() {
           isRealTimeMode = false;
           audioRecorderManager.stopRecording();
           audioProcessor.stopProcessing();
           
           updateNotification("转录服务运行中", false);
           Log.i(TAG, "实时转录已停止");
       }
       
       /**
        * 处理音频文件转录
        */
       public void processAudioFile(String filePath) {
           if (!whisperEngine.isModelLoaded()) {
               if (currentCallback != null) {
                   currentCallback.onTranscriptionError(TranscriptionError.MODEL_NOT_LOADED);
               }
               return;
           }
           
           // 在后台线程中处理
           new Thread(() -> {
               try {
                   // 读取音频文件
                   AudioData audioData = loadAudioFile(filePath);
                   if (audioData == null) {
                       if (currentCallback != null) {
                           currentCallback.onTranscriptionError(
                               TranscriptionError.AUDIO_FORMAT_UNSUPPORTED);
                       }
                       return;
                   }
                   
                   // 执行转录
                   whisperEngine.transcribe(audioData, currentCallback);
                   
               } catch (Exception e) {
                   Log.e(TAG, "处理音频文件失败", e);
                   if (currentCallback != null) {
                       currentCallback.onTranscriptionError(TranscriptionError.GENERAL_ERROR);
                   }
               }
           }).start();
       }
       
       /**
        * 处理实时音频数据
        */
       private void processRealTimeAudio(byte[] audioData) {
           if (audioData == null || audioData.length == 0) {
               return;
           }
           
           // 创建AudioData对象
           AudioData audio = new AudioData(audioData, 16000, 1, 16, 
                                         audioData.length / 32); // 简化时长计算
           
           // 执行实时转录
           whisperEngine.transcribeRealTime(audio, currentCallback);
       }
       
       /**
        * 处理录制完成的音频
        */
       private void processRecordedAudio() {
           AudioData audioData = audioRecorderManager.getAudioData();
           if (audioData != null) {
               updateNotification("正在转录...", false);
               whisperEngine.transcribe(audioData, new TranscriptionCallback() {
                   @Override
                   public void onTranscriptionStarted() {
                       if (currentCallback != null) {
                           currentCallback.onTranscriptionStarted();
                       }
                   }
                   
                   @Override
                   public void onPartialResult(String partialText) {
                       if (currentCallback != null) {
                           currentCallback.onPartialResult(partialText);
                       }
                   }
                   
                   @Override
                   public void onTranscriptionCompleted(TranscriptionResult result) {
                       updateNotification("转录完成", false);
                       if (currentCallback != null) {
                           currentCallback.onTranscriptionCompleted(result);
                       }
                   }
                   
                   @Override
                   public void onTranscriptionError(TranscriptionError error) {
                       updateNotification("转录失败", false);
                       if (currentCallback != null) {
                           currentCallback.onTranscriptionError(error);
                       }
                   }
                   
                   @Override
                   public void onProgressUpdate(float progress) {
                       if (currentCallback != null) {
                           currentCallback.onProgressUpdate(progress);
                       }
                   }
               });
           }
       }
       
       /**
        * 加载音频文件
        */
       private AudioData loadAudioFile(String filePath) {
           try {
               File file = new File(filePath);
               if (!file.exists()) {
                   Log.e(TAG, "音频文件不存在: " + filePath);
                   return null;
               }
               
               // 读取文件数据
               FileInputStream fis = new FileInputStream(file);
               byte[] audioBytes = new byte[(int) file.length()];
               fis.read(audioBytes);
               fis.close();
               
               // 简化处理，假设是16kHz, 16bit, 单声道PCM数据
               long duration = (long) (audioBytes.length / (16000 * 2.0) * 1000);
               
               return new AudioData(audioBytes, 16000, 1, 16, duration);
               
           } catch (IOException e) {
               Log.e(TAG, "读取音频文件失败", e);
               return null;
           }
       }
       
       /**
        * 获取默认模型路径
        */
       private String getDefaultModelPath() {
           return getFilesDir().getAbsolutePath() + "/whisper-cantonese.bin";
       }
       
       // 公共接口方法
       public boolean isRecording() {
           return audioRecorderManager != null && audioRecorderManager.isRecording();
       }
       
       public boolean isRealTimeMode() {
           return isRealTimeMode;
       }
       
       public boolean isModelLoaded() {
           return whisperEngine != null && whisperEngine.isModelLoaded();
       }
       ```
  
  - **验收标准**: 转录业务逻辑完整，支持实时和批量转录
  - **预计时间**: 1.5天

- [ ] **4.3 实现转录状态管理**
  - **实施步骤**:
    1. **创建转录状态枚举** (`utils/TranscriptionState.java`)
       ```java
       public enum TranscriptionState {
           IDLE("空闲"),
           INITIALIZING("初始化中"),
           RECORDING("录音中"),
           PROCESSING("处理中"),
           COMPLETED("已完成"),
           ERROR("错误"),
           PAUSED("已暂停");
           
           private final String description;
           
           TranscriptionState(String description) {
               this.description = description;
           }
           
           public String getDescription() {
               return description;
           }
       }
       ```
    
    2. **创建状态管理器** (`utils/TranscriptionStateManager.java`)
       ```java
       public class TranscriptionStateManager {
           private static final String TAG = "TranscriptionStateManager";
           private TranscriptionState currentState = TranscriptionState.IDLE;
           private final List<StateChangeListener> listeners = new ArrayList<>();
           private final Object stateLock = new Object();
           
           public interface StateChangeListener {
               void onStateChanged(TranscriptionState oldState, TranscriptionState newState);
           }
           
           public void addStateChangeListener(StateChangeListener listener) {
               synchronized (listeners) {
                   listeners.add(listener);
               }
           }
           
           public void removeStateChangeListener(StateChangeListener listener) {
               synchronized (listeners) {
                   listeners.remove(listener);
               }
           }
           
           public void setState(TranscriptionState newState) {
               TranscriptionState oldState;
               synchronized (stateLock) {
                   oldState = currentState;
                   currentState = newState;
               }
               
               Log.i(TAG, String.format("状态变更: %s -> %s", 
                                       oldState.getDescription(), 
                                       newState.getDescription()));
               
               // 通知监听器
               synchronized (listeners) {
                   for (StateChangeListener listener : listeners) {
                       try {
                           listener.onStateChanged(oldState, newState);
                       } catch (Exception e) {
                           Log.e(TAG, "状态变更通知失败", e);
                       }
                   }
               }
           }
           
           public TranscriptionState getCurrentState() {
               synchronized (stateLock) {
                   return currentState;
               }
           }
           
           public boolean canTransitionTo(TranscriptionState targetState) {
               synchronized (stateLock) {
                   return isValidTransition(currentState, targetState);
               }
           }
           
           private boolean isValidTransition(TranscriptionState from, TranscriptionState to) {
               // 定义有效的状态转换
               switch (from) {
                   case IDLE:
                       return to == TranscriptionState.INITIALIZING || 
                              to == TranscriptionState.RECORDING;
                   case INITIALIZING:
                       return to == TranscriptionState.IDLE || 
                              to == TranscriptionState.RECORDING ||
                              to == TranscriptionState.ERROR;
                   case RECORDING:
                       return to == TranscriptionState.PROCESSING || 
                              to == TranscriptionState.PAUSED ||
                              to == TranscriptionState.IDLE ||
                              to == TranscriptionState.ERROR;
                   case PROCESSING:
                       return to == TranscriptionState.COMPLETED || 
                              to == TranscriptionState.ERROR ||
                              to == TranscriptionState.IDLE;
                   case PAUSED:
                       return to == TranscriptionState.RECORDING || 
                              to == TranscriptionState.IDLE;
                   case COMPLETED:
                   case ERROR:
                       return to == TranscriptionState.IDLE;
                   default:
                       return false;
               }
           }
       }
       ```
    
    3. **创建任务队列管理器** (`utils/TranscriptionTaskQueue.java`)
       ```java
       public class TranscriptionTaskQueue {
           private static final String TAG = "TranscriptionTaskQueue";
           private final Queue<TranscriptionTask> taskQueue = new LinkedList<>();
           private final Object queueLock = new Object();
           private boolean isProcessing = false;
           private ExecutorService executorService;
           
           public static class TranscriptionTask {
               public enum TaskType {
                   REAL_TIME, BATCH_AUDIO, FILE_PROCESSING
               }
               
               private final String id;
               private final TaskType type;
               private final AudioData audioData;
               private final String filePath;
               private final TranscriptionCallback callback;
               private final long timestamp;
               
               public TranscriptionTask(TaskType type, AudioData audioData, 
                                      TranscriptionCallback callback) {
                   this.id = UUID.randomUUID().toString();
                   this.type = type;
                   this.audioData = audioData;
                   this.filePath = null;
                   this.callback = callback;
                   this.timestamp = System.currentTimeMillis();
               }
               
               public TranscriptionTask(String filePath, TranscriptionCallback callback) {
                   this.id = UUID.randomUUID().toString();
                   this.type = TaskType.FILE_PROCESSING;
                   this.audioData = null;
                   this.filePath = filePath;
                   this.callback = callback;
                   this.timestamp = System.currentTimeMillis();
               }
               
               // getter方法
               public String getId() { return id; }
               public TaskType getType() { return type; }
               public AudioData getAudioData() { return audioData; }
               public String getFilePath() { return filePath; }
               public TranscriptionCallback getCallback() { return callback; }
               public long getTimestamp() { return timestamp; }
           }
           
           public TranscriptionTaskQueue() {
               this.executorService = Executors.newSingleThreadExecutor();
           }
           
           public void addTask(TranscriptionTask task) {
               synchronized (queueLock) {
                   taskQueue.offer(task);
                   Log.i(TAG, String.format("任务已添加到队列: %s, 队列大小: %d", 
                                           task.getId(), taskQueue.size()));
               }
               
               processNextTask();
           }
           
           public void clearQueue() {
               synchronized (queueLock) {
                   int clearedCount = taskQueue.size();
                   taskQueue.clear();
                   Log.i(TAG, String.format("队列已清空，清除了 %d 个任务", clearedCount));
               }
           }
           
           public int getQueueSize() {
               synchronized (queueLock) {
                   return taskQueue.size();
               }
           }
           
           private void processNextTask() {
               if (isProcessing) {
                   return;
               }
               
               TranscriptionTask task;
               synchronized (queueLock) {
                   task = taskQueue.poll();
               }
               
               if (task == null) {
                   return;
               }
               
               isProcessing = true;
               executorService.execute(() -> {
                   try {
                       Log.i(TAG, "开始处理任务: " + task.getId());
                       // 这里会调用实际的转录逻辑
                       // processTask(task);
                   } catch (Exception e) {
                       Log.e(TAG, "处理任务失败: " + task.getId(), e);
                   } finally {
                       isProcessing = false;
                       // 处理下一个任务
                       processNextTask();
                   }
               });
           }
           
           public void shutdown() {
               if (executorService != null && !executorService.isShutdown()) {
                   executorService.shutdown();
               }
           }
       }
       ```
  
  - **验收标准**: 状态管理和任务队列正常工作
  - **预计时间**: 1天

- [ ] **4.4 编写转录服务单元测试** (可选)
  - **预计时间**: 1天### 阶段五：数
据存储和历史记录管理

- [ ] **任务5: 数据存储和历史记录管理**
  - **目标**: 实现完整的数据持久化和历史记录管理功能
  - **预计时间**: 2-3天
  - **依赖**: 任务1完成
  - **验收标准**: 转录记录能够正确保存、查询、编辑和删除
  - _对应需求: 需求4.1-4.5_

- [ ] **5.1 创建数据库结构**
  - **实施步骤**:
    1. **创建Room数据库** (`data/AppDatabase.java`)
       ```java
       @Database(
           entities = {TranscriptionRecord.class},
           version = 1,
           exportSchema = false
       )
       @TypeConverters({Converters.class})
       public abstract class AppDatabase extends RoomDatabase {
           private static final String DATABASE_NAME = "transcription_database";
           private static volatile AppDatabase INSTANCE;
           
           public abstract TranscriptionDao transcriptionDao();
           
           public static AppDatabase getDatabase(final Context context) {
               if (INSTANCE == null) {
                   synchronized (AppDatabase.class) {
                       if (INSTANCE == null) {
                           INSTANCE = Room.databaseBuilder(
                               context.getApplicationContext(),
                               AppDatabase.class,
                               DATABASE_NAME
                           )
                           .addCallback(roomCallback)
                           .build();
                       }
                   }
               }
               return INSTANCE;
           }
           
           private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
               @Override
               public void onCreate(@NonNull SupportSQLiteDatabase db) {
                   super.onCreate(db);
                   // 创建全文搜索索引
                   db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS transcription_fts " +
                            "USING fts4(content='transcription_records', original_text, edited_text)");
                   
                   // 创建触发器以维护FTS表
                   db.execSQL("CREATE TRIGGER IF NOT EXISTS transcription_fts_insert " +
                            "AFTER INSERT ON transcription_records BEGIN " +
                            "INSERT INTO transcription_fts(docid, original_text, edited_text) " +
                            "VALUES (new.id, new.original_text, new.edited_text); END");
                   
                   db.execSQL("CREATE TRIGGER IF NOT EXISTS transcription_fts_update " +
                            "AFTER UPDATE ON transcription_records BEGIN " +
                            "UPDATE transcription_fts SET original_text = new.original_text, " +
                            "edited_text = new.edited_text WHERE docid = new.id; END");
                   
                   db.execSQL("CREATE TRIGGER IF NOT EXISTS transcription_fts_delete " +
                            "AFTER DELETE ON transcription_records BEGIN " +
                            "DELETE FROM transcription_fts WHERE docid = old.id; END");
               }
           };
       }
       ```
    
    2. **创建DAO接口** (`data/TranscriptionDao.java`)
       ```java
       @Dao
       public interface TranscriptionDao {
           
           @Query("SELECT * FROM transcription_records ORDER BY timestamp DESC")
           LiveData<List<TranscriptionRecord>> getAllTranscriptions();
           
           @Query("SELECT * FROM transcription_records WHERE id = :id")
           LiveData<TranscriptionRecord> getTranscriptionById(long id);
           
           @Query("SELECT * FROM transcription_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
           LiveData<List<TranscriptionRecord>> getTranscriptionsByTimeRange(long startTime, long endTime);
           
           @Query("SELECT * FROM transcription_records WHERE confidence >= :minConfidence ORDER BY confidence DESC")
           LiveData<List<TranscriptionRecord>> getTranscriptionsByConfidence(float minConfidence);
           
           @Query("SELECT * FROM transcription_records WHERE is_real_time = :isRealTime ORDER BY timestamp DESC")
           LiveData<List<TranscriptionRecord>> getTranscriptionsByType(boolean isRealTime);
           
           @Query("SELECT * FROM transcription_records ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
           LiveData<List<TranscriptionRecord>> getTranscriptionsPaged(int limit, int offset);
           
           // 全文搜索
           @Query("SELECT tr.* FROM transcription_records tr " +
                  "JOIN transcription_fts fts ON tr.id = fts.docid " +
                  "WHERE transcription_fts MATCH :query " +
                  "ORDER BY tr.timestamp DESC")
           LiveData<List<TranscriptionRecord>> searchTranscriptions(String query);
           
           @Insert
           long insert(TranscriptionRecord record);
           
           @Insert
           void insertAll(TranscriptionRecord... records);
           
           @Update
           void update(TranscriptionRecord record);
           
           @Delete
           void delete(TranscriptionRecord record);
           
           @Query("DELETE FROM transcription_records WHERE id = :id")
           void deleteById(long id);
           
           @Query("DELETE FROM transcription_records WHERE timestamp < :timestamp")
           void deleteOldRecords(long timestamp);
           
           @Query("SELECT COUNT(*) FROM transcription_records")
           LiveData<Integer> getRecordCount();
           
           @Query("SELECT SUM(duration) FROM transcription_records")
           LiveData<Integer> getTotalDuration();
           
           @Query("SELECT AVG(confidence) FROM transcription_records")
           LiveData<Float> getAverageConfidence();
       }
       ```
    
    3. **创建类型转换器** (`data/Converters.java`)
       ```java
       public class Converters {
           @TypeConverter
           public static Date fromTimestamp(Long value) {
               return value == null ? null : new Date(value);
           }
           
           @TypeConverter
           public static Long dateToTimestamp(Date date) {
               return date == null ? null : date.getTime();
           }
           
           @TypeConverter
           public static String fromStringList(List<String> strings) {
               if (strings == null) {
                   return null;
               }
               return TextUtils.join(",", strings);
           }
           
           @TypeConverter
           public static List<String> fromString(String value) {
               if (value == null) {
                   return null;
               }
               return Arrays.asList(value.split(","));
           }
       }
       ```
  
  - **验收标准**: 数据库结构创建完成，支持全文搜索和索引
  - **预计时间**: 1天

- [ ] **5.2 实现TranscriptionRepository**
  - **实施步骤**:
    1. **创建Repository类** (`data/TranscriptionRepository.java`)
       ```java
       public class TranscriptionRepository {
           private static final String TAG = "TranscriptionRepository";
           private TranscriptionDao transcriptionDao;
           private LiveData<List<TranscriptionRecord>> allTranscriptions;
           private ExecutorService executorService;
           
           public TranscriptionRepository(Application application) {
               AppDatabase db = AppDatabase.getDatabase(application);
               transcriptionDao = db.transcriptionDao();
               allTranscriptions = transcriptionDao.getAllTranscriptions();
               executorService = Executors.newFixedThreadPool(2);
           }
           
           // 获取所有转录记录
           public LiveData<List<TranscriptionRecord>> getAllTranscriptions() {
               return allTranscriptions;
           }
           
           // 根据ID获取转录记录
           public LiveData<TranscriptionRecord> getTranscriptionById(long id) {
               return transcriptionDao.getTranscriptionById(id);
           }
           
           // 保存转录记录
           public void saveTranscription(TranscriptionRecord record, 
                                       RepositoryCallback<Long> callback) {
               executorService.execute(() -> {
                   try {
                       long id = transcriptionDao.insert(record);
                       Log.i(TAG, "转录记录保存成功，ID: " + id);
                       
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onSuccess(id));
                       }
                   } catch (Exception e) {
                       Log.e(TAG, "保存转录记录失败", e);
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onError(e));
                       }
                   }
               });
           }
           
           // 更新转录记录
           public void updateTranscription(TranscriptionRecord record, 
                                         RepositoryCallback<Void> callback) {
               executorService.execute(() -> {
                   try {
                       transcriptionDao.update(record);
                       Log.i(TAG, "转录记录更新成功，ID: " + record.getId());
                       
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onSuccess(null));
                       }
                   } catch (Exception e) {
                       Log.e(TAG, "更新转录记录失败", e);
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onError(e));
                       }
                   }
               });
           }
           
           // 删除转录记录
           public void deleteTranscription(TranscriptionRecord record, 
                                         RepositoryCallback<Void> callback) {
               executorService.execute(() -> {
                   try {
                       // 删除关联的音频文件
                       if (record.getAudioFilePath() != null) {
                           File audioFile = new File(record.getAudioFilePath());
                           if (audioFile.exists()) {
                               audioFile.delete();
                           }
                       }
                       
                       transcriptionDao.delete(record);
                       Log.i(TAG, "转录记录删除成功，ID: " + record.getId());
                       
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onSuccess(null));
                       }
                   } catch (Exception e) {
                       Log.e(TAG, "删除转录记录失败", e);
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onError(e));
                       }
                   }
               });
           }
           
           // 搜索转录记录
           public LiveData<List<TranscriptionRecord>> searchTranscriptions(String query) {
               // 处理搜索查询，添加通配符
               String processedQuery = "*" + query.replace(" ", "* *") + "*";
               return transcriptionDao.searchTranscriptions(processedQuery);
           }
           
           // 按时间范围查询
           public LiveData<List<TranscriptionRecord>> getTranscriptionsByTimeRange(
                   long startTime, long endTime) {
               return transcriptionDao.getTranscriptionsByTimeRange(startTime, endTime);
           }
           
           // 按置信度筛选
           public LiveData<List<TranscriptionRecord>> getTranscriptionsByConfidence(
                   float minConfidence) {
               return transcriptionDao.getTranscriptionsByConfidence(minConfidence);
           }
           
           // 分页查询
           public LiveData<List<TranscriptionRecord>> getTranscriptionsPaged(
                   int page, int pageSize) {
               int offset = page * pageSize;
               return transcriptionDao.getTranscriptionsPaged(pageSize, offset);
           }
           
           // 清理旧记录
           public void cleanupOldRecords(int daysToKeep, RepositoryCallback<Integer> callback) {
               executorService.execute(() -> {
                   try {
                       long cutoffTime = System.currentTimeMillis() - 
                                       (daysToKeep * 24L * 60L * 60L * 1000L);
                       
                       // 获取要删除的记录，以便删除音频文件
                       // 这里简化处理，实际项目中需要先查询再删除
                       transcriptionDao.deleteOldRecords(cutoffTime);
                       
                       Log.i(TAG, "清理了超过 " + daysToKeep + " 天的旧记录");
                       
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onSuccess(0)); // 返回删除的记录数
                       }
                   } catch (Exception e) {
                       Log.e(TAG, "清理旧记录失败", e);
                       if (callback != null) {
                           new Handler(Looper.getMainLooper()).post(() -> 
                               callback.onError(e));
                       }
                   }
               });
           }
           
           // 获取统计信息
           public LiveData<Integer> getRecordCount() {
               return transcriptionDao.getRecordCount();
           }
           
           public LiveData<Integer> getTotalDuration() {
               return transcriptionDao.getTotalDuration();
           }
           
           public LiveData<Float> getAverageConfidence() {
               return transcriptionDao.getAverageConfidence();
           }
           
           // 回调接口
           public interface RepositoryCallback<T> {
               void onSuccess(T result);
               void onError(Exception error);
           }
           
           // 清理资源
           public void cleanup() {
               if (executorService != null && !executorService.isShutdown()) {
                   executorService.shutdown();
               }
           }
       }
       ```
  
  - **验收标准**: Repository能够正确执行CRUD操作和搜索功能
  - **预计时间**: 1.5天

### 阶段六：用户界面实现

- [ ] **任务6: 用户界面实现**
  - **目标**: 创建完整的用户界面，包括录音、转录结果显示和历史记录管理
  - **预计时间**: 4-5天
  - **依赖**: 任务4和任务5完成
  - **验收标准**: 用户界面友好，功能完整，响应流畅
  - _对应需求: 需求1.1, 1.2, 1.5, 2.2, 2.5, 4.2-4.5, 5.1, 5.5_

- [ ] **6.1 创建主界面Activity**
  - **实施步骤**:
    1. **实现MainActivity** (`ui/MainActivity.java`)
       ```java
       public class MainActivity extends AppCompatActivity implements 
               TranscriptionStateManager.StateChangeListener {
           
           private static final String TAG = "MainActivity";
           private static final int REQUEST_AUDIO_PERMISSION = 1001;
           
           // UI组件
           private Button recordButton;
           private TextView statusText;
           private ProgressBar progressBar;
           private Switch realTimeModeSwitch;
           private TextView transcriptionResultText;
           private RecyclerView historyRecyclerView;
           
           // 服务和管理器
           private TranscriptionService transcriptionService;
           private boolean serviceBound = false;
           private TranscriptionStateManager stateManager;
           private TranscriptionRepository repository;
           
           // ViewModels
           private TranscriptionViewModel transcriptionViewModel;
           
           @Override
           protected void onCreate(Bundle savedInstanceState) {
               super.onCreate(savedInstanceState);
               setContentView(R.layout.activity_main);
               
               initViews();
               initViewModels();
               initServices();
               checkPermissions();
               
               // 设置状态管理器
               stateManager = new TranscriptionStateManager();
               stateManager.addStateChangeListener(this);
           }
           
           private void initViews() {
               recordButton = findViewById(R.id.record_button);
               statusText = findViewById(R.id.status_text);
               progressBar = findViewById(R.id.progress_bar);
               realTimeModeSwitch = findViewById(R.id.real_time_mode_switch);
               transcriptionResultText = findViewById(R.id.transcription_result_text);
               historyRecyclerView = findViewById(R.id.history_recycler_view);
               
               // 设置点击监听器
               recordButton.setOnClickListener(v -> toggleRecording());
               realTimeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                   // 切换实时模式
                   if (serviceBound && transcriptionService != null) {
                       if (isChecked && !transcriptionService.isRealTimeMode()) {
                           transcriptionService.startRealTimeTranscription();
                       } else if (!isChecked && transcriptionService.isRealTimeMode()) {
                           transcriptionService.stopRealTimeTranscription();
                       }
                   }
               });
               
               // 设置历史记录列表
               setupHistoryRecyclerView();
           }
           
           private void initViewModels() {
               transcriptionViewModel = new ViewModelProvider(this)
                   .get(TranscriptionViewModel.class);
               
               // 观察转录记录
               transcriptionViewModel.getAllTranscriptions().observe(this, records -> {
                   if (records != null) {
                       updateHistoryList(records);
                   }
               });
           }
           
           private void initServices() {
               // 绑定转录服务
               Intent serviceIntent = new Intent(this, TranscriptionService.class);
               startService(serviceIntent);
               bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
           }
           
           private ServiceConnection serviceConnection = new ServiceConnection() {
               @Override
               public void onServiceConnected(ComponentName className, IBinder service) {
                   TranscriptionService.TranscriptionBinder binder = 
                       (TranscriptionService.TranscriptionBinder) service;
                   transcriptionService = binder.getService();
                   serviceBound = true;
                   
                   // 设置转录回调
                   transcriptionService.setTranscriptionCallback(transcriptionCallback);
                   
                   Log.i(TAG, "转录服务已连接");
                   updateUI();
               }
               
               @Override
               public void onServiceDisconnected(ComponentName arg0) {
                   serviceBound = false;
                   Log.i(TAG, "转录服务已断开");
               }
           };
           
           private TranscriptionCallback transcriptionCallback = new TranscriptionCallback() {
               @Override
               public void onTranscriptionStarted() {
                   runOnUiThread(() -> {
                       stateManager.setState(TranscriptionState.PROCESSING);
                       statusText.setText("转录中...");
                       progressBar.setVisibility(View.VISIBLE);
                   });
               }
               
               @Override
               public void onPartialResult(String partialText) {
                   runOnUiThread(() -> {
                       transcriptionResultText.setText(partialText);
                   });
               }
               
               @Override
               public void onTranscriptionCompleted(TranscriptionResult result) {
                   runOnUiThread(() -> {
                       stateManager.setState(TranscriptionState.COMPLETED);
                       transcriptionResultText.setText(result.getText());
                       statusText.setText("转录完成");
                       progressBar.setVisibility(View.GONE);
                       
                       // 保存转录结果
                       saveTranscriptionResult(result);
                   });
               }
               
               @Override
               public void onTranscriptionError(TranscriptionError error) {
                   runOnUiThread(() -> {
                       stateManager.setState(TranscriptionState.ERROR);
                       statusText.setText("转录失败: " + error.getMessage());
                       progressBar.setVisibility(View.GONE);
                       showErrorDialog(error);
                   });
               }
               
               @Override
               public void onProgressUpdate(float progress) {
                   runOnUiThread(() -> {
                       progressBar.setProgress((int) (progress * 100));
                   });
               }
           };
           
           private void toggleRecording() {
               if (!serviceBound || transcriptionService == null) {
                   Toast.makeText(this, "服务未就绪", Toast.LENGTH_SHORT).show();
                   return;
               }
               
               if (transcriptionService.isRecording()) {
                   // 停止录音
                   if (transcriptionService.isRealTimeMode()) {
                       transcriptionService.stopRealTimeTranscription();
                   } else {
                       // 批量模式下停止录音会自动触发转录
                   }
                   stateManager.setState(TranscriptionState.IDLE);
               } else {
                   // 开始录音
                   if (realTimeModeSwitch.isChecked()) {
                       transcriptionService.startRealTimeTranscription();
                   } else {
                       // 开始批量录音
                       // transcriptionService.startBatchRecording();
                   }
                   stateManager.setState(TranscriptionState.RECORDING);
               }
           }
           
           @Override
           public void onStateChanged(TranscriptionState oldState, TranscriptionState newState) {
               runOnUiThread(() -> updateUIForState(newState));
           }
           
           private void updateUIForState(TranscriptionState state) {
               switch (state) {
                   case IDLE:
                       recordButton.setText("开始录音");
                       recordButton.setEnabled(true);
                       statusText.setText("就绪");
                       break;
                   case RECORDING:
                       recordButton.setText("停止录音");
                       recordButton.setEnabled(true);
                       statusText.setText("录音中...");
                       break;
                   case PROCESSING:
                       recordButton.setEnabled(false);
                       statusText.setText("处理中...");
                       break;
                   case COMPLETED:
                       recordButton.setText("开始录音");
                       recordButton.setEnabled(true);
                       break;
                   case ERROR:
                       recordButton.setText("开始录音");
                       recordButton.setEnabled(true);
                       break;
               }
           }
           
           // 其他辅助方法...
           private void checkPermissions() {
               if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                       != PackageManager.PERMISSION_GRANTED) {
                   ActivityCompat.requestPermissions(this,
                           new String[]{Manifest.permission.RECORD_AUDIO},
                           REQUEST_AUDIO_PERMISSION);
               }
           }
           
           @Override
           protected void onDestroy() {
               super.onDestroy();
               if (serviceBound) {
                   unbindService(serviceConnection);
                   serviceBound = false;
               }
               if (stateManager != null) {
                   stateManager.removeStateChangeListener(this);
               }
           }
       }
       ```
  
  - **验收标准**: 主界面能够正常显示和交互，服务绑定正常
  - **预计时间**: 2天

## 实施说明

### 开发优先级
1. **高优先级**: 任务1-4 (核心功能)
2. **中优先级**: 任务5-6 (数据和界面)
3. **低优先级**: 可选测试任务

### 质量保证
- 每个任务完成后进行功能验证
- 关键模块完成后进行集成测试
- 定期进行代码审查
- 持续监控性能指标

### 风险控制
- Whisper模型集成可能遇到兼容性问题
- JNI开发需要C++经验
- 实时转录对性能要求较高
- 建议预留20%的缓冲时间

### 总预计时间
- **核心开发**: 15-20天
- **测试和优化**: 5-7天
- **总计**: 20-27天