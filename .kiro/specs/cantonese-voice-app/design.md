# 设计文档

## 概述

本项目是一个基于React Native的Android移动应用，集成OpenAI Whisper粤语微调模型，实现实时语音识别和文本转换。应用采用模块化架构设计，支持离线运行，确保在无网络环境下也能提供高质量的粤语语音识别服务。

## 技术栈选择

### 前端框架
- **React Native 0.72+**: 最新稳定版本，提供优秀的跨平台开发体验
  - 理由：成熟的生态系统，丰富的第三方库支持
  - 替代方案：Flutter（学习曲线较陡）、原生Android开发（开发效率较低）

### 语音处理
- **react-native-audio-recorder-player**: 音频录制和播放
- **@react-native-async-storage/async-storage**: 本地数据存储
- **react-native-fs**: 文件系统操作

### AI模型集成
- **Whisper.cpp**: C++实现的Whisper模型，支持移动端部署
- **react-native-whisper**: React Native的Whisper绑定库
- **粤语微调模型**: 基于OpenAI Whisper-large-v2的粤语优化版本

### 状态管理
- **React Context + useReducer**: 轻量级状态管理
- **React Query**: 数据缓存和同步

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────┐
│              用户界面层                    │
│  ┌─────────────┐  ┌─────────────────┐    │
│  │  录音界面    │  │   历史记录界面   │    │
│  └─────────────┘  └─────────────────┘    │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│              业务逻辑层                    │
│  ┌─────────────┐  ┌─────────────────┐    │
│  │ 语音管理器   │  │   文本处理器     │    │
│  └─────────────┘  └─────────────────┘    │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│              数据访问层                    │
│  ┌─────────────┐  ┌─────────────────┐    │
│  │ 音频处理器   │  │   存储管理器     │    │
│  └─────────────┘  └─────────────────┘    │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│              AI模型层                     │
│  ┌─────────────┐  ┌─────────────────┐    │
│  │ Whisper引擎  │  │   模型加载器     │    │
│  └─────────────┘  └─────────────────┘    │
└─────────────────────────────────────────┘
```

## 组件和接口

### 核心组件

#### 1. 语音录制组件 (VoiceRecorder)
```typescript
interface VoiceRecorderProps {
  onRecordingStart: () => void;
  onRecordingStop: (audioPath: string) => void;
  onError: (error: Error) => void;
}

interface AudioConfig {
  sampleRate: 16000;
  channels: 1;
  bitsPerSample: 16;
  format: 'wav';
}
```

#### 2. 语音识别引擎 (WhisperEngine)
```typescript
interface WhisperEngine {
  initialize(modelPath: string): Promise<void>;
  transcribe(audioPath: string): Promise<TranscriptionResult>;
  isModelLoaded(): boolean;
  getModelInfo(): ModelInfo;
}

interface TranscriptionResult {
  text: string;
  confidence: number;
  segments: TranscriptionSegment[];
  processingTime: number;
}
```

#### 3. 存储管理器 (StorageManager)
```typescript
interface StorageManager {
  saveTranscription(transcription: TranscriptionRecord): Promise<string>;
  getTranscriptions(limit?: number): Promise<TranscriptionRecord[]>;
  deleteTranscription(id: string): Promise<void>;
  clearAllTranscriptions(): Promise<void>;
}

