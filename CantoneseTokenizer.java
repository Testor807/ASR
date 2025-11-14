package com.example.offlinecantoneseasr;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// CantoneseTokenizer.java
public class CantoneseTokenizer {
    private static final String TAG = "CantoneseTokenizer";

    private Map<String, Integer> vocab;
    private Map<Integer, String> reverseVocab;
    private int vocabSize;

    public CantoneseTokenizer(String tokenizerJson) throws JSONException {
        loadVocabulary(tokenizerJson);
    }

    private void loadVocabulary(String tokenizerJson) throws JSONException {
        vocab = new HashMap<>();
        reverseVocab = new HashMap<>();

        JSONObject jsonObject = new JSONObject(tokenizerJson);
        JSONObject vocabObject = jsonObject.getJSONObject("model").getJSONObject("vocab");

        Iterator<String> keys = vocabObject.keys();
        while (keys.hasNext()) {
            String token = keys.next();
            int id = vocabObject.getInt(token);
            vocab.put(token, id);
            reverseVocab.put(id, token);
        }

        vocabSize = vocab.size();
        Log.d(TAG, "词汇表加载完成，大小: " + vocabSize);
    }

    public String decode(float[][] tokenProbs) {
        StringBuilder text = new StringBuilder();
        boolean previousWasSpace = true;

        for (float[] tokenProb : tokenProbs) {
            int tokenId = argMax(tokenProb);

            // 跳过特殊token
            if (tokenId == 50257 || tokenId == 50256) { // 结束token
                break;
            }

            String token = reverseVocab.get(tokenId);
            if (token != null) {
                if (token.startsWith("▁")) { // Whisper的空间标记
                    if (!previousWasSpace) {
                        text.append(" ");
                    }
                    token = token.substring(1);
                    previousWasSpace = false;
                }

                text.append(token);
                previousWasSpace = false;
            }
        }

        return text.toString().trim();
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public int getVocabSize() {
        return vocabSize;
    }
}