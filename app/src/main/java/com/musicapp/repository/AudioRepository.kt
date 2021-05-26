package com.musicapp.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.musicapp.model.AudioTable
import com.musicapp.room.AudioDatabase

class AudioRepository {

        var audioDatabase: AudioDatabase? = null
        var audioTableLiveData: LiveData<AudioTable>? = null

        private fun initializeDB(context: Context): AudioDatabase {
            return AudioDatabase.getDatabaseClient(context)
        }

        suspend fun addAudio(context: Context, audioTable: AudioTable) {
            audioDatabase = initializeDB(context)
            audioDatabase!!.audioDao().insert(audioTable)
        }

        suspend fun getAudios(context: Context): MutableList<AudioTable>? {
            audioDatabase = initializeDB(context)
            return audioDatabase!!.audioDao().loadAll()
        }
}