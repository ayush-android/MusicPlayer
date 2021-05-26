package com.musicapp.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.musicapp.model.AudioTable


@Database(entities = [AudioTable::class],version = 1,exportSchema = false)
abstract class AudioDatabase:RoomDatabase() {

    abstract fun audioDao():AudioDao

    companion object{
        const val DB_NAME = "MusicApp.db"
        @Volatile
        private var INSTANCE: AudioDatabase? = null

        fun getDatabaseClient(context: Context) : AudioDatabase{

            if (INSTANCE != null) return INSTANCE!!

            synchronized(this){
                INSTANCE = Room.databaseBuilder(context,AudioDatabase::class.java, DB_NAME)
                    .build()
            }
            return INSTANCE!!
        }
    }
}