interface TranscriptionRecord {
  id: string;
  text: string;
  audioPath?: string;
  timestamp: Date;
  confidence: number;
  duration: number;
}
```

### 项目目录结构

```
cantonese-voice-app/
├── android/                     # Android原生代码
│   ├── app/
│   │   ├── src/main/assets/     # Whisper模型文件
│   │   └── build.gradle
│   └── gradle.properties
├── ios/                         # iOS代码（预留）
├── src/                         # React Native源代码
│   ├── components/              # 可复用组件
│   │   ├── VoiceRecorder/       # 语音录制组件
│   │   ├── TranscriptionList/   # 转录列表组件
│   │   ├── AudioWaveform/       # 音频波形显示
│   │   └── LoadingSpinner/      # 加载指示器
│   ├── screens/                 # 页面组件
│   │   ├── HomeScreen/          # 主页面
│   │   ├── HistoryScreen/       # 历史记录页面
│   │   └── SettingsScreen/      # 设置页面
│   ├── services/                # 业务服务
│   │   ├── WhisperService/      # Whisper集成服务
│   │   ├── AudioService/        # 音频处理服务
│   │   ├── StorageService/      # 数据存储服务
│   │   └── PermissionService/   # 权限管理服务
│   ├── utils/                   # 工具函数
│   │   ├── audioUtils.ts        # 音频处理工具
│   │   ├── fileUtils.ts         # 文件操作工具
│   │   └── constants.ts         # 常量定义
│   ├── types/                   # TypeScript类型定义
│   │   ├── audio.ts             # 音频相关类型
│   │   ├── transcription.ts     # 转录相关类型
│   │   └── navigation.ts        # 导航类型
│   ├── hooks/                   # 自定义Hooks
│   │   ├── useVoiceRecorder.ts  # 语音录制Hook
│   │   ├── useWhisper.ts        # Whisper使用Hook
│   │   └── useStorage.ts        # 存储Hook
│   ├── context/                 # React Context
│   │   ├── AppContext.tsx       # 应用全局状态
│   │   └── ThemeContext.tsx     # 主题上下文
│   └── App.tsx                  # 应用入口
├── assets/                      # 静态资源
│   ├── models/                  # AI模型文件
│   │   └── whisper-cantonese.bin
│   ├── images/                  # 图片资源
│   └── fonts/                   # 字体文件
├── docs/                        # 项目文档
│   ├── architecture.md          # 架构设计文档
│   ├── api.md                   # API接口文档
│   ├── deployment.md            # 部署指南
│   └── development.md           # 开发指南
├── scripts/                     # 构建脚本
│   ├── download-model.js        # 模型下载脚本
│   └── build-android.js         # Android构建脚本
├── __tests__/                   # 测试文件
├── package.json                 # 项目依赖
├── metro.config.js              # Metro配置
├── babel.config.js              # Babel配置
├── tsconfig.json                # TypeScript配置
└── README.md                    # 项目说明
```

## 数据模型

### 转录记录模型
```typescript
interface TranscriptionRecord {
  id: string;                    // 唯一标识符
  text: string;                  // 转录文本
  originalAudioPath?: string;    // 原始音频文件路径
  confidence: number;            // 识别置信度 (0-1)
  duration: number;              // 音频时长（秒）
  timestamp: Date;               // 创建时间
  language: 'yue';               // 语言标识（粤语）
  segments: TranscriptionSegment[]; // 分段信息
}

interface TranscriptionSegment {
  start: number;                 // 开始时间（秒）
  end: number;                   // 结束时间（秒）
  text: string;                  // 分段文本
  confidence: number;            // 分段置信度
}
```

### 应用状态模型
```typescript
interface AppState {
  isRecording: boolean;          // 录音状态
  isProcessing: boolean;         // 处理状态
  currentTranscription?: string; // 当前转录结果
  transcriptionHistory: TranscriptionRecord[]; // 历史记录
  modelStatus: ModelStatus;      // 模型状态
  permissions: PermissionStatus; // 权限状态
}

enum ModelStatus {
  NOT_LOADED = 'not_loaded',
  LOADING = 'loading',
  READY = 'ready',
  ERROR = 'error'
}
```

## 错误处理

### 错误类型定义
```typescript
enum ErrorType {
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  MODEL_LOAD_FAILED = 'MODEL_LOAD_FAILED',
  AUDIO_RECORDING_FAILED = 'AUDIO_RECORDING_FAILED',
  TRANSCRIPTION_FAILED = 'TRANSCRIPTION_FAILED',
  STORAGE_ERROR = 'STORAGE_ERROR',
  NETWORK_ERROR = 'NETWORK_ERROR'
}

interface AppError {
  type: ErrorType;
  message: string;
  details?: any;
  timestamp: Date;
}
```

### 错误处理策略
1. **权限错误**: 引导用户到设置页面开启权限
2. **模型加载错误**: 提供重新下载模型选项
3. **录音错误**: 检查设备音频功能，提供故障排除建议
4. **转录错误**: 提供重试机制，记录错误日志
5. **存储错误**: 检查存储空间，提供清理选项

## 测试策略

### 单元测试
- 音频处理工具函数测试
- 数据存储服务测试
- Whisper服务集成测试
- 自定义Hooks测试

### 集成测试
- 录音到转录的完整流程测试
- 离线模式功能测试
- 历史记录管理测试

### 端到端测试
- 用户录音和查看结果的完整流程
- 不同音频质量下的识别准确性测试
- 长时间录音的性能测试

### 性能测试
- 模型加载时间测试
- 内存使用监控
- 电池消耗测试
- 不同设备兼容性测试

## 安全考虑

### 数据隐私
- 音频数据仅存储在本地设备
- 不向外部服务器传输语音数据
- 提供数据清理功能

### 权限管理
- 最小权限原则
- 运行时权限请求
- 权限状态监控

### 模型安全
- 模型文件完整性验证
- 防止模型文件被篡改
- 安全的模型更新机制