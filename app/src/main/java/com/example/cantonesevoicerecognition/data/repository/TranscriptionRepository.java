package com.example.cantonesevoicerecognition.data.repository;

import android.app.Application;
import com.example.cantonesevoicerecognition.data.database.AppDatabase;
import com.example.cantonesevoicerecognition.data.database.TranscriptionDao;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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