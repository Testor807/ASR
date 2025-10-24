# 实施计划

## 开发环境要求
- Node.js 18+ 
- React Native CLI 12+
- Android Studio 2023.1+
- JDK 17
- Android SDK API 33+

## 预估开发时间
- 总计：6-8周
- 核心功能（任务1-6）：4-5周
- 优化和部署（任务7-8）：2-3周

---

- [ ] 1. 项目初始化和环境配置 **[预估：3-4天]**
  - 创建React Native项目结构，配置TypeScript和必要的开发工具
  - 配置Android构建环境，设置Gradle和依赖管理
  - 安装和配置音频处理相关的第三方库
  - 设置开发环境的调试和热重载功能
  - _需求: 1.1, 4.1_

- [ ] 1.1 创建基础项目结构 **[预估：1天]**
  - **命令执行**：
    ```bash
    npx react-native@latest init CantoneseVoiceApp --template react-native-template-typescript
    cd CantoneseVoiceApp
    ```
  - **目录创建**：
    ```bash
    mkdir -p src/{components,screens,services,utils,types,hooks,context}
    mkdir -p src/components/{VoiceRecorder,TranscriptionList,AudioWaveform,LoadingSpinner}
    mkdir -p src/screens/{HomeScreen,HistoryScreen,SettingsScreen}
    mkdir -p src/services/{WhisperService,AudioService,StorageService,PermissionService}
    mkdir -p assets/{models,images,fonts}
    mkdir -p docs
    ```
  - **配置文件设置**：
    - 创建 `.eslintrc.js`、`.prettierrc`
    - 配置 `tsconfig.json` 路径映射
    - 设置 `metro.config.js` 资源处理
  - _需求: 4.1_

- [ ] 1.2 配置Android原生环境 **[预估：1天]**
  - **Android Gradle配置** (`android/app/build.gradle`)：
    ```gradle
    android {
        compileSdkVersion 33
        buildToolsVersion "33.0.0"
        
        defaultConfig {
            minSdkVersion 24  // 支持Whisper.cpp
            targetSdkVersion 33
            ndk {
                abiFilters "arm64-v8a", "armeabi-v7a"
            }
        }
        
        packagingOptions {
            pickFirst '**/libc++_shared.so'
            pickFirst '**/libjsc.so'
        }
    }
    ```
  - **权限配置** (`android/app/src/main/AndroidManifest.xml`)：
    ```xml
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />
    ```
  - **ProGuard规则** (`android/app/proguard-rules.pro`)：
    ```
    -keep class com.whisper.** { *; }
    -keep class ai.onnxruntime.** { *; }
    -dontwarn com.whisper.**
    ```
  - **资源文件夹**：创建 `android/app/src/main/assets/models/`
  - _需求: 1.1, 2.2_

- [ ] 1.3 安装核心依赖库 **[预估：1天]**
  - **依赖安装命令**：
    ```bash
    npm install react-native-audio-recorder-player
    npm install @react-native-async-storage/async-storage
    npm install react-native-fs
    npm install react-native-permissions
    npm install react-native-sound
    npm install @react-navigation/native @react-navigation/stack
    npm install react-native-vector-icons
    npm install react-native-svg
    ```
  - **iOS配置** (如需要)：
    ```bash
    cd ios && pod install
    ```
  - **权限配置**：
    ```typescript
    // src/utils/permissions.ts
    import {PermissionsAndroid, Platform} from 'react-native';
    
    export const requestAudioPermission = async () => {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );
        return granted === PermissionsAndroid.RESULTS.GRANTED;
      }
      return true;
    };
    ```
  - _需求: 1.1, 5.1_

- [ ] 2. Whisper模型集成和AI引擎开发 **[预估：5-7天]**
  - 集成Whisper.cpp库到React Native项目中
  - 下载和配置粤语微调的Whisper模型文件
  - 实现模型加载和初始化功能
  - 开发语音转文本的核心处理逻辑
  - _需求: 1.2, 2.1, 3.1_

