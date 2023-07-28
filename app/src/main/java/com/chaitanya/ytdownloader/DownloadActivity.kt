package com.chaitanya.ytdownloader

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.chaitanya.ytdownloader.databinding.ActivityDownloadBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.pow

class DownloadActivity : AppCompatActivity() {

    lateinit var binding: ActivityDownloadBinding
    private var folderPath: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val title = intent.getStringExtra("title")
        val image = intent.getStringExtra("image")
        val views = intent.getStringExtra("views")
        val likes = intent.getStringExtra("likes")
        val stream = intent.getStringExtra("stream")
        val folderPathCont = intent.getParcelableExtra<Uri>("folder")

        // Set the UI elements with video information
        binding.tvTitle.text = title
        binding.tvView.text = "${withSuffix(views!!.toLong())} Views"
        binding.tvLikes.text = "${withSuffix(likes!!.toLong())} Likes"
        binding.btnAnother.setOnClickListener {
            finish()
        }
        // Set the folder path and initiate the video download
        folderPath = folderPathCont
        val documentFile = DocumentFile.fromTreeUri(this, folderPath!!)
        val file = documentFile?.createFile("video/mp4", "$title.mp4")
        if (stream != null) {
            Log.e("af",stream.toString())

                val intent = Intent(this@DownloadActivity, DownloadService::class.java)
                intent.putExtra("OUTPUT_URI", file?.uri.toString())
                intent.putExtra("VIDEO_URL", stream)
            intent.putExtra("VIDEO_TITLE", title)
                ContextCompat.startForegroundService(this@DownloadActivity, intent)

        }
        // Load the video thumbnail using Picasso library
        Picasso.get().load(image).into(binding.ivThumbnail)
    }
    private fun withSuffix(count: Long): String {
        if (count < 1000) return "" + count
        val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
        return String.format(
            "%.1f %c",
            count / 1000.0.pow(exp.toDouble()),
            "kMPEG"[exp - 1]
        )
    }
}