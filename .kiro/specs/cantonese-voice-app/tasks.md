# 粤语语音识别应用 - 详细实施计划

## 📋 开发环境要求

### 必需软件和工具
- **Node.js 18.0+** - JavaScript运行环境
- **React Native CLI 12.0+** - React Native命令行工具
- **Android Studio 2023.1+** - Android开发IDE
- **JDK 17** - Java开发工具包
- **Android SDK API 33+** - Android软件开发工具包
- **Git 2.30+** - 版本控制工具
- **Python 3.8+** - 用于构建脚本和工具

### Windows 开发环境配置
```cmd
:: 检查Node.js版本
node --version

:: 检查npm版本  
npm --version

:: 安装React Native CLI
npm install -g @react-native-community/cli

:: 检查Android开发环境
npx react-native doctor
```

## ⏱️ 项目开发时间规划

### 总体时间安排
- **项目总计**：6-8周（42-56个工作日）
- **核心功能开发**（任务1-6）：4-5周
- **优化和部署**（任务7-8）：2-3周

### 里程碑计划
- **第1周**：项目初始化和基础架构搭建
- **第2-3周**：AI模型集成和音频处理功能
- **第4-5周**：用户界面和数据存储功能
- **第6周**：性能优化和错误处理
- **第7-8周**：测试、打包和部署准备

---

## 🚀 详细实施任务

### 任务1：项目初始化和环境配置 **[预估：3-4天]**

**目标**：建立完整的开发环境，创建项目基础架构，配置所有必要的开发工具和依赖库。

**主要工作内容**：
- 创建React Native TypeScript项目结构
- 配置Android构建环境和Gradle依赖管理
- 安装和配置音频处理相关的第三方库
- 设置开发环境的调试、热重载和代码质量工具
- 建立项目文档和开发规范

**验收标准**：
- 项目能够成功编译并在Android设备上运行
- 所有开发工具配置正确且功能正常
- 代码质量检查工具正常工作
- 项目目录结构清晰，符合最佳实践

---

#### 子任务1.1：创建基础项目结构 **[预估：1天]**

**详细实施步骤**：

**步骤1：创建React Native项目**
```cmd
:: 创建新的React Native TypeScript项目
npx @react-native-community/cli@latest init CantoneseVoiceApp --template react-native-template-typescript

:: 进入项目目录
cd CantoneseVoiceApp

:: 验证项目创建成功
dir
```

**步骤2：创建项目目录结构**
```cmd
:: 创建主要源码目录
mkdir src
mkdir src\components
mkdir src\screens  
mkdir src\services
mkdir src\utils
mkdir src\types
mkdir src\hooks
mkdir src\context

:: 创建组件子目录
mkdir src\components\VoiceRecorder
mkdir src\components\TranscriptionList
mkdir src\components\AudioWaveform
mkdir src\components\LoadingSpinner

:: 创建页面目录
mkdir src\screens\HomeScreen
mkdir src\screens\HistoryScreen
mkdir src\screens\SettingsScreen

:: 创建服务目录
mkdir src\services\WhisperService
mkdir src\services\AudioService
mkdir src\services\StorageService
mkdir src\services\PermissionService

:: 创建资源目录
mkdir assets
mkdir assets\models
mkdir assets\images
mkdir assets\fonts

:: 创建文档目录
mkdir docs
mkdir scripts
mkdir __tests__
```

**步骤3：配置开发工具**

**创建ESLint配置文件** (`.eslintrc.js`)：
```javascript
module.exports = {
  root: true,
  extends: [
    '@react-native-community',
    'plugin:@typescript-eslint/recommended',
  ],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  rules: {
    // 自定义规则
    '@typescript-eslint/no-unused-vars': 'error',
    'react-native/no-inline-styles': 'warn',
  },
};
```

**创建Prettier配置文件** (`.prettierrc`)：
```json
{
  "arrowParens": "avoid",
  "bracketSameLine": true,
  "bracketSpacing": false,
  "singleQuote": true,
  "trailingComma": "all",
  "tabWidth": 2,
  "semi": true
}
```

**配置TypeScript路径映射** (`tsconfig.json`)：
```json
{
  "extends": "@react-native/typescript-config/tsconfig.json",
  "compilerOptions": {
    "baseUrl": "./src",
    "paths": {
      "@components/*": ["components/*"],
      "@screens/*": ["screens/*"],
      "@services/*": ["services/*"],
      "@utils/*": ["utils/*"],
      "@types/*": ["types/*"],
      "@hooks/*": ["hooks/*"],
      "@context/*": ["context/*"]
    }
  }
}
```

**配置Metro打包工具** (`metro.config.js`)：
```javascript
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

const config = {
  resolver: {
    alias: {
      '@components': './src/components',
      '@screens': './src/screens',
      '@services': './src/services',
      '@utils': './src/utils',
      '@types': './src/types',
      '@hooks': './src/hooks',
      '@context': './src/context',
    },
  },
  transformer: {
    assetPlugins: ['expo-asset/tools/hashAssetFiles'],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
```

**验证步骤**：
```cmd
:: 检查项目结构
tree /f src

:: 验证TypeScript配置
npx tsc --noEmit

:: 运行代码检查
npx eslint src --ext .ts,.tsx

:: 格式化代码
npx prettier --write "src/**/*.{ts,tsx}"
```

**关联需求**：需求4.1（用户界面响应性）

---

#### 子任务1.2：配置Android原生环境 **[预估：1天]**

**详细实施步骤**：

**步骤1：配置Android Gradle构建**

