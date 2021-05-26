package com.musicapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

object Utils {

    fun getAlbumImage(path: String): Bitmap? {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val data = mmr.embeddedPicture
            if (data != null) BitmapFactory.decodeByteArray(data, 0, data.size) else null
        }catch (e:Exception){
            e.printStackTrace()
            null
        }
    }
}