- [ ] 2.1 集成Whisper.cpp原生库 **[预估：2-3天]**
  - **下载Whisper.cpp源码**：
    ```bash
    git clone https://github.com/ggerganov/whisper.cpp.git
    cd whisper.cpp
    git checkout v1.5.4  # 使用稳定版本
    ```
  - **Android CMake配置** (`android/app/src/main/cpp/CMakeLists.txt`)：
    ```cmake
    cmake_minimum_required(VERSION 3.18.1)
    project("cantonesewhisper")
    
    # 添加Whisper.cpp源文件
    add_subdirectory(whisper.cpp)
    
    # 创建JNI库
    add_library(cantonesewhisper SHARED
        whisper_jni.cpp
        whisper.cpp/whisper.cpp
        whisper.cpp/ggml.c
    )
    
    target_link_libraries(cantonesewhisper
        android
        log
    )
    ```
  - **JNI接口实现** (`android/app/src/main/cpp/whisper_jni.cpp`)：
    ```cpp
    #include <jni.h>
    #include <android/log.h>
    #include "whisper.h"
    
    extern "C" JNIEXPORT jlong JNICALL
    Java_com_cantonesewhisper_WhisperModule_initContext(
        JNIEnv *env, jobject thiz, jstring model_path) {
        
        const char *path = env->GetStringUTFChars(model_path, 0);
        struct whisper_context *ctx = whisper_init_from_file(path);
        env->ReleaseStringUTFChars(model_path, path);
        
        return reinterpret_cast<jlong>(ctx);
    }
    
    extern "C" JNIEXPORT jstring JNICALL
    Java_com_cantonesewhisper_WhisperModule_transcribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jfloatArray audio_data) {
        
        struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
        
        // 音频数据处理和转录逻辑
        // ...
        
        return env->NewStringUTF(transcribed_text.c_str());
    }
    ```
  - **Gradle配置更新** (`android/app/build.gradle`)：
    ```gradle
    android {
        externalNativeBuild {
            cmake {
                path "src/main/cpp/CMakeLists.txt"
                version "3.18.1"
            }
        }
    }
    ```
  - _需求: 2.1, 3.1_

- [ ] 2.2 实现WhisperService核心服务 **[预估：2天]**
  - **创建WhisperService类** (`src/services/WhisperService/index.ts`)：
    ```typescript
    import { NativeModules } from 'react-native';
    
    interface WhisperModule {
      initContext(modelPath: string): Promise<number>;
      transcribe(contextPtr: number, audioData: number[]): Promise<string>;
      releaseContext(contextPtr: number): Promise<void>;
    }
    
    const { WhisperModule } = NativeModules;
    
    export class WhisperService {
      private contextPtr: number | null = null;
      private isInitialized = false;
      
      async initialize(modelPath: string): Promise<void> {
        try {
          this.contextPtr = await WhisperModule.initContext(modelPath);
          this.isInitialized = true;
        } catch (error) {
          throw new Error(`模型初始化失败: ${error.message}`);
        }
      }
      
      async transcribe(audioData: Float32Array): Promise<TranscriptionResult> {
        if (!this.isInitialized || !this.contextPtr) {
          throw new Error('Whisper模型未初始化');
        }
        
        const startTime = Date.now();
        const audioArray = Array.from(audioData);
        
        try {
          const text = await WhisperModule.transcribe(this.contextPtr, audioArray);
          const processingTime = Date.now() - startTime;
          
          return {
            text: text.trim(),
            confidence: this.calculateConfidence(text),
            processingTime,
            segments: this.parseSegments(text)
          };
        } catch (error) {
          throw new Error(`转录失败: ${error.message}`);
        }
      }
      
      private calculateConfidence(text: string): number {
        // 基于文本长度和特征计算置信度
        return Math.min(0.95, Math.max(0.1, text.length / 100));
      }
      
      private parseSegments(text: string): TranscriptionSegment[] {
        // 解析分段信息
        return [{
          start: 0,
          end: 0,
          text,
          confidence: this.calculateConfidence(text)
        }];
      }
    }
    ```
  - **音频预处理工具** (`src/utils/audioUtils.ts`)：
    ```typescript
    export const convertToFloat32Array = (audioBuffer: ArrayBuffer): Float32Array => {
      const int16Array = new Int16Array(audioBuffer);
      const float32Array = new Float32Array(int16Array.length);
      
      for (let i = 0; i < int16Array.length; i++) {
        float32Array[i] = int16Array[i] / 32768.0; // 归一化到[-1, 1]
      }
      
      return float32Array;
    };
    
    export const resampleAudio = (
      audioData: Float32Array, 
      originalSampleRate: number, 
      targetSampleRate: number = 16000
    ): Float32Array => {
      if (originalSampleRate === targetSampleRate) {
        return audioData;
      }
      
      const ratio = originalSampleRate / targetSampleRate;
      const newLength = Math.round(audioData.length / ratio);
      const result = new Float32Array(newLength);
      
      for (let i = 0; i < newLength; i++) {
        const srcIndex = Math.round(i * ratio);
        result[i] = audioData[srcIndex] || 0;
      }
      
      return result;
    };
    ```
  - _需求: 1.2, 3.1, 3.2_

