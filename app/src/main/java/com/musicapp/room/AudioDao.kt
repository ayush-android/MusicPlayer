package com.musicapp.room

import androidx.room.*
import com.musicapp.model.AudioTable

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audio: AudioTable): Long

    @Query("SELECT * FROM Audio ORDER BY songName ASC")
    fun loadAll(): MutableList<AudioTable>?

    @Query("SELECT * FROM Audio where songName = :audioTitle")
    fun loadOneBySongTitle(audioTitle: String): AudioTable?

    @Update
    fun update(audio: AudioTable)
}