**修改应用级Gradle配置** (`android\app\build.gradle`)：
```gradle
android {
    compileSdkVersion 33
    buildToolsVersion "33.0.0"
    
    defaultConfig {
        applicationId "com.cantonesewhisper.app"
        minSdkVersion 24  // 支持Whisper.cpp和现代音频API
        targetSdkVersion 33
        versionCode 1
        versionName "1.0.0"
        
        // 配置NDK支持的架构
        ndk {
            abiFilters "arm64-v8a", "armeabi-v7a"
        }
        
        // 启用多DEX支持
        multiDexEnabled true
    }
    
    // 构建类型配置
    buildTypes {
        debug {
            debuggable true
            minifyEnabled false
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.debug // 临时使用debug签名
        }
    }
    
    // 解决依赖冲突
    packagingOptions {
        pickFirst '**/libc++_shared.so'
        pickFirst '**/libjsc.so'
        pickFirst '**/libfbjni.so'
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/NOTICE.txt'
    }
    
    // 配置CMake支持（为Whisper.cpp做准备）
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.18.1"
        }
    }
}

// 添加依赖
dependencies {
    implementation 'androidx.multidex:multidex:2.0.1'
    // 其他依赖将在后续任务中添加
}
```

**步骤2：配置应用权限**

**修改Android清单文件** (`android\app\src\main\AndroidManifest.xml`)：
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.cantonesewhisper.app">

    <!-- 音频录制权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- 文件存储权限 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- 网络权限（用于模型下载等） -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 防止设备休眠（录音时） -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <!-- 音频功能声明 -->
    <uses-feature 
        android:name="android.hardware.microphone" 
        android:required="true" />
    
    <application
        android:name=".MainApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:allowBackup="false"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme"
            android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**步骤3：配置代码混淆规则**

**创建ProGuard规则文件** (`android\app\proguard-rules.pro`)：
```proguard
# React Native相关
-keep class com.facebook.react.** { *; }
-keep class com.facebook.jni.** { *; }

# Whisper.cpp相关
-keep class com.whisper.** { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn com.whisper.**
-dontwarn ai.onnxruntime.**

# 音频处理相关
-keep class com.rnim.rn.audio.** { *; }
-dontwarn com.rnim.rn.audio.**

# 保持JNI方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持Serializable类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
```

**步骤4：创建资源目录结构**
```cmd
:: 创建模型文件存储目录
mkdir android\app\src\main\assets
mkdir android\app\src\main\assets\models

:: 创建原生代码目录（为Whisper.cpp集成做准备）
mkdir android\app\src\main\cpp

:: 创建JNI接口目录
mkdir android\app\src\main\java\com\cantonesewhisper\whisper
```

**步骤5：验证Android配置**
```cmd
:: 进入Android目录
cd android

:: 清理构建缓存
gradlew clean

:: 检查Gradle配置
gradlew tasks

:: 尝试构建项目
gradlew assembleDebug

:: 返回项目根目录
cd ..
```

**验证步骤**：
```cmd
:: 检查Android构建是否成功
dir android\app\build\outputs\apk\debug

:: 验证权限配置
findstr "RECORD_AUDIO" android\app\src\main\AndroidManifest.xml

:: 检查资源目录
dir android\app\src\main\assets
```

**关联需求**：需求1.1（音频录制功能）、需求2.2（离线识别功能）

---

#### 子任务1.3：安装和配置核心依赖库 **[预估：1天]**

**详细实施步骤**：

**步骤1：安装音频处理相关依赖**
```cmd
:: 安装音频录制和播放库
npm install react-native-audio-recorder-player@3.6.4

:: 安装文件系统操作库
npm install react-native-fs@2.20.0

:: 安装权限管理库
npm install react-native-permissions@3.10.1

:: 安装音频播放库（备用）
npm install react-native-sound@0.11.2
```

**步骤2：安装数据存储和状态管理依赖**
```cmd
:: 安装本地存储库
npm install @react-native-async-storage/async-storage@1.19.3

:: 安装网络状态检测库
npm install @react-native-netinfo/netinfo@9.4.1

:: 安装设备信息库
npm install react-native-device-info@10.11.0
```

**步骤3：安装导航和UI相关依赖**
```cmd
:: 安装导航库
npm install @react-navigation/native@6.1.8
npm install @react-navigation/stack@6.3.18
npm install @react-navigation/bottom-tabs@6.5.9

:: 安装导航依赖
npm install react-native-screens@3.25.0
npm install react-native-safe-area-context@4.7.4
npm install react-native-gesture-handler@2.13.1

:: 安装图标库
npm install react-native-vector-icons@10.0.0

:: 安装SVG支持
npm install react-native-svg@13.14.0
```

**步骤4：安装开发和测试工具**
```cmd
:: 安装类型定义
npm install --save-dev @types/react-native-vector-icons@6.4.14

:: 安装测试相关依赖
npm install --save-dev @testing-library/react-native@12.3.0
npm install --save-dev @testing-library/jest-native@5.4.3

:: 安装代码质量工具
npm install --save-dev eslint-plugin-react-hooks@4.6.0
npm install --save-dev @typescript-eslint/eslint-plugin@6.7.4
```

**步骤5：配置Android链接**
```cmd
:: 对于React Native 0.60+，大部分库支持自动链接
:: 但某些库需要手动配置

:: 重新构建项目以应用自动链接
cd android
gradlew clean
cd ..

:: 重新安装依赖
npm install
```

**步骤6：创建权限管理工具**

