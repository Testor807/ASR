# 设计文档

## 概述

本设计文档描述了一个Android移动应用的架构，该应用集成OpenAI Whisper粤语微调模型，提供高效的语音识别和转录功能。应用采用模块化架构，支持实时转录和离线使用，使用Java语言开发。

## 架构

### 整体架构

应用采用分层架构模式，包含以下主要层次：

```
┌─────────────────────────────────────┐
│           UI Layer (Activity/Fragment)           │
├─────────────────────────────────────┤
│           Service Layer             │
├─────────────────────────────────────┤
│           Business Logic Layer      │
├─────────────────────────────────────┤
│           Data Access Layer         │
├─────────────────────────────────────┤
│           Native Layer (JNI)        │
└─────────────────────────────────────┘
```

### 核心架构组件

1. **UI层**: 负责用户界面展示和交互
2. **服务层**: 处理后台任务和系统服务
3. **业务逻辑层**: 核心业务逻辑处理
4. **数据访问层**: 数据库和文件操作
5. **原生层**: Whisper模型集成和音频处理

## 组件和接口

### 主要组件

#### 1. AudioRecorderManager
```java
public class AudioRecorderManager {
    public void startRecording();
    public void stopRecording();
    public void pauseRecording();
    public AudioData getAudioData();
    public void setAudioFormat(AudioFormat format);
}
```

#### 2. WhisperEngine
```java
public class WhisperEngine {
    public boolean initializeModel(String modelPath);
    public TranscriptionResult transcribe(AudioData audioData);
    public TranscriptionResult transcribeRealTime(AudioStream audioStream);
    public boolean isModelLoaded();
    public void releaseModel();
}
```

#### 3. TranscriptionService
```java
public class TranscriptionService extends Service {
    public void startRealTimeTranscription();
    public void stopRealTimeTranscription();
    public TranscriptionResult processAudioFile(String filePath);
    public void setTranscriptionCallback(TranscriptionCallback callback);
}
```

#### 4. TranscriptionRepository
```java
public class TranscriptionRepository {
    public void saveTranscription(TranscriptionRecord record);
    public List<TranscriptionRecord> getAllTranscriptions();
    public TranscriptionRecord getTranscriptionById(long id);
    public void updateTranscription(TranscriptionRecord record);
    public void deleteTranscription(long id);
    public List<TranscriptionRecord> searchTranscriptions(String query);
}
```

#### 5. OfflineModeManager
```java
public class OfflineModeManager {
    public boolean isOfflineModeAvailable();
    public void enableOfflineMode();
    public void disableOfflineMode();
    public boolean downloadModel();
    public float getModelDownloadProgress();
}
```

### 接口定义

#### TranscriptionCallback
```java
public interface TranscriptionCallback {
    void onTranscriptionStarted();
    void onPartialResult(String partialText);
    void onTranscriptionCompleted(TranscriptionResult result);
    void onTranscriptionError(TranscriptionError error);
}
```

#### AudioStreamListener
```java
public interface AudioStreamListener {
    void onAudioDataAvailable(byte[] audioData);
    void onRecordingStarted();
    void onRecordingStopped();
    void onRecordingError(AudioError error);
}
```

## 数据模型

### TranscriptionRecord
```java
public class TranscriptionRecord {
    private long id;
    private String originalText;
    private String editedText;
    private long timestamp;
    private String audioFilePath;
    private int duration;
    private float confidence;
    private boolean isRealTime;
    
    // Getters and Setters
}
```

### AudioData
```java
public class AudioData {
    private byte[] rawData;
    private int sampleRate;
    private int channels;
    private int bitDepth;
    private long duration;
    
    // Getters and Setters
}
```

### TranscriptionResult
```java
public class TranscriptionResult {
    private String text;
    private float confidence;
    private long processingTime;
    private List<WordSegment> segments;
    private boolean isComplete;
    
    // Getters and Setters
}
```

### WordSegment
```java
public class WordSegment {
    private String word;
    private float startTime;
    private float endTime;
    private float confidence;
    
    // Getters and Setters
}
```

## 错误处理

### 错误类型定义

#### TranscriptionError
```java
public enum TranscriptionError {
    MODEL_NOT_LOADED("语音模型未加载"),
    AUDIO_FORMAT_UNSUPPORTED("不支持的音频格式"),
    INSUFFICIENT_STORAGE("存储空间不足"),
    PERMISSION_DENIED("权限被拒绝"),
    NETWORK_ERROR("网络错误"),
    MODEL_CORRUPTED("模型文件损坏");
    
    private final String message;
}
```

### 错误处理策略

1. **模型加载失败**: 提示用户重新下载模型，提供离线模式降级方案
2. **音频录制错误**: 检查麦克风权限，提供权限申请引导
3. **存储空间不足**: 清理旧的转录记录，压缩音频文件
4. **网络连接问题**: 自动切换到离线模式
5. **转录失败**: 重试机制，最多重试3次

### 异常处理流程

```java
public class ErrorHandler {
    public void handleTranscriptionError(TranscriptionError error) {
        switch (error) {
            case MODEL_NOT_LOADED:
                // 重新初始化模型
                break;
            case PERMISSION_DENIED:
                // 引导用户授权
                break;
            case INSUFFICIENT_STORAGE:
                // 清理存储空间
                break;
            default:
                // 通用错误处理
        }
    }
}
```

## 测试策略

### 单元测试

1. **WhisperEngine测试**
   - 模型加载和释放
   - 音频数据转录准确性
   - 错误情况处理

2. **AudioRecorderManager测试**
   - 录音开始/停止功能
   - 音频格式设置
   - 权限检查

3. **TranscriptionRepository测试**
   - 数据库CRUD操作
   - 搜索功能
   - 数据完整性

### 集成测试

1. **端到端转录流程测试**
   - 录音 → 转录 → 保存完整流程
   - 实时转录功能
   - 离线模式切换

2. **性能测试**
   - 转录速度测试
   - 内存使用监控
   - 电池消耗测试

### UI测试

1. **用户界面测试**
   - 录音按钮响应
   - 转录结果显示
   - 历史记录管理

2. **用户体验测试**
   - 应用启动时间
   - 界面响应速度
   - 错误提示友好性

### 兼容性测试

1. **设备兼容性**
   - 不同Android版本测试
   - 不同屏幕尺寸适配
   - 不同硬件配置测试

2. **音频兼容性**
   - 不同麦克风质量
   - 环境噪音影响
   - 不同说话速度和音调

## 技术实现细节

### Whisper模型集成

1. **模型文件管理**
   - 使用ONNX Runtime for Android集成Whisper模型
   - 模型文件存储在应用私有目录
   - 支持模型文件压缩和增量更新

2. **JNI接口设计**
   - 创建native方法调用Whisper C++库
   - 音频数据格式转换
   - 内存管理和资源释放

3. **性能优化**
   - 使用多线程处理音频转录
   - 音频数据缓冲和批处理
   - GPU加速支持（如果设备支持）

### 实时转录实现

1. **音频流处理**
   - 使用AudioRecord API捕获音频
   - 音频数据分块处理
   - VAD（语音活动检测）集成

2. **流式转录**
   - 滑动窗口音频处理
   - 部分结果实时更新
   - 句子边界检测

### 离线模式实现

1. **模型本地化**
   - 应用安装时预置基础模型
   - 支持用户下载更大的高精度模型
   - 模型版本管理和更新

2. **数据同步**
   - 离线转录结果本地存储
   - 网络恢复后可选择云端备份
   - 冲突解决机制