package com.example.cantonesevoicerecognition.data.database;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import java.util.List;

@Dao
     public interface TranscriptionDao {
         @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
         List<TranscriptionRecord> getAllTranscriptions();
         
         @Query("SELECT * FROM transcriptions WHERE id = :id")
         TranscriptionRecord getTranscriptionById(long id);
         
         @Query("SELECT * FROM transcriptions WHERE original_text LIKE :query OR edited_text LIKE :query")
         List<TranscriptionRecord> searchTranscriptions(String query);
         
         @Insert
         long insertTranscription(TranscriptionRecord record);
         
         @Update
         void updateTranscription(TranscriptionRecord record);
         
         @Delete
         void deleteTranscription(TranscriptionRecord record);
         
         @Query("DELETE FROM transcriptions WHERE id = :id")
         void deleteTranscriptionById(long id);
     }