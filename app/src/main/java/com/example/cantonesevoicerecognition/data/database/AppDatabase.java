package com.example.cantonesevoicerecognition.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;

@Database(
         entities = {TranscriptionRecord.class},
         version = 1,
         exportSchema = false
     )
     public abstract class AppDatabase extends RoomDatabase {
         public abstract TranscriptionDao transcriptionDao();
         
         private static volatile AppDatabase INSTANCE;
         
         public static AppDatabase getDatabase(final Context context) {
             if (INSTANCE == null) {
                 synchronized (AppDatabase.class) {
                     if (INSTANCE == null) {
                         INSTANCE = Room.databaseBuilder(
                             context.getApplicationContext(),
                             AppDatabase.class,
                             "transcription_database"
                         ).build();
                     }
                 }
             }
             return INSTANCE;
         }
     }