**创建权限工具文件** (`src\utils\permissions.ts`)：
```typescript
import {
  PermissionsAndroid, 
  Platform, 
  Alert,
  Linking
} from 'react-native';
import {PERMISSIONS, request, check, RESULTS} from 'react-native-permissions';

/**
 * 权限类型枚举
 */
export enum PermissionType {
  MICROPHONE = 'microphone',
  STORAGE = 'storage',
  CAMERA = 'camera'
}

/**
 * 权限状态接口
 */
export interface PermissionStatus {
  granted: boolean;
  message: string;
}

/**
 * 请求麦克风权限
 */
export const requestMicrophonePermission = async (): Promise<PermissionStatus> => {
  try {
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        {
          title: '麦克风权限请求',
          message: '应用需要访问麦克风来录制语音',
          buttonNeutral: '稍后询问',
          buttonNegative: '拒绝',
          buttonPositive: '允许',
        }
      );
      
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        return {granted: true, message: '麦克风权限已授予'};
      } else {
        return {granted: false, message: '麦克风权限被拒绝'};
      }
    }
    
    // iOS权限处理（预留）
    const result = await request(PERMISSIONS.IOS.MICROPHONE);
    return {
      granted: result === RESULTS.GRANTED,
      message: result === RESULTS.GRANTED ? '权限已授予' : '权限被拒绝'
    };
    
  } catch (error) {
    console.error('请求麦克风权限失败:', error);
    return {granted: false, message: '权限请求失败'};
  }
};

/**
 * 请求存储权限
 */
export const requestStoragePermission = async (): Promise<PermissionStatus> => {
  try {
    if (Platform.OS === 'android') {
      // Android 13+ 不再需要存储权限
      if (Platform.Version >= 33) {
        return {granted: true, message: '存储权限不需要'};
      }
      
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
        {
          title: '存储权限请求',
          message: '应用需要访问存储来保存录音文件',
          buttonNeutral: '稍后询问',
          buttonNegative: '拒绝',
          buttonPositive: '允许',
        }
      );
      
      return {
        granted: granted === PermissionsAndroid.RESULTS.GRANTED,
        message: granted === PermissionsAndroid.RESULTS.GRANTED ? 
          '存储权限已授予' : '存储权限被拒绝'
      };
    }
    
    return {granted: true, message: 'iOS存储权限自动授予'};
    
  } catch (error) {
    console.error('请求存储权限失败:', error);
    return {granted: false, message: '权限请求失败'};
  }
};

/**
 * 检查权限状态
 */
export const checkPermissionStatus = async (
  permissionType: PermissionType
): Promise<boolean> => {
  try {
    if (Platform.OS === 'android') {
      let permission: string;
      
      switch (permissionType) {
        case PermissionType.MICROPHONE:
          permission = PermissionsAndroid.PERMISSIONS.RECORD_AUDIO;
          break;
        case PermissionType.STORAGE:
          permission = PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE;
          break;
        default:
          return false;
      }
      
      const result = await PermissionsAndroid.check(permission);
      return result;
    }
    
    return true; // iOS默认返回true
    
  } catch (error) {
    console.error('检查权限状态失败:', error);
    return false;
  }
};

/**
 * 请求所有必需权限
 */
export const requestAllPermissions = async (): Promise<{
  microphone: boolean;
  storage: boolean;
}> => {
  const microphoneResult = await requestMicrophonePermission();
  const storageResult = await requestStoragePermission();
  
  return {
    microphone: microphoneResult.granted,
    storage: storageResult.granted
  };
};

/**
 * 显示权限设置引导
 */
export const showPermissionSettingsAlert = (permissionName: string) => {
  Alert.alert(
    '权限被拒绝',
    `${permissionName}权限被拒绝，请到设置中手动开启`,
    [
      {text: '取消', style: 'cancel'},
      {text: '去设置', onPress: () => Linking.openSettings()}
    ]
  );
};
```

**步骤7：创建应用配置文件**

**创建常量配置文件** (`src\utils\constants.ts`)：
```typescript
import {Platform} from 'react-native';

/**
 * 应用基础配置
 */
export const APP_CONFIG = {
  NAME: '粤语语音识别',
  VERSION: '1.0.0',
  BUILD_NUMBER: 1,
  BUNDLE_ID: 'com.cantonesewhisper.app'
};

/**
 * 音频配置
 */
export const AUDIO_CONFIG = {
  SAMPLE_RATE: 16000,        // Whisper要求的采样率
  CHANNELS: 1,               // 单声道
  BITS_PER_SAMPLE: 16,       // 16位采样
  FORMAT: 'wav',             // 音频格式
  MAX_DURATION: 300,         // 最大录音时长（秒）
  MIN_DURATION: 1,           // 最小录音时长（秒）
};

/**
 * 存储配置
 */
export const STORAGE_CONFIG = {
  MAX_RECORDS: 1000,         // 最大历史记录数
  MAX_STORAGE_SIZE: 500,     // 最大存储空间（MB）
  CACHE_DURATION: 7,         // 缓存保留天数
};

/**
 * 模型配置
 */
export const MODEL_CONFIG = {
  NAME: 'whisper-cantonese.bin',
  VERSION: '1.0.0',
  SIZE: 3000,                // 模型大小（MB）
  DOWNLOAD_URL: 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2.bin',
  LOCAL_PATH: Platform.select({
    android: 'android_asset://models/',
    ios: 'models/',
  }),
};

/**
 * API配置
 */
export const API_CONFIG = {
  TIMEOUT: 30000,            // 请求超时时间（毫秒）
  RETRY_COUNT: 3,            // 重试次数
  BASE_URL: '',              // API基础URL（如果需要）
};

/**
 * UI配置
 */
export const UI_CONFIG = {
  COLORS: {
    PRIMARY: '#007AFF',
    SECONDARY: '#5856D6',
    SUCCESS: '#34C759',
    WARNING: '#FF9500',
    ERROR: '#FF3B30',
    BACKGROUND: '#F2F2F7',
    SURFACE: '#FFFFFF',
    TEXT_PRIMARY: '#000000',
    TEXT_SECONDARY: '#8E8E93',
  },
  FONTS: {
    REGULAR: 'System',
    BOLD: 'System-Bold',
    SIZE_SMALL: 12,
    SIZE_MEDIUM: 16,
    SIZE_LARGE: 20,
    SIZE_XLARGE: 24,
  },
  SPACING: {
    XS: 4,
    SM: 8,
    MD: 16,
    LG: 24,
    XL: 32,
  },
};
```

**步骤8：验证依赖安装**
```cmd
:: 检查package.json中的依赖
type package.json | findstr "react-native-audio-recorder-player"

:: 验证自动链接
npx react-native info

:: 清理并重新构建
npm run android
```

**验证步骤**：
```cmd
:: 检查node_modules是否正确安装
dir node_modules\react-native-audio-recorder-player

:: 验证TypeScript编译
npx tsc --noEmit

:: 运行代码检查
npx eslint src\utils\permissions.ts
```

**关联需求**：需求1.1（音频录制功能）、需求5.1（数据存储功能）

---

### 任务2：Whisper模型集成和AI引擎开发 **[预估：5-7天]**

