#include <jni.h>
#include <android/log.h>
#include "whisper_engine.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static WhisperEngine* g_whisper_engine = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_cantonesevoicerecognition_native_WhisperJNI_initializeModel(
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
Java_com_example_cantonesevoicerecognition_native_WhisperJNI_transcribeAudio(
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
Java_com_example_cantonesevoicerecognition_native_WhisperJNI_releaseModel(
    JNIEnv *env, jobject thiz) {
    
    if (g_whisper_engine != nullptr) {
        delete g_whisper_engine;
        g_whisper_engine = nullptr;
    }
}