- [ ] 2.3 配置粤语模型文件 **[预估：1天]**
  - **模型下载脚本** (`scripts/download-model.js`)：
    ```javascript
    const fs = require('fs');
    const https = require('https');
    const path = require('path');
    
    const MODEL_URL = 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2.bin';
    const MODEL_PATH = path.join(__dirname, '../android/app/src/main/assets/models/whisper-cantonese.bin');
    
    async function downloadModel() {
      console.log('开始下载粤语Whisper模型...');
      
      const file = fs.createWriteStream(MODEL_PATH);
      
      https.get(MODEL_URL, (response) => {
        const totalSize = parseInt(response.headers['content-length'], 10);
        let downloadedSize = 0;
        
        response.on('data', (chunk) => {
          downloadedSize += chunk.length;
          const progress = ((downloadedSize / totalSize) * 100).toFixed(2);
          process.stdout.write(`\r下载进度: ${progress}%`);
        });
        
        response.pipe(file);
        
        file.on('finish', () => {
          file.close();
          console.log('\n模型下载完成！');
          verifyModel();
        });
      });
    }
    
    function verifyModel() {
      const stats = fs.statSync(MODEL_PATH);
      console.log(`模型文件大小: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
      
      // 简单的文件完整性检查
      if (stats.size > 1000000) { // 至少1MB
        console.log('模型文件验证通过');
      } else {
        console.error('模型文件可能损坏');
      }
    }
    
    downloadModel();
    ```
  - **模型管理服务** (`src/services/ModelManager.ts`)：
    ```typescript
    import RNFS from 'react-native-fs';
    import { Platform } from 'react-native';
    
    export class ModelManager {
      private static readonly MODEL_NAME = 'whisper-cantonese.bin';
      private static readonly MODEL_VERSION = '1.0.0';
      
      static async getModelPath(): Promise<string> {
        if (Platform.OS === 'android') {
          return `${RNFS.MainBundlePath}/assets/models/${this.MODEL_NAME}`;
        }
        return `${RNFS.MainBundlePath}/models/${this.MODEL_NAME}`;
      }
      
      static async isModelAvailable(): Promise<boolean> {
        const modelPath = await this.getModelPath();
        return RNFS.exists(modelPath);
      }
      
      static async getModelInfo(): Promise<ModelInfo> {
        const modelPath = await this.getModelPath();
        const stats = await RNFS.stat(modelPath);
        
        return {
          name: this.MODEL_NAME,
          version: this.MODEL_VERSION,
          size: stats.size,
          path: modelPath,
          lastModified: new Date(stats.mtime)
        };
      }
    }
    ```
  - **package.json脚本添加**：
    ```json
    {
      "scripts": {
        "download-model": "node scripts/download-model.js",
        "verify-model": "node scripts/verify-model.js"
      }
    }
    ```
  - _需求: 3.1, 3.2_

- [ ]* 2.4 编写AI引擎单元测试
  - 创建WhisperService的单元测试用例
  - 测试模型加载和初始化功能
  - 验证音频预处理和转录准确性
  - 测试错误处理和异常情况
  - _需求: 3.1, 3.2_

- [ ] 3. 音频录制和处理功能开发 **[预估：4-5天]**
  - 实现实时音频录制功能
  - 开发音频格式转换和预处理逻辑
  - 创建音频质量检测和优化功能
  - 实现录音状态管理和用户反馈
  - _需求: 1.1, 1.3, 4.3_

- [ ] 3.1 开发VoiceRecorder组件 **[预估：2天]**
  - **VoiceRecorder组件实现** (`src/components/VoiceRecorder/index.tsx`)：
    ```typescript
    import React, { useState, useEffect, useRef } from 'react';
    import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
    import AudioRecorderPlayer from 'react-native-audio-recorder-player';
    import { requestAudioPermission } from '../../utils/permissions';
    
    interface VoiceRecorderProps {
      onRecordingStart: () => void;
      onRecordingStop: (audioPath: string, duration: number) => void;
      onError: (error: Error) => void;
      maxDuration?: number; // 最大录音时长（秒）
    }
    
    export const VoiceRecorder: React.FC<VoiceRecorderProps> = ({
      onRecordingStart,
      onRecordingStop,
      onError,
      maxDuration = 300 // 默认5分钟
    }) => {
      const [isRecording, setIsRecording] = useState(false);
      const [recordTime, setRecordTime] = useState(0);
      const [audioLevels, setAudioLevels] = useState<number[]>([]);
      
      const audioRecorderPlayer = useRef(new AudioRecorderPlayer()).current;
      const recordingPath = useRef<string>('');
      
      const startRecording = async () => {
        try {
          const hasPermission = await requestAudioPermission();
          if (!hasPermission) {
            throw new Error('需要麦克风权限才能录音');
          }
          
          const path = `${RNFS.CachesDirectoryPath}/recording_${Date.now()}.wav`;
          recordingPath.current = path;
          
          const audioSet = {
            AudioEncoderAndroid: AudioEncoderAndroidType.AAC,
            AudioSourceAndroid: AudioSourceAndroidType.MIC,
            AVEncoderAudioQualityKeyIOS: AVEncoderAudioQualityIOSType.high,
            AVNumberOfChannelsKeyIOS: 1,
            AVFormatIDKeyIOS: AVEncodingOption.aac,
          };
          
          await audioRecorderPlayer.startRecorder(path, audioSet);
          
          audioRecorderPlayer.addRecordBackListener((e) => {
            setRecordTime(Math.floor(e.currentPosition / 1000));
            
            // 添加音频电平监控
            const level = e.currentMetering || 0;
            setAudioLevels(prev => [...prev.slice(-50), level]); // 保留最近50个采样
            
            // 检查最大时长
            if (e.currentPosition >= maxDuration * 1000) {
              stopRecording();
            }
          });
          
          setIsRecording(true);
          onRecordingStart();
          
        } catch (error) {
          onError(new Error(`录音启动失败: ${error.message}`));
        }
      };
      
      const stopRecording = async () => {
        try {
          const result = await audioRecorderPlayer.stopRecorder();
          audioRecorderPlayer.removeRecordBackListener();
          
          setIsRecording(false);
          setRecordTime(0);
          setAudioLevels([]);
          
          onRecordingStop(recordingPath.current, recordTime);
          
        } catch (error) {
          onError(new Error(`录音停止失败: ${error.message}`));
        }
      };
      
      const formatTime = (seconds: number): string => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
      };
      
      return (
        <View style={styles.container}>
          {/* 音频波形显示 */}
          <View style={styles.waveformContainer}>
            {audioLevels.map((level, index) => (
              <View
                key={index}
                style={[
                  styles.waveformBar,
                  { height: Math.max(2, Math.abs(level) * 50) }
                ]}
              />
            ))}
          </View>
          
          {/* 录音时间显示 */}
          <Text style={styles.timeText}>{formatTime(recordTime)}</Text>
          
          {/* 录音按钮 */}
          <TouchableOpacity
            style={[styles.recordButton, isRecording && styles.recordingButton]}
            onPress={isRecording ? stopRecording : startRecording}
          >
            <Text style={styles.buttonText}>
              {isRecording ? '停止录音' : '开始录音'}
            </Text>
          </TouchableOpacity>
        </View>
      );
    };
    
    const styles = StyleSheet.create({
      container: {
        alignItems: 'center',
        padding: 20,
      },
      waveformContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        height: 60,
        marginBottom: 20,
      },
      waveformBar: {
        width: 3,
        backgroundColor: '#007AFF',
        marginHorizontal: 1,
        borderRadius: 1.5,
      },
      timeText: {
        fontSize: 24,
        fontWeight: 'bold',
        marginBottom: 20,
        color: '#333',
      },
      recordButton: {
        backgroundColor: '#007AFF',
        paddingHorizontal: 30,
        paddingVertical: 15,
        borderRadius: 25,
      },
      recordingButton: {
        backgroundColor: '#FF3B30',
      },
      buttonText: {
        color: 'white',
        fontSize: 16,
        fontWeight: 'bold',
      },
    });
    ```
  - **自定义Hook** (`src/hooks/useVoiceRecorder.ts`)：
    ```typescript
    import { useState, useCallback } from 'react';
    import { VoiceRecorder } from '../components/VoiceRecorder';
    
    export const useVoiceRecorder = () => {
      const [isRecording, setIsRecording] = useState(false);
      const [currentRecording, setCurrentRecording] = useState<string | null>(null);
      const [error, setError] = useState<Error | null>(null);
      
      const handleRecordingStart = useCallback(() => {
        setIsRecording(true);
        setError(null);
      }, []);
      
      const handleRecordingStop = useCallback((audioPath: string, duration: number) => {
        setIsRecording(false);
        setCurrentRecording(audioPath);
      }, []);
      
      const handleError = useCallback((error: Error) => {
        setError(error);
        setIsRecording(false);
      }, []);
      
      return {
        isRecording,
        currentRecording,
        error,
        handleRecordingStart,
        handleRecordingStop,
        handleError,
      };
    };
    ```
  - _需求: 1.1, 1.3, 4.3_

- [ ] 3.2 实现AudioService音频处理服务
  - 创建音频文件格式转换功能（支持WAV、MP3等）
  - 实现音频降噪和质量优化算法
  - 开发音频分段和批处理功能
  - 添加音频元数据提取和管理功能
  - _需求: 1.2, 3.3_

- [ ] 3.3 集成权限管理系统
  - 实现麦克风权限的动态请求功能
  - 创建存储权限管理和文件访问控制
  - 开发权限状态监控和用户引导功能
  - 实现权限被拒绝时的降级处理方案
  - _需求: 1.1, 4.2_

- [ ]* 3.4 编写音频处理测试
  - 创建音频录制功能的集成测试
  - 测试不同音频格式的转换准确性
  - 验证权限管理和错误处理逻辑
  - 测试音频质量优化效果
  - _需求: 1.1, 3.3_

- [ ] 4. 用户界面和交互开发 **[预估：4-5天]**
  - 设计和实现主录音界面
  - 开发转录结果显示和编辑功能
  - 创建历史记录列表和管理界面
  - 实现应用设置和配置页面
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4.1 创建HomeScreen主界面 **[预估：2天]**
  - **主界面实现** (`src/screens/HomeScreen/index.tsx`)：
    ```typescript
    import React, { useState, useEffect } from 'react';
    import {
      View,
      Text,
      StyleSheet,
      ScrollView,
      TouchableOpacity,
      Alert,
      Clipboard,
      Share,
    } from 'react-native';
    import { VoiceRecorder } from '../../components/VoiceRecorder';
    import { WhisperService } from '../../services/WhisperService';
    import { StorageService } from '../../services/StorageService';
    import { LoadingSpinner } from '../../components/LoadingSpinner';
    
    export const HomeScreen: React.FC = () => {
      const [transcriptionText, setTranscriptionText] = useState<string>('');
      const [isProcessing, setIsProcessing] = useState(false);
      const [whisperService] = useState(() => new WhisperService());
      const [storageService] = useState(() => new StorageService());
      
      useEffect(() => {
        initializeWhisper();
      }, []);
      
      const initializeWhisper = async () => {
        try {
          setIsProcessing(true);
          await whisperService.initialize();
        } catch (error) {
          Alert.alert('初始化错误', `Whisper模型加载失败: ${error.message}`);
        } finally {
          setIsProcessing(false);
        }
      };
      
      const handleRecordingStop = async (audioPath: string, duration: number) => {
        try {
          setIsProcessing(true);
          setTranscriptionText('正在转录中...');
          
          // 音频预处理
          const audioData = await processAudioFile(audioPath);
          
          // 语音转录
          const result = await whisperService.transcribe(audioData);
          setTranscriptionText(result.text);
          
          // 保存到历史记录
          await storageService.saveTranscription({
            id: Date.now().toString(),
            text: result.text,
            audioPath,
            timestamp: new Date(),
            confidence: result.confidence,
            duration,
          });
          
        } catch (error) {
          Alert.alert('转录错误', `语音转录失败: ${error.message}`);
          setTranscriptionText('');
        } finally {
          setIsProcessing(false);
        }
      };
      
      const processAudioFile = async (audioPath: string): Promise<Float32Array> => {
        // 这里实现音频文件读取和预处理
        // 返回适合Whisper处理的Float32Array格式
        const audioBuffer = await RNFS.readFile(audioPath, 'base64');
        // 转换逻辑...
        return new Float32Array([]); // 占位符
      };
      
      const handleCopyText = () => {
        if (transcriptionText) {
          Clipboard.setString(transcriptionText);
          Alert.alert('已复制', '转录文本已复制到剪贴板');
        }
      };
      
      const handleShareText = async () => {
        if (transcriptionText) {
          try {
            await Share.share({
              message: transcriptionText,
              title: '语音转录结果',
            });
          } catch (error) {
            Alert.alert('分享失败', error.message);
          }
        }
      };
      
      const handleClearText = () => {
        setTranscriptionText('');
      };
      
      return (
        <View style={styles.container}>
          {/* 标题栏 */}
          <View style={styles.header}>
            <Text style={styles.title}>粤语语音识别</Text>
            <Text style={styles.subtitle}>点击按钮开始录音</Text>
          </View>
          
          {/* 录音组件 */}
          <VoiceRecorder
            onRecordingStart={() => setTranscriptionText('')}
            onRecordingStop={handleRecordingStop}
            onError={(error) => Alert.alert('录音错误', error.message)}
            maxDuration={300}
          />
          
          {/* 处理状态指示器 */}
          {isProcessing && (
            <View style={styles.processingContainer}>
              <LoadingSpinner />
              <Text style={styles.processingText}>正在处理中...</Text>
            </View>
          )}
          
          {/* 转录结果显示区域 */}
          <ScrollView style={styles.transcriptionContainer}>
            <Text style={styles.transcriptionText}>
              {transcriptionText || '转录结果将在这里显示'}
            </Text>
          </ScrollView>
          
          {/* 操作按钮 */}
          {transcriptionText && !isProcessing && (
            <View style={styles.actionButtons}>
              <TouchableOpacity style={styles.actionButton} onPress={handleCopyText}>
                <Text style={styles.actionButtonText}>复制</Text>
              </TouchableOpacity>
              
              <TouchableOpacity style={styles.actionButton} onPress={handleShareText}>
                <Text style={styles.actionButtonText}>分享</Text>
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={[styles.actionButton, styles.clearButton]} 
                onPress={handleClearText}
              >
                <Text style={styles.actionButtonText}>清除</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      );
    };
    
    const styles = StyleSheet.create({
      container: {
        flex: 1,
        backgroundColor: '#f5f5f5',
      },
      header: {
        alignItems: 'center',
        paddingTop: 60,
        paddingBottom: 20,
        backgroundColor: 'white',
      },
      title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#333',
        marginBottom: 5,
      },
      subtitle: {
        fontSize: 16,
        color: '#666',
      },
      processingContainer: {
        alignItems: 'center',
        padding: 20,
      },
      processingText: {
        marginTop: 10,
        fontSize: 16,
        color: '#666',
      },
      transcriptionContainer: {
        flex: 1,
        margin: 20,
        padding: 15,
        backgroundColor: 'white',
        borderRadius: 10,
        elevation: 2,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
      },
      transcriptionText: {
        fontSize: 16,
        lineHeight: 24,
        color: '#333',
      },
      actionButtons: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        padding: 20,
        backgroundColor: 'white',
      },
      actionButton: {
        backgroundColor: '#007AFF',
        paddingHorizontal: 20,
        paddingVertical: 10,
        borderRadius: 8,
        minWidth: 80,
        alignItems: 'center',
      },
      clearButton: {
        backgroundColor: '#FF3B30',
      },
      actionButtonText: {
        color: 'white',
        fontSize: 16,
        fontWeight: '600',
      },
    });
    ```
  - **导航配置** (`src/navigation/AppNavigator.tsx`)：
    ```typescript
    import React from 'react';
    import { NavigationContainer } from '@react-navigation/native';
    import { createStackNavigator } from '@react-navigation/stack';
    import { HomeScreen } from '../screens/HomeScreen';
    import { HistoryScreen } from '../screens/HistoryScreen';
    import { SettingsScreen } from '../screens/SettingsScreen';
    
    const Stack = createStackNavigator();
    
    export const AppNavigator: React.FC = () => {
      return (
        <NavigationContainer>
          <Stack.Navigator
            initialRouteName="Home"
            screenOptions={{
              headerStyle: {
                backgroundColor: '#007AFF',
              },
              headerTintColor: '#fff',
              headerTitleStyle: {
                fontWeight: 'bold',
              },
            }}
          >
            <Stack.Screen 
              name="Home" 
              component={HomeScreen} 
              options={{ title: '粤语语音识别' }}
            />
            <Stack.Screen 
              name="History" 
              component={HistoryScreen} 
              options={{ title: '历史记录' }}
            />
            <Stack.Screen 
              name="Settings" 
              component={SettingsScreen} 
              options={{ title: '设置' }}
            />
          </Stack.Navigator>
        </NavigationContainer>
      );
    };
    ```
  - _需求: 4.1, 4.2, 4.3, 4.4_

- [ ] 4.2 开发HistoryScreen历史记录界面
  - 创建转录历史的列表显示组件
  - 实现搜索和筛选历史记录功能
  - 添加历史记录的详细查看和编辑功能
  - 实现批量删除和导出历史记录功能
  - _需求: 5.1, 5.2, 5.3, 5.5_

- [ ] 4.3 实现TranscriptionList组件
  - 创建可复用的转录记录列表组件
  - 实现虚拟滚动优化大量数据显示
  - 添加下拉刷新和上拉加载更多功能
  - 实现列表项的滑动操作（删除、分享）
  - _需求: 5.2, 5.3, 5.5_

- [ ] 4.4 开发设置和配置界面
  - 创建应用设置页面，包含音频质量配置
  - 实现模型管理和更新功能界面
  - 添加数据管理选项（清理缓存、导出数据）
  - 创建关于页面和用户帮助文档
  - _需求: 4.2, 5.4_

- [ ]* 4.5 编写UI组件测试
  - 创建主要UI组件的快照测试
  - 测试用户交互和状态变化
  - 验证界面响应性和适配性
  - 测试无障碍功能和可用性
  - _需求: 4.1, 4.2, 4.3, 4.4_

- [ ] 5. 数据存储和历史记录管理
  - 实现本地数据存储和检索功能
  - 开发转录历史的管理和组织功能
  - 创建数据备份和恢复机制
  - 实现存储空间优化和清理功能
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 5.1 开发StorageService存储服务
  - 创建基于AsyncStorage的数据持久化服务
  - 实现转录记录的CRUD操作接口
  - 开发数据索引和快速检索功能
  - 添加数据压缩和存储优化机制
  - _需求: 5.1, 5.4_

- [ ] 5.2 实现历史记录管理功能
  - 创建转录记录的分类和标签系统
  - 实现按时间、长度、置信度的排序功能
  - 开发搜索和过滤历史记录的算法
  - 添加收藏和重要记录标记功能
  - _需求: 5.2, 5.3_

- [ ] 5.3 开发数据导出和分享功能
  - 实现转录结果的文本格式导出
  - 创建批量导出和邮件分享功能
  - 开发数据备份到云存储的接口
  - 添加数据格式转换（TXT、JSON、CSV）功能
  - _需求: 4.5, 5.3_

- [ ]* 5.4 编写存储服务测试
  - 创建数据存储和检索的单元测试
  - 测试大量数据的性能和稳定性
  - 验证数据完整性和一致性
  - 测试存储空间管理和清理功能
  - _需求: 5.1, 5.4_

- [ ] 6. 离线功能和性能优化
  - 实现网络状态检测和离线模式切换
  - 优化模型加载和内存使用效率
  - 开发后台处理和任务队列功能
  - 实现应用性能监控和优化
  - _需求: 2.1, 2.2, 2.3_

- [ ] 6.1 实现离线模式管理
  - 创建网络状态监控和自动切换功能
  - 实现离线模式下的功能降级处理
  - 开发离线状态的用户界面指示
  - 添加离线数据同步和恢复机制
  - _需求: 2.1, 2.2, 2.3_

- [ ] 6.2 优化模型加载和性能
  - 实现模型的懒加载和预加载策略
  - 优化内存使用和垃圾回收机制
  - 开发模型缓存和版本管理功能
  - 添加性能监控和资源使用统计
  - _需求: 2.2, 3.1_

- [ ] 6.3 实现后台处理功能
  - 创建音频处理的后台任务队列
  - 实现长时间转录的分段处理
  - 开发任务进度跟踪和状态管理
  - 添加后台任务的暂停和恢复功能
  - _需求: 1.2, 2.1_

- [ ]* 6.4 编写性能测试
  - 创建模型加载时间的基准测试
  - 测试不同设备上的性能表现
  - 验证内存使用和电池消耗
  - 测试长时间运行的稳定性
  - _需求: 2.2, 3.1_

- [ ] 7. 错误处理和用户体验优化
  - 实现全局错误捕获和处理机制
  - 开发用户友好的错误提示和恢复功能
  - 创建应用崩溃报告和日志系统
  - 实现用户反馈和问题诊断功能
  - _需求: 1.4, 3.4, 4.1_

- [ ] 7.1 开发全局错误处理系统
  - 创建统一的错误类型定义和分类
  - 实现错误边界组件和异常捕获
  - 开发错误日志记录和上报功能
  - 添加错误恢复和重试机制
  - _需求: 1.4, 3.4_

- [ ] 7.2 实现用户引导和帮助系统
  - 创建首次使用的引导流程
  - 实现功能介绍和操作提示
  - 开发常见问题解答和帮助文档
  - 添加用户反馈和建议收集功能
  - _需求: 4.2, 4.1_

- [ ] 7.3 优化加载和响应体验
  - 实现智能预加载和缓存策略
  - 创建流畅的动画和过渡效果
  - 开发响应式布局和适配功能
  - 添加无障碍支持和可用性优化
  - _需求: 4.1, 4.3_

- [ ]* 7.4 编写用户体验测试
  - 创建用户交互流程的端到端测试
  - 测试错误处理和恢复机制
  - 验证无障碍功能和可用性
  - 测试不同场景下的用户体验
  - _需求: 4.1, 4.2, 4.3_

- [ ] 8. 应用打包和部署准备
  - 配置生产环境的构建脚本
  - 实现代码混淆和安全加固
  - 创建应用签名和发布配置
  - 准备应用商店发布材料
  - _需求: 所有需求的最终集成_

- [ ] 8.1 配置生产构建环境
  - 设置Release模式的Gradle构建配置
  - 配置代码混淆和资源压缩
  - 实现应用签名和密钥管理
  - 优化APK大小和启动性能
  - _需求: 所有需求的最终集成_

- [ ] 8.2 实现安全加固措施
  - 添加应用完整性验证功能
  - 实现敏感数据的加密存储
  - 配置网络安全和证书固定
  - 添加反调试和防篡改保护
  - _需求: 2.2, 5.1_

- [ ] 8.3 准备应用发布材料
  - 创建应用图标和启动画面
  - 编写应用描述和功能介绍
  - 准备应用截图和演示视频
  - 配置应用权限说明和隐私政策
  - _需求: 4.1, 4.2_

- [ ]* 8.4 执行最终测试验证 **[预估：2天]**
  - 进行完整的功能回归测试
  - 验证不同设备和系统版本的兼容性
  - 测试应用在各种网络环境下的表现
  - 执行安全测试和性能基准测试
  - _需求: 所有需求的综合验证_

---

## 开发指南和最佳实践

### 🚀 快速开始
1. **环境准备**：
   ```bash
   # 检查环境
   npx react-native doctor
   
   # 创建项目
   npx react-native@latest init CantoneseVoiceApp --template react-native-template-typescript
   
   # 下载模型
   npm run download-model
   ```

2. **开发流程**：
   - 按任务顺序逐步实施
   - 每完成一个主要任务后进行测试
   - 使用Git进行版本控制，每个任务创建一个分支

### 🔧 技术要点

#### Whisper模型优化
- **模型选择**：推荐使用`ggml-large-v2.bin`（约3GB）获得最佳精度
- **内存管理**：实现模型的懒加载，避免应用启动时加载
- **性能优化**：使用量化模型减少内存占用

#### 音频处理最佳实践
- **采样率**：统一使用16kHz，符合Whisper要求
- **格式转换**：录音使用WAV格式，避免压缩损失
- **降噪处理**：实现基础的音频预处理算法

#### 离线功能实现
- **网络检测**：使用`@react-native-netinfo/netinfo`监控网络状态
- **数据同步**：设计离线优先的数据存储策略
- **用户体验**：提供清晰的离线状态指示

### 🐛 常见问题解决

#### 1. 模型加载失败
```typescript
// 解决方案：检查模型文件路径和权限
const modelPath = Platform.select({
  android: `${RNFS.MainBundlePath}/assets/models/whisper-cantonese.bin`,
  ios: `${RNFS.MainBundlePath}/models/whisper-cantonese.bin`,
});