**目标**：集成OpenAI Whisper.cpp库，实现高精度的粤语语音识别功能，建立完整的AI推理引擎。

**主要工作内容**：
- 集成Whisper.cpp原生库到React Native项目
- 下载和配置经过粤语微调的Whisper模型文件
- 实现模型加载、初始化和内存管理功能
- 开发语音转文本的核心处理逻辑和优化算法
- 建立音频预处理和后处理管道

**验收标准**：
- Whisper模型能够成功加载并初始化
- 语音识别准确率达到90%以上（标准粤语）
- 单次转录响应时间小于音频时长的50%
- 内存使用稳定，无明显内存泄漏

**技术难点**：
- C++原生库与React Native的JNI集成
- 大型模型文件的内存管理和优化
- 音频数据格式转换和预处理
- 多线程处理和异步操作

---

#### 子任务2.1：集成Whisper.cpp原生库 **[预估：2-3天]**

**详细实施步骤**：

**步骤1：下载和准备Whisper.cpp源码**
```cmd
:: 创建临时目录用于下载源码
mkdir temp
cd temp

:: 克隆Whisper.cpp仓库
git clone https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp

:: 切换到稳定版本
git checkout v1.5.4

:: 查看项目结构
dir

:: 返回项目根目录
cd ..\..
```

**步骤2：设置Android NDK构建环境**

**创建CMake配置文件** (`android\app\src\main\cpp\CMakeLists.txt`)：
```cmake
cmake_minimum_required(VERSION 3.18.1)
project("cantonesewhisper")

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加编译选项
add_compile_options(-O3 -DNDEBUG)

# 设置Whisper.cpp源码路径
set(WHISPER_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

# 添加Whisper.cpp源文件
set(WHISPER_SOURCES
    ${WHISPER_CPP_DIR}/whisper.cpp
    ${WHISPER_CPP_DIR}/ggml.c
    ${WHISPER_CPP_DIR}/ggml-alloc.c
    ${WHISPER_CPP_DIR}/ggml-backend.c
    ${WHISPER_CPP_DIR}/ggml-quants.c
)

# 添加头文件目录
include_directories(${WHISPER_CPP_DIR})

# 创建Whisper静态库
add_library(whisper STATIC ${WHISPER_SOURCES})

# 设置Whisper库的编译选项
target_compile_definitions(whisper PRIVATE
    GGML_USE_ACCELERATE=0
    WHISPER_USE_COREML=0
    WHISPER_USE_METAL=0
)

# 创建JNI共享库
add_library(cantonesewhisper SHARED
    whisper_jni.cpp
    audio_utils.cpp
)

# 链接库
target_link_libraries(cantonesewhisper
    whisper
    android
    log
    m
)

# 设置JNI库的编译选项
target_compile_definitions(cantonesewhisper PRIVATE
    ANDROID=1
)
```

**步骤3：实现JNI接口**

**创建JNI接口文件** (`android\app\src\main\cpp\whisper_jni.cpp`)：
```cpp
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <thread>
#include <mutex>
#include "whisper.h"

// 日志宏定义
#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 全局变量
static std::mutex g_mutex;
static std::map<jlong, std::shared_ptr<whisper_context>> g_contexts;

/**
 * 初始化Whisper上下文
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_cantonesewhisper_WhisperModule_initContext(
    JNIEnv *env, jobject thiz, jstring model_path) {
    
    const char *path = env->GetStringUTFChars(model_path, 0);
    LOGI("正在初始化Whisper模型: %s", path);
    
    try {
        // 初始化Whisper参数
        struct whisper_context_params cparams = whisper_context_default_params();
        cparams.use_gpu = false; // Android暂不支持GPU加速
        
        // 加载模型
        struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
        
        if (ctx == nullptr) {
            LOGE("模型加载失败: %s", path);
            env->ReleaseStringUTFChars(model_path, path);
            return 0;
        }
        
        // 存储上下文
        jlong context_ptr = reinterpret_cast<jlong>(ctx);
        {
            std::lock_guard<std::mutex> lock(g_mutex);
            g_contexts[context_ptr] = std::shared_ptr<whisper_context>(ctx, whisper_free);
        }
        
        LOGI("Whisper模型初始化成功，上下文指针: %ld", context_ptr);
        env->ReleaseStringUTFChars(model_path, path);
        return context_ptr;
        
    } catch (const std::exception& e) {
        LOGE("初始化异常: %s", e.what());
        env->ReleaseStringUTFChars(model_path, path);
        return 0;
    }
}

/**
 * 语音转录
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_cantonesewhisper_WhisperModule_transcribe(
    JNIEnv *env, jobject thiz, jlong context_ptr, jfloatArray audio_data) {
    
    LOGI("开始语音转录，上下文指针: %ld", context_ptr);
    
    try {
        // 获取上下文
        std::shared_ptr<whisper_context> ctx;
        {
            std::lock_guard<std::mutex> lock(g_mutex);
            auto it = g_contexts.find(context_ptr);
            if (it == g_contexts.end()) {
                LOGE("无效的上下文指针: %ld", context_ptr);
                return env->NewStringUTF("");
            }
            ctx = it->second;
        }
        
        // 获取音频数据
        jsize audio_length = env->GetArrayLength(audio_data);
        jfloat *audio_buffer = env->GetFloatArrayElements(audio_data, nullptr);
        
        if (audio_buffer == nullptr || audio_length == 0) {
            LOGE("音频数据为空");
            return env->NewStringUTF("");
        }
        
        LOGI("音频数据长度: %d", audio_length);
        
        // 设置Whisper参数
        struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        wparams.language = "zh";  // 中文
        wparams.n_threads = std::min(4, (int)std::thread::hardware_concurrency());
        wparams.translate = false;
        wparams.print_progress = false;
        wparams.print_timestamps = false;
        
        // 执行转录
        int result = whisper_full(ctx.get(), wparams, audio_buffer, audio_length);
        
        // 释放音频数据
        env->ReleaseFloatArrayElements(audio_data, audio_buffer, JNI_ABORT);
        
        if (result != 0) {
            LOGE("转录失败，错误代码: %d", result);
            return env->NewStringUTF("");
        }
        
        // 获取转录结果
        std::string transcribed_text;
        const int n_segments = whisper_full_n_segments(ctx.get());
        
        for (int i = 0; i < n_segments; ++i) {
            const char *text = whisper_full_get_segment_text(ctx.get(), i);
            if (text != nullptr) {
                transcribed_text += text;
            }
        }
        
        LOGI("转录完成，结果长度: %zu", transcribed_text.length());
        return env->NewStringUTF(transcribed_text.c_str());
        
    } catch (const std::exception& e) {
        LOGE("转录异常: %s", e.what());
        return env->NewStringUTF("");
    }
}

/**
 * 获取模型信息
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_cantonesewhisper_WhisperModule_getModelInfo(
    JNIEnv *env, jobject thiz, jlong context_ptr) {
    
    try {
        std::shared_ptr<whisper_context> ctx;
        {
            std::lock_guard<std::mutex> lock(g_mutex);
            auto it = g_contexts.find(context_ptr);
            if (it == g_contexts.end()) {
                return env->NewStringUTF("{}");
            }
            ctx = it->second;
        }
        
        // 构建模型信息JSON
        std::string model_info = "{";
        model_info += "\"vocab_size\":" + std::to_string(whisper_model_n_vocab(ctx.get())) + ",";
        model_info += "\"audio_ctx_size\":" + std::to_string(whisper_model_n_audio_ctx(ctx.get())) + ",";
        model_info += "\"text_ctx_size\":" + std::to_string(whisper_model_n_text_ctx(ctx.get()));
        model_info += "}";
        
        return env->NewStringUTF(model_info.c_str());
        
    } catch (const std::exception& e) {
        LOGE("获取模型信息异常: %s", e.what());
        return env->NewStringUTF("{}");
    }
}

/**
 * 释放上下文
 */
extern "C" JNIEXPORT void JNICALL
Java_com_cantonesewhisper_WhisperModule_releaseContext(
    JNIEnv *env, jobject thiz, jlong context_ptr) {
    
    LOGI("释放Whisper上下文: %ld", context_ptr);
    
    std::lock_guard<std::mutex> lock(g_mutex);
    auto it = g_contexts.find(context_ptr);
    if (it != g_contexts.end()) {
        g_contexts.erase(it);
        LOGI("上下文释放成功");
    }
}
```

