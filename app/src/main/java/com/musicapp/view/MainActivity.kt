package com.musicapp.view

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.musicapp.R
import com.musicapp.Utils
import com.musicapp.adapter.AudioListAdapter
import com.musicapp.model.AudioTable
import com.musicapp.service.MediaPlayerService
import com.musicapp.viewmodel.AudioViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), AudioListAdapter.OnAudioClickListener,
    MediaPlayerService.OnNotificationActionClickListener {

    private var pause: Boolean = false
    private lateinit var mediaPlayer: MediaPlayer
    private val viewModel: AudioViewModel by viewModels()
    private val audioList = ArrayList<AudioTable>()
    private lateinit var adapter: AudioListAdapter
    private lateinit var mediaPlayerService: MediaPlayerService
    private var mBound: Boolean = false

    companion object {
        const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress_bar.visibility = View.VISIBLE
        /**
         * This will return a list of all MP3 files from Database.
         */
        viewModel.getAllAudios()

        recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        adapter = AudioListAdapter(audioList, this)
        recycler_view.adapter = adapter

        /*On Pull to Refresh, the app will sync the songs from device*/
        swipeRefreshLayout.setOnRefreshListener {
            progress_bar.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
            audioList.clear()
            getDataFromDevice()
        }

        control_button.setOnClickListener {
            if (pause) {
                resumeSong()
            } else {
                pauseSong()
            }
        }

        viewModel.audioListLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
//                If there is no data in local db
                getDataFromDevice()
            } else {
                audioList.addAll(it)
                progress_bar.visibility = View.GONE
                text_no_music.visibility = View.GONE
                adapter.notifyDataSetChanged()
            }
        })


    }

    override fun onStart() {
        super.onStart()
//        Bind to MediaPlayerService
        Intent(this, MediaPlayerService::class.java).also { intent->
            bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        mBound = false
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.LocalBinder
            mediaPlayerService = binder.getService()
            mBound = true
            mediaPlayerService.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    private fun getDataFromDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isReadPhoneStatePermissionGranted()) {
                getAllAudioFromDevice()
            } else {
                progress_bar.visibility = View.GONE
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE_CODE
                )
            }
        } else {
            getAllAudioFromDevice()
        }
    }

    private fun getAllAudioFromDevice() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC"
        val c: Cursor? = contentResolver.query(uri, projection, selection, null, sortOrder)
        if (c == null) {
            Toast.makeText(this, getString(R.string.cursor_error), Toast.LENGTH_LONG).show();
        } else if (!c.moveToFirst()) {
            text_no_music.visibility = View.VISIBLE
            Toast.makeText(this, getString(R.string.no_music_found), Toast.LENGTH_LONG).show();
        } else {
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID))
                val path = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA))
                val title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val albumId = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                val duration = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION))

                if (path != null && path.endsWith(".mp3") && (duration == null || duration.toInt() != 0)) {
                    // Create a model object.
                    val audioTable = AudioTable(id.toInt(), title, path, artist, albumId, duration)
                    viewModel.addAudio(audioTable)

                    // Add the model object to the list .
                    audioList.add(audioTable)
                }
            }
            c.close()
        }
        progress_bar.visibility = View.GONE
        text_no_music.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun isReadPhoneStatePermissionGranted(): Boolean {
        val firstPermissionResult = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return firstPermissionResult == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE_CODE -> if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {// Permission Granted
                    progress_bar.visibility = View.VISIBLE
                    getAllAudioFromDevice()
                } else {
                    // Permission Denied
                    Toast.makeText(
                        this,
                        getString(R.string.read_persmission_error),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    text_no_music.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onAudioClick(path: String, audioTable: AudioTable) {
        player_layout.visibility = View.VISIBLE
        song_name.text = audioTable.songName
        artist_name.text = audioTable.artistName

        val bitmap = Utils.getAlbumImage(audioTable.path)
        if (bitmap!=null)
        album_image.setImageBitmap(bitmap)
        startService(
            Intent(
                this,
                MediaPlayerService::class.java
            ).setAction(MediaPlayerService.ACTION_PLAY)
                .putExtra("TITLE",audioTable.songName)
                .putExtra("ARTIST",audioTable.artistName)
        )

        playSong(Uri.parse(path))
    }

    private fun initialiseMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
        mediaPlayer.setOnCompletionListener {
            control_button.setImageResource(R.drawable.ic_play)
            player_layout.visibility = View.GONE
        }
    }

    private fun playSong(uri: Uri) {
        try {
            if (!this::mediaPlayer.isInitialized) {
                initialiseMediaPlayer()
            }
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.apply {
                setDataSource(
                    applicationContext,
                    uri
                ) //to set media source and send the object to the initialized state
                prepare() //to send the object to prepared state
                start() //to start the music and send the object to the started state
            }
            control_button.setImageResource(R.drawable.ic_pause)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.play_error), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun resumeSong() {
        if (pause) { //initially, pause is set to false
            mediaPlayer.seekTo(mediaPlayer.currentPosition)
            mediaPlayer.start()
            pause = false
            control_button.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun pauseSong() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            control_button.setImageResource(R.drawable.ic_play)
            pause = true
        }
    }

    override fun onNotificationActionClick(needToPlay: Boolean) {
        if (needToPlay){
            resumeSong()
        }else{
            pauseSong()
        }
    }

}