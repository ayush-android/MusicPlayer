package com.musicapp.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "Audio")
@Parcelize
data class AudioTable(@PrimaryKey var id: Int,
                      var songName: String?,
                      var path: String,
                      var artistName: String?,
                      var albumArt: String?,
                      var duration: String?) : Parcelable