**步骤4：创建音频处理工具**

**创建音频工具文件** (`android\app\src\main\cpp\audio_utils.cpp`)：
```cpp
#include <jni.h>
#include <android/log.h>
#include <vector>
#include <algorithm>
#include <cmath>

#define LOG_TAG "AudioUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * 音频重采样
 */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_cantonesewhisper_AudioUtils_resampleAudio(
    JNIEnv *env, jobject thiz, jfloatArray input_audio, 
    jint input_sample_rate, jint output_sample_rate) {
    
    jsize input_length = env->GetArrayLength(input_audio);
    jfloat *input_data = env->GetFloatArrayElements(input_audio, nullptr);
    
    if (input_sample_rate == output_sample_rate) {
        // 无需重采样
        env->ReleaseFloatArrayElements(input_audio, input_data, JNI_ABORT);
        return input_audio;
    }
    
    // 计算输出长度
    double ratio = (double)output_sample_rate / input_sample_rate;
    jsize output_length = (jsize)(input_length * ratio);
    
    std::vector<float> output_data(output_length);
    
    // 简单线性插值重采样
    for (int i = 0; i < output_length; i++) {
        double src_index = i / ratio;
        int src_index_int = (int)src_index;
        double fraction = src_index - src_index_int;
        
        if (src_index_int >= input_length - 1) {
            output_data[i] = input_data[input_length - 1];
        } else {
            output_data[i] = input_data[src_index_int] * (1.0 - fraction) + 
                           input_data[src_index_int + 1] * fraction;
        }
    }
    
    // 创建输出数组
    jfloatArray output_array = env->NewFloatArray(output_length);
    env->SetFloatArrayRegion(output_array, 0, output_length, output_data.data());
    
    env->ReleaseFloatArrayElements(input_audio, input_data, JNI_ABORT);
    return output_array;
}

/**
 * 音频归一化
 */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_cantonesewhisper_AudioUtils_normalizeAudio(
    JNIEnv *env, jobject thiz, jfloatArray input_audio) {
    
    jsize length = env->GetArrayLength(input_audio);
    jfloat *data = env->GetFloatArrayElements(input_audio, nullptr);
    
    // 找到最大绝对值
    float max_val = 0.0f;
    for (int i = 0; i < length; i++) {
        max_val = std::max(max_val, std::abs(data[i]));
    }
    
    // 归一化
    if (max_val > 0.0f) {
        float scale = 0.95f / max_val; // 留一点余量
        for (int i = 0; i < length; i++) {
            data[i] *= scale;
        }
    }
    
    jfloatArray output_array = env->NewFloatArray(length);
    env->SetFloatArrayRegion(output_array, 0, length, data);
    
    env->ReleaseFloatArrayElements(input_audio, data, JNI_ABORT);
    return output_array;
}
```

**步骤5：更新Gradle配置**

**修改应用级Gradle文件** (`android\app\build.gradle`)：
```gradle
android {
    // ... 其他配置

    // 启用NDK构建
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.18.1"
        }
    }
    
    // NDK配置
    ndk {
        abiFilters "arm64-v8a", "armeabi-v7a"
    }
    
    // 构建配置
    buildTypes {
        debug {
            ndk {
                debugSymbolLevel 'SYMBOL_TABLE'
            }
        }
        release {
            ndk {
                debugSymbolLevel 'NONE'
            }
        }
    }
}
```

**步骤6：复制Whisper.cpp源码到项目**
```cmd
:: 复制Whisper.cpp源码到Android项目
xcopy /E /I temp\whisper.cpp android\app\src\main\cpp\whisper.cpp

:: 验证文件复制
dir android\app\src\main\cpp\whisper.cpp
```

