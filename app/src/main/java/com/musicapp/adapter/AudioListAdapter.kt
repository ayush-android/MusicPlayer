package com.musicapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musicapp.R
import com.musicapp.Utils
import com.musicapp.model.AudioTable
import kotlinx.android.synthetic.main.item_audio.view.*

class AudioListAdapter(private val audioList: List<AudioTable>, private val audioClickListener: OnAudioClickListener) : RecyclerView.Adapter<AudioListAdapter.AudioViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio,parent,false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.apply {
            val raw = audioList[position]
            songName.text = raw.songName
            songAlbum.text = raw.artistName
            val bitmap = Utils.getAlbumImage(raw.path)
            if (bitmap!=null)
                songImg.setImageBitmap(bitmap)
            else songImg.setImageResource(R.drawable.placeholder)

            itemView.setOnClickListener{
                audioClickListener.onAudioClick(raw.path,raw)
            }
        }
    }

    override fun getItemCount(): Int {
        return audioList.size
    }

    class AudioViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val songImg = itemView.music_image
        val songName = itemView.music_item_name_text_view
        val songAlbum = itemView.music_album_text_view
    }

    interface OnAudioClickListener{
        fun onAudioClick(path: String, audioTable: AudioTable)
    }

}