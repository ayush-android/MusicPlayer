package com.musicapp.service

import android.app.*
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.musicapp.R


class MediaPlayerService : Service() {
    private var initiateCallback: Boolean = false
    private val mManager: MediaSessionManager? = null
    private var mSession: MediaSession? = null
    private var mController: MediaController? = null
    private lateinit var mListener: OnNotificationActionClickListener
    private var title: String? = ""
    private var artistName: String? = ""

    // Binder given to clients
    private val binder = LocalBinder()

    companion object {
        val ACTION_PLAY = "action_play"
        val ACTION_PAUSE = "action_pause"
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        // Return this instance of MediaPlayerService so clients can call public methods
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mManager == null) {
            initMediaSessions()
        }
        handleIntent(intent)
        return super.onStartCommand(intent, flags, startId)
    }



    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return
        if (intent.extras != null) {
            title = intent.extras?.getString("TITLE")
            artistName = intent.extras?.getString("ARTIST")
            initiateCallback = false
        } else {
            initiateCallback = true
        }

        val action = intent.action
        if (action.equals(ACTION_PLAY, ignoreCase = true)) {
            /*createNotification(
                generateAction(R.drawable.ic_pause, "Pause", ACTION_PAUSE))*/
            mController!!.transportControls.play()
        } else if (action.equals(ACTION_PAUSE, ignoreCase = true)) {
            mController!!.transportControls.pause()
        }
    }

    fun setCallback(listener: OnNotificationActionClickListener) {
        mListener = listener
    }

    private fun generateAction(
        icon: Int,
        title: String,
        intentAction: String
    ): NotificationCompat.Action {
        val intent = Intent(applicationContext, MediaPlayerService::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private var CHANNEL_ID: String? = null

    private fun createNotificationChannel() {
        val channelName: CharSequence? = CHANNEL_ID
        val channelDesc = "channelDesc"
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance)
            channel.description = channelDesc
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )!!
//            Mute sound in notification
            channel.setSound(null, null)
            val currChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (currChannel == null) notificationManager.createNotificationChannel(channel)
        }
    }


    fun createNotification(action: NotificationCompat.Action) {
        CHANNEL_ID = "Channel ID"
        if (title != null) {
            createNotificationChannel()
//            val style = androidx.media.app.NotificationCompat.MediaStyle()
            val intent = Intent(this, MediaPlayerService::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            val mBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(this, CHANNEL_ID!!)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(artistName)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            mBuilder.addAction(action)
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(123, mBuilder.build())
        }
    }

    private fun initMediaSessions() {
        mSession = MediaSession(applicationContext, "simple player session")
        mController = MediaController(applicationContext, mSession!!.getSessionToken())
        mSession!!.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                createNotification(
                    generateAction(R.drawable.ic_pause, "Pause", ACTION_PAUSE)
                )
                if (initiateCallback)
                    mListener.onNotificationActionClick(true)
            }

            override fun onPause() {
                super.onPause()
                createNotification(
                    generateAction(R.drawable.ic_play, "Play", ACTION_PLAY)
                )
                if (initiateCallback)
                    mListener.onNotificationActionClick(false)
            }
        }
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mSession!!.release()
        return super.onUnbind(intent)
    }

    interface OnNotificationActionClickListener {
        fun onNotificationActionClick(needToPlay: Boolean)
    }
}