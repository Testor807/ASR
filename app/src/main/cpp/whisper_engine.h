#ifndef WHISPER_ENGINE_H
#define WHISPER_ENGINE_H

#include <string>
#include <vector>

class WhisperEngine {
public:
    WhisperEngine();
    ~WhisperEngine();
    
    bool initialize(const std::string& model_path);
    std::string transcribe(const float* audio_data, size_t length);
    bool isInitialized() const { return initialized_; }
    
private:
    bool initialized_;
    
    std::vector<float> preprocessAudio(const float* audio_data, size_t length);
    std::string postprocessOutput(const std::vector<float>& output);
};

#endif // WHISPER_ENGINE_H