**步骤7：构建和测试**
```cmd
:: 清理构建缓存
cd android
gradlew clean

:: 构建项目
gradlew assembleDebug

:: 检查生成的库文件
dir app\build\intermediates\cmake\debug\obj

:: 返回项目根目录
cd ..
```

**验证步骤**：
```cmd
:: 检查CMake配置
type android\app\src\main\cpp\CMakeLists.txt

:: 验证JNI文件
type android\app\src\main\cpp\whisper_jni.cpp

:: 检查构建输出
dir android\app\build\intermediates\cmake\debug\obj\arm64-v8a
```

**关联需求**：需求2.1（离线识别功能）、需求3.1（高精度识别）

---

#### 子任务2.2：实现WhisperService核心服务 **[预估：2天]**

**详细实施步骤**：

**步骤1：创建Java原生模块接口**

**创建WhisperModule Java类** (`android\app\src\main\java\com\cantonesewhisper\WhisperModule.java`)：
```java
package com.cantonesewhisper;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import android.util.Log;

public class WhisperModule extends ReactContextBaseJavaModule {
    private static final String TAG = "WhisperModule";
    
    // 加载原生库
    static {
        try {
            System.loadLibrary("cantonesewhisper");
            Log.i(TAG, "Whisper原生库加载成功");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Whisper原生库加载失败: " + e.getMessage());
        }
    }
    
    public WhisperModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    
    @Override
    public String getName() {
        return "WhisperModule";
    }
    
    /**
     * 初始化Whisper上下文
     */
    @ReactMethod
    public void initContext(String modelPath, Promise promise) {
        try {
            Log.i(TAG, "开始初始化Whisper模型: " + modelPath);
            long contextPtr = nativeInitContext(modelPath);
            
            if (contextPtr == 0) {
                promise.reject("INIT_FAILED", "模型初始化失败");
            } else {
                promise.resolve((double) contextPtr);
                Log.i(TAG, "模型初始化成功，上下文指针: " + contextPtr);
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化异常: " + e.getMessage());
            promise.reject("INIT_ERROR", e.getMessage());
        }
    }
    
    /**
     * 执行语音转录
     */
    @ReactMethod
    public void transcribe(double contextPtr, ReadableArray audioData, Promise promise) {
        try {
            Log.i(TAG, "开始语音转录");
            
            // 转换音频数据
            float[] audioArray = new float[audioData.size()];
            for (int i = 0; i < audioData.size(); i++) {
                audioArray[i] = (float) audioData.getDouble(i);
            }
            
            // 执行转录
            String result = nativeTranscribe((long) contextPtr, audioArray);
            
            if (result != null && !result.isEmpty()) {
                promise.resolve(result);
                Log.i(TAG, "转录成功，结果长度: " + result.length());
            } else {
                promise.reject("TRANSCRIBE_FAILED", "转录结果为空");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "转录异常: " + e.getMessage());
            promise.reject("TRANSCRIBE_ERROR", e.getMessage());
        }
    }
    
    /**
     * 获取模型信息
     */
    @ReactMethod
    public void getModelInfo(double contextPtr, Promise promise) {
        try {
            String modelInfo = nativeGetModelInfo((long) contextPtr);
            promise.resolve(modelInfo);
        } catch (Exception e) {
            promise.reject("MODEL_INFO_ERROR", e.getMessage());
        }
    }
    
    /**
     * 释放上下文
     */
    @ReactMethod
    public void releaseContext(double contextPtr, Promise promise) {
        try {
            nativeReleaseContext((long) contextPtr);
            promise.resolve(null);
            Log.i(TAG, "上下文释放成功");
        } catch (Exception e) {
            promise.reject("RELEASE_ERROR", e.getMessage());
        }
    }
    
    // 原生方法声明
    private native long nativeInitContext(String modelPath);
    private native String nativeTranscribe(long contextPtr, float[] audioData);
    private native String nativeGetModelInfo(long contextPtr);
    private native void nativeReleaseContext(long contextPtr);
}
```

**步骤2：注册原生模块**

**创建模块包类** (`android\app\src\main\java\com\cantonesewhisper\WhisperPackage.java`)：
```java
package com.cantonesewhisper;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhisperPackage implements ReactPackage {
    
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new WhisperModule(reactContext));
        return modules;
    }
    
    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
```

**注册到MainApplication** (`android\app\src\main\java\com\cantonesewhisper\MainApplication.java`)：
```java
// 在getPackages()方法中添加
@Override
protected List<ReactPackage> getPackages() {
    @SuppressWarnings("UnnecessaryLocalVariable")
    List<ReactPackage> packages = new PackageList(this).getPackages();
    // 添加Whisper模块
    packages.add(new WhisperPackage());
    return packages;
}
```

**步骤3：创建TypeScript类型定义**

**创建类型定义文件** (`src\types\whisper.ts`)：
```typescript
/**
 * 转录结果接口
 */
export interface TranscriptionResult {
  text: string;                    // 转录文本
  confidence: number;              // 置信度 (0-1)
  processingTime: number;          // 处理时间（毫秒）
  segments: TranscriptionSegment[]; // 分段信息
  language?: string;               // 识别的语言
  modelInfo?: ModelInfo;           // 模型信息
}

/**
 * 转录分段接口
 */
export interface TranscriptionSegment {
  start: number;                   // 开始时间（秒）
  end: number;                     // 结束时间（秒）
  text: string;                    // 分段文本
  confidence: number;              // 分段置信度
  tokens?: Token[];                // 词元信息
}

/**
 * 词元接口
 */
export interface Token {
  id: number;                      // 词元ID
  text: string;                    // 词元文本
  probability: number;             // 概率
  timestamp: number;               // 时间戳
}

/**
 * 模型信息接口
 */
export interface ModelInfo {
  vocabSize: number;               // 词汇表大小
  audioCtxSize: number;            // 音频上下文大小
  textCtxSize: number;             // 文本上下文大小
  modelType: string;               // 模型类型
  version: string;                 // 版本信息
}

/**
 * Whisper配置接口
 */
export interface WhisperConfig {
  language: string;                // 目标语言
  translate: boolean;              // 是否翻译
  threads: number;                 // 线程数
  maxTokens: number;               // 最大词元数
  temperature: number;             // 温度参数
  compressionRatio: number;        // 压缩比阈值
  noSpeechThreshold: number;       // 无语音阈值
}

/**
 * 音频处理配置
 */
export interface AudioProcessingConfig {
  sampleRate: number;              // 采样率
  channels: number;                // 声道数
  bitDepth: number;                // 位深度
  normalize: boolean;              // 是否归一化
  removeNoise: boolean;            // 是否降噪
}
```

