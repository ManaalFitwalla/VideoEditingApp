package com.example.fabcut

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.view.View
import android.widget.FrameLayout
import androidx.media3.common.Player
import android.widget.TextView
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import java.io.File
import android.widget.Toast
import kotlin.text.compareTo


class VideoTrimActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private lateinit var thumbRecycler: RecyclerView

    private lateinit var leftHandle: ImageView
    private lateinit var rightHandle: ImageView

    private lateinit var leftShade: View
    private lateinit var rightShade: View

    private lateinit var trimContainer: FrameLayout
    private lateinit var timeText: TextView
    private lateinit var btnClose: ImageView
    private val thumbnailList = ArrayList<Bitmap>()
    private lateinit var btnDone: ImageView
    private var startX = 0f
    private var endX = 0f

    private var startTimeMs = 0L
    private var endTimeMs = 0L

    private var videoDuration = 0L
    private var trimStart = 0L
    private var trimEnd = 0L

    private var isDraggingLeft = false
    private var isDraggingRight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_trim)

        btnDone = findViewById(R.id.btnDone)
        android.widget.Toast.makeText(
            this,
            "VideoTrimActivity Opened",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        playerView = findViewById(R.id.playerView)
        timeText = findViewById(R.id.timeText)
        trimContainer = findViewById(R.id.trimContainer)

        thumbRecycler = findViewById(R.id.thumbRecycler)

        leftHandle = findViewById(R.id.leftHandle)

        rightHandle = findViewById(R.id.rightHandle)

        thumbRecycler = findViewById(R.id.thumbRecycler)

        btnClose = findViewById(R.id.btnclose)
        btnDone = findViewById(R.id.btnDone)
        val outputFile = File(cacheDir, "trimmed_video.mp4")
        btnClose.setOnClickListener {
            finish()
        }

        btnDone.setOnClickListener {

            Toast.makeText(
                this,
                "Export Started",
                Toast.LENGTH_SHORT
            ).show()

        }
        thumbRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        player = ExoPlayer.Builder(this).build()

        playerView.player = player

        val uri = Uri.parse(intent.getStringExtra("VIDEO_URI"))

        player.setMediaItem(MediaItem.fromUri(uri))

        player.prepare()
        player.play()
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {

                if (state == Player.STATE_READY) {

                    videoDuration = player.duration

                    trimStart = 0
                    trimEnd = videoDuration

                    timeText.text =
                        "0s - ${videoDuration / 1000}s"                }
            }
        })
        generateThumbnails(uri)
        leftHandle.bringToFront()
        rightHandle.bringToFront()

        leftHandle.setOnTouchListener { v, event ->

            when(event.action){

                android.view.MotionEvent.ACTION_MOVE->{

                    var newX = event.x + v.x

                    if(newX<0)
                        newX=0f

                    if(newX>rightHandle.x-120)
                        newX=rightHandle.x-120

                    leftHandle.x = newX
                    player.seekTo(trimStart)
                    trimStart =
                        (
                                leftHandle.x/
                                        trimContainer.width*
                                        videoDuration
                                ).toLong()
                    player.seekTo(trimStart)

                    timeText.text =
                        "${trimStart/1000}s - ${trimEnd/1000}s"

                }

            }

            true
        }
        rightHandle.setOnTouchListener { v, event ->

            when(event.action){

                android.view.MotionEvent.ACTION_MOVE->{

                    var newX = event.x + v.x

                    if(newX<leftHandle.x+120)
                        newX=leftHandle.x+120

                    if(newX>trimContainer.width-rightHandle.width)
                        newX=(trimContainer.width-rightHandle.width).toFloat()

                    rightHandle.x=newX
                    trimEnd =
                        (
                                rightHandle.x/
                                        trimContainer.width*
                                        videoDuration
                                ).toLong()

                    timeText.text =
                        "${trimStart/1000}s - ${trimEnd/1000}s"
                    player.seekTo(trimEnd)

                }

            }

            true
        }
        trimContainer.post {

            startX = 0f

            endX = (trimContainer.width - rightHandle.width).toFloat()

            leftHandle.x = startX

            rightHandle.x = endX
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        handler.post(object : Runnable {
            override fun run() {

                if (::player.isInitialized && player.isPlaying) {

                    if (player.currentPosition >= trimEnd) {
                        player.seekTo(trimStart)
                    }
                }

                handler.postDelayed(this, 100)
            }
        })
}


    private fun generateThumbnails(videoUri: Uri) {


        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(this, videoUri)

        val duration =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

        thumbnailList.clear()

        val frameCount = 10

        for (i in 0 until frameCount) {

            val timeUs =
                (duration * 1000 / frameCount) * i

            val bitmap =
                retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

            bitmap?.let {
                thumbnailList.add(it)
            }
        }

        retriever.release()

        thumbRecycler.adapter =
            ThumbnailAdapter(thumbnailList)
    }


    override fun onStop() {
        super.onStop()
        player.release()
    }
}