const exists = await RNFS.exists(modelPath);
if (!exists) {
  throw new Error('模型文件不存在，请重新下载');
}
```

#### 2. 音频权限问题
```typescript
// 解决方案：完善权限请求流程
const requestPermissions = async () => {
  const permissions = [
    PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
    PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
  ];
  
  const results = await PermissionsAndroid.requestMultiple(permissions);
  
  return Object.values(results).every(
    result => result === PermissionsAndroid.RESULTS.GRANTED
  );
};
```

#### 3. 内存溢出问题
```typescript
// 解决方案：实现内存监控和清理
const monitorMemory = () => {
  const memoryWarningListener = DeviceEventEmitter.addListener(
    'memoryWarning',
    () => {
      // 清理不必要的缓存
      clearAudioCache();
      // 释放未使用的模型资源
      whisperService.cleanup();
    }
  );
  
  return () => memoryWarningListener.remove();
};
```

### 📱 设备兼容性

#### 最低系统要求
- **Android**: API Level 24 (Android 7.0)
- **RAM**: 最少4GB，推荐6GB以上
- **存储**: 至少5GB可用空间（包含模型文件）
- **处理器**: ARM64架构，支持NEON指令集

#### 性能优化建议
- 在低端设备上使用较小的模型（如base或small版本）
- 实现自适应音频质量设置
- 提供性能模式选择（高精度vs快速模式）

### 🔒 安全考虑

#### 数据保护
- 所有语音数据仅存储在本地设备
- 实现数据加密存储
- 提供数据清理功能

#### 应用安全
- 启用代码混淆和资源保护
- 实现应用完整性检查
- 防止调试和逆向工程

### 📊 性能监控

#### 关键指标
- 模型加载时间：< 10秒
- 转录响应时间：< 实际音频时长的50%
- 内存使用：< 1GB（包含模型）
- 电池消耗：录音1小时 < 10%电量

#### 监控实现
```typescript
const performanceMonitor = {
  startTimer: (operation: string) => {
    const start = Date.now();
    return () => {
      const duration = Date.now() - start;
      console.log(`${operation} 耗时: ${duration}ms`);
      // 上报性能数据
    };
  },
  
  trackMemory: () => {
    // 监控内存使用情况
    const memoryInfo = performance.memory;
    console.log('内存使用:', memoryInfo);
  }
};
```

### 🚀 部署和发布

#### 构建优化
```bash
# Android Release构建
cd android
./gradlew assembleRelease

# 检查APK大小
ls -lh app/build/outputs/apk/release/
```

#### 发布检查清单
- [ ] 所有功能测试通过
- [ ] 性能指标达标
- [ ] 安全扫描无问题
- [ ] 不同设备兼容性验证
- [ ] 应用商店资料准备完成

---

## 📞 技术支持

如果在开发过程中遇到问题，可以：
1. 查看项目的README.md文档
2. 检查相关的GitHub Issues
3. 参考React Native和Whisper.cpp官方文档
4. 使用调试工具分析具体问题

**祝您开发顺利！** 🎉