**步骤4：实现WhisperService核心服务**

**创建WhisperService类** (`src\services\WhisperService\index.ts`)：
```typescript
import { NativeModules, Platform } from 'react-native';
import RNFS from 'react-native-fs';
import {
  TranscriptionResult,
  TranscriptionSegment,
  ModelInfo,
  WhisperConfig,
  AudioProcessingConfig
} from '../../types/whisper';
import { convertToFloat32Array, resampleAudio, normalizeAudio } from '../../utils/audioUtils';

// 原生模块接口
interface WhisperNativeModule {
  initContext(modelPath: string): Promise<number>;
  transcribe(contextPtr: number, audioData: number[]): Promise<string>;
  getModelInfo(contextPtr: number): Promise<string>;
  releaseContext(contextPtr: number): Promise<void>;
}

const { WhisperModule } = NativeModules as { WhisperModule: WhisperNativeModule };

/**
 * Whisper语音识别服务
 */
export class WhisperService {
  private contextPtr: number | null = null;
  private isInitialized = false;
  private modelPath: string = '';
  private config: WhisperConfig;
  
  constructor(config?: Partial<WhisperConfig>) {
    this.config = {
      language: 'zh',
      translate: false,
      threads: Math.min(4, require('os').cpus?.()?.length || 2),
      maxTokens: 224,
      temperature: 0.0,
      compressionRatio: 2.4,
      noSpeechThreshold: 0.6,
      ...config
    };
  }
  
  /**
   * 初始化Whisper模型
   */
  async initialize(modelPath?: string): Promise<void> {
    try {
      console.log('开始初始化Whisper服务...');
      
      // 确定模型路径
      this.modelPath = modelPath || await this.getDefaultModelPath();
      
      // 检查模型文件是否存在
      const modelExists = await RNFS.exists(this.modelPath);
      if (!modelExists) {
        throw new Error(`模型文件不存在: ${this.modelPath}`);
      }
      
      // 检查模型文件大小
      const modelStats = await RNFS.stat(this.modelPath);
      console.log(`模型文件大小: ${(modelStats.size / 1024 / 1024).toFixed(2)} MB`);
      
      // 初始化原生上下文
      this.contextPtr = await WhisperModule.initContext(this.modelPath);
      
      if (!this.contextPtr || this.contextPtr === 0) {
        throw new Error('模型初始化失败，返回的上下文指针无效');
      }
      
      this.isInitialized = true;
      console.log('Whisper模型初始化成功');
      
    } catch (error) {
      this.isInitialized = false;
      this.contextPtr = null;
      throw new Error(`Whisper初始化失败: ${error.message}`);
    }
  }
  
  /**
   * 执行语音转录
   */
  async transcribe(
    audioData: Float32Array,
    processingConfig?: AudioProcessingConfig
  ): Promise<TranscriptionResult> {
    if (!this.isInitialized || !this.contextPtr) {
      throw new Error('Whisper模型未初始化，请先调用initialize()');
    }
    
    try {
      console.log(`开始转录，音频长度: ${audioData.length} 采样点`);
      const startTime = Date.now();
      
      // 音频预处理
      let processedAudio = audioData;
      
      if (processingConfig) {
        // 重采样到16kHz
        if (processingConfig.sampleRate !== 16000) {
          processedAudio = resampleAudio(
            processedAudio, 
            processingConfig.sampleRate, 
            16000
          );
        }
        
        // 归一化
        if (processingConfig.normalize) {
          processedAudio = normalizeAudio(processedAudio);
        }
      }
      
      // 转换为数组格式
      const audioArray = Array.from(processedAudio);
      
      // 执行转录
      const transcriptionText = await WhisperModule.transcribe(
        this.contextPtr, 
        audioArray
      );
      
      const processingTime = Date.now() - startTime;
      
      // 构建结果
      const result: TranscriptionResult = {
        text: transcriptionText.trim(),
        confidence: this.calculateConfidence(transcriptionText),
        processingTime,
        segments: this.parseSegments(transcriptionText),
        language: this.config.language
      };
      
      console.log(`转录完成，耗时: ${processingTime}ms，结果: "${result.text}"`);
      return result;
      
    } catch (error) {
      console.error('转录失败:', error);
      throw new Error(`语音转录失败: ${error.message}`);
    }
  }
  
  /**
   * 获取模型信息
   */
  async getModelInfo(): Promise<ModelInfo> {
    if (!this.isInitialized || !this.contextPtr) {
      throw new Error('Whisper模型未初始化');
    }
    
    try {
      const modelInfoJson = await WhisperModule.getModelInfo(this.contextPtr);
      const modelInfo = JSON.parse(modelInfoJson);
      
      return {
        vocabSize: modelInfo.vocab_size || 0,
        audioCtxSize: modelInfo.audio_ctx_size || 0,
        textCtxSize: modelInfo.text_ctx_size || 0,
        modelType: 'whisper-large-v2',
        version: '1.5.4'
      };
      
    } catch (error) {
      throw new Error(`获取模型信息失败: ${error.message}`);
    }
  }
  
  /**
   * 检查初始化状态
   */
  isReady(): boolean {
    return this.isInitialized && this.contextPtr !== null;
  }
  
  /**
   * 释放资源
   */
  async dispose(): Promise<void> {
    if (this.contextPtr) {
      try {
        await WhisperModule.releaseContext(this.contextPtr);
        console.log('Whisper上下文已释放');
      } catch (error) {
        console.error('释放上下文失败:', error);
      }
      
      this.contextPtr = null;
      this.isInitialized = false;
    }
  }
  
  /**
   * 获取默认模型路径
   */
  private async getDefaultModelPath(): Promise<string> {
    if (Platform.OS === 'android') {
      return `${RNFS.MainBundlePath}/assets/models/whisper-cantonese.bin`;
    } else {
      return `${RNFS.MainBundlePath}/models/whisper-cantonese.bin`;
    }
  }
  
  /**
   * 计算置信度
   */
  private calculateConfidence(text: string): number {
    if (!text || text.trim().length === 0) {
      return 0.0;
    }
    
    // 基于文本特征计算置信度
    const length = text.trim().length;
    const hasChineseChars = /[\u4e00-\u9fff]/.test(text);
    const hasRepeatedChars = /(.)\1{3,}/.test(text);
    
    let confidence = 0.5; // 基础置信度
    
    // 长度因子
    if (length > 5) confidence += 0.2;
    if (length > 20) confidence += 0.1;
    
    // 中文字符因子
    if (hasChineseChars) confidence += 0.2;
    
    // 重复字符惩罚
    if (hasRepeatedChars) confidence -= 0.3;
    
    return Math.max(0.1, Math.min(0.95, confidence));
  }
  
  /**
   * 解析分段信息
   */
  private parseSegments(text: string): TranscriptionSegment[] {
    // 简单实现，将整个文本作为一个分段
    // 实际实现中可以根据标点符号或时间戳进行分段
    return [{
      start: 0,
      end: 0,
      text: text.trim(),
      confidence: this.calculateConfidence(text)
    }];
  }
}

// 导出单例实例
export const whisperService = new WhisperService();
```

