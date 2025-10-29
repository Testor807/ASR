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