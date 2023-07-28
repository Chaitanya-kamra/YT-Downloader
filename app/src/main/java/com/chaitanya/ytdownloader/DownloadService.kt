package com.chaitanya.ytdownloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadService:Service() {

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"
    }
    private val channelId = "download_channel"

    private var notificationTitle = "Downloading Video"
    private var notificationText = "Download in progress"

    private lateinit var notificationManager: NotificationManager

    private lateinit var client: OkHttpClient
    private lateinit var request: Request
    private lateinit var call: Call



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.e("update","okay create")
        client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)

            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            val videoUrl = intent.getStringExtra("VIDEO_URL")
            val outputUri = intent.getStringExtra("OUTPUT_URI").toString()
            val notificationTitle = intent.getStringExtra("VIDEO_TITLE").toString()
            val notification = createForegroundNotification()
            startForeground(startId,notification.build() )
            if (videoUrl != null) {

                downloadVideo(videoUrl,outputUri,startId,notificationTitle,notification)
                Log.e("update","okay download")
            }
        }
        return START_NOT_STICKY
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Download Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun createForegroundNotification(): NotificationCompat.Builder {
        val cancelIntent = Intent(this, DownloadService::class.java)
        cancelIntent.action = ACTION_CANCEL_DOWNLOAD
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setProgress(100,0,false)
            .addAction(R.drawable.ic_action_cross, "CANCEL", cancelPendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)


        return notificationBuilder
    }

    private fun downloadVideo(videoUrl: String, outputUri: String, startId: Int,notificationTitle:String,notification : NotificationCompat.Builder) {
        request = Request.Builder()
            .url(videoUrl)
            .header("Connection", "close")
            .build()
        call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle download failure
                stopSelf() // Stop the service when download fails
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    saveToFile(responseBody,outputUri,startId,notificationTitle,notification)

                }
                stopSelf()

                notificationManager.cancel(startId)
            }
        })
    }

    private fun saveToFile(responseBody: ResponseBody,outputUri: String,startId: Int,notificationTitle: String,notification: Builder) {
        val bytes = ByteArray(4096)
        val inputStream = responseBody.byteStream()

        val outputStream = contentResolver.openOutputStream(Uri.parse(outputUri))


        val totalFileSize = responseBody.contentLength()
        var fileSizeDownloaded = 0L

        while (true) {
            val read = inputStream.read(bytes)
            if (read == -1) break

            outputStream!!.write(bytes, 0, read)
            fileSizeDownloaded += read.toLong()
            val progress = ((fileSizeDownloaded * 100) / totalFileSize).toInt()
            updateProgressNotification(progress,startId,totalFileSize,fileSizeDownloaded,notificationTitle,notification)

        }

        outputStream!!.flush()
        outputStream.close()
        inputStream.close()
    }

    private fun updateProgressNotification(progress: Int,startId: Int,totalFileSize:Long,downloadedSize:Long,notificationTitle: String,notification: Builder) {

        val notificationBuilder = notification.setContentTitle(notificationTitle).setProgress(100,progress,false)
            .setContentText("Downloaded ${getSizeInMB(downloadedSize)}MB / ${getSizeInMB(totalFileSize)}MB")
        notificationManager.notify(startId, notificationBuilder.build())

    }

    private fun getSizeInMB(bytes: Long): String {
        return String.format("%.2f", bytes / (1024.0 * 1024.0))
    }


}