**步骤5：创建音频处理工具**

**创建音频工具文件** (`src\utils\audioUtils.ts`)：
```typescript
import { NativeModules } from 'react-native';

// 音频处理原生模块
const { AudioUtils } = NativeModules;

/**
 * 将ArrayBuffer转换为Float32Array
 */
export const convertToFloat32Array = (audioBuffer: ArrayBuffer): Float32Array => {
  const int16Array = new Int16Array(audioBuffer);
  const float32Array = new Float32Array(int16Array.length);
  
  // 将16位整数转换为浮点数并归一化到[-1, 1]
  for (let i = 0; i < int16Array.length; i++) {
    float32Array[i] = int16Array[i] / 32768.0;
  }
  
  return float32Array;
};

/**
 * 音频重采样
 */
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
  
  // 简单线性插值重采样
  for (let i = 0; i < newLength; i++) {
    const srcIndex = i * ratio;
    const srcIndexInt = Math.floor(srcIndex);
    const fraction = srcIndex - srcIndexInt;
    
    if (srcIndexInt >= audioData.length - 1) {
      result[i] = audioData[audioData.length - 1];
    } else {
      result[i] = audioData[srcIndexInt] * (1 - fraction) + 
                 audioData[srcIndexInt + 1] * fraction;
    }
  }
  
  return result;
};

/**
 * 音频归一化
 */
export const normalizeAudio = (audioData: Float32Array): Float32Array => {
  // 找到最大绝对值
  let maxVal = 0;
  for (let i = 0; i < audioData.length; i++) {
    maxVal = Math.max(maxVal, Math.abs(audioData[i]));
  }
  
  if (maxVal === 0) {
    return audioData;
  }
  
  // 归一化，留一点余量避免削波
  const scale = 0.95 / maxVal;
  const result = new Float32Array(audioData.length);
  
  for (let i = 0; i < audioData.length; i++) {
    result[i] = audioData[i] * scale;
  }
  
  return result;
};

/**
 * 简单降噪处理
 */
export const removeNoise = (audioData: Float32Array, threshold: number = 0.01): Float32Array => {
  const result = new Float32Array(audioData.length);
  
  for (let i = 0; i < audioData.length; i++) {
    // 简单的门限降噪
    if (Math.abs(audioData[i]) < threshold) {
      result[i] = 0;
    } else {
      result[i] = audioData[i];
    }
  }
  
  return result;
};

/**
 * 计算音频RMS（均方根）
 */
export const calculateRMS = (audioData: Float32Array): number => {
  let sum = 0;
  for (let i = 0; i < audioData.length; i++) {
    sum += audioData[i] * audioData[i];
  }
  return Math.sqrt(sum / audioData.length);
};

/**
 * 检测静音段
 */
export const detectSilence = (
  audioData: Float32Array,
  threshold: number = 0.01,
  minSilenceDuration: number = 0.5,
  sampleRate: number = 16000
): Array<{start: number, end: number}> => {
  const silenceSegments: Array<{start: number, end: number}> = [];
  const minSilenceSamples = minSilenceDuration * sampleRate;
  
  let silenceStart = -1;
  let silenceLength = 0;
  
  for (let i = 0; i < audioData.length; i++) {
    if (Math.abs(audioData[i]) < threshold) {
      if (silenceStart === -1) {
        silenceStart = i;
      }
      silenceLength++;
    } else {
      if (silenceStart !== -1 && silenceLength >= minSilenceSamples) {
        silenceSegments.push({
          start: silenceStart / sampleRate,
          end: (silenceStart + silenceLength) / sampleRate
        });
      }
      silenceStart = -1;
      silenceLength = 0;
    }
  }
  
  // 处理结尾的静音
  if (silenceStart !== -1 && silenceLength >= minSilenceSamples) {
    silenceSegments.push({
      start: silenceStart / sampleRate,
      end: (silenceStart + silenceLength) / sampleRate
    });
  }
  
  return silenceSegments;
};
```

**验证步骤**：
```cmd
:: 检查Java文件
type android\app\src\main\java\com\cantonesewhisper\WhisperModule.java

:: 验证TypeScript文件
npx tsc --noEmit src\services\WhisperService\index.ts

:: 构建项目
cd android
gradlew assembleDebug
cd ..
```

**关联需求**：需求1.2（离线识别）、需求3.1（高精度识别）、需求3.2（音频处理）

---

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