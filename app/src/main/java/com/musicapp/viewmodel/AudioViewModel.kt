package com.musicapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.musicapp.model.AudioTable
import com.musicapp.repository.AudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioViewModel(val app: Application):AndroidViewModel(app) {

    private var audioMutableLiveData = MutableLiveData<MutableList<AudioTable>?>()
    var audioListLiveData: LiveData<MutableList<AudioTable>?> = audioMutableLiveData

    fun addAudio(audioTable: AudioTable){
        CoroutineScope(Dispatchers.IO).launch {
            AudioRepository().addAudio(app.applicationContext,audioTable)
        }
    }

    fun getAllAudios(){
        CoroutineScope(Dispatchers.IO).launch {
            val list = AudioRepository().getAudios(app.applicationContext)
            if (list != null) {
                audioMutableLiveData.postValue(list)
            }
            else audioMutableLiveData.postValue(null)
        }
    }

}