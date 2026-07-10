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
import java.io.File
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.media3.common.util.UnstableApi

@UnstableApi
class VideoTrimActivity : AppCompatActivity() {

    private lateinit var selectionBorder: View
    private lateinit var btnPlayPause: ImageView
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
    private lateinit var playHead: View

    private var isDraggingLeft = false
    private var isDraggingRight = false
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_trim)

        playHead = findViewById(R.id.playHead)

        btnPlayPause = findViewById(R.id.btnPlayPause)

        playerView = findViewById(R.id.playerView)
        timeText = findViewById(R.id.timeText)
        trimContainer = findViewById(R.id.trimContainer)
        leftShade = findViewById(R.id.leftShade)
        rightShade = findViewById(R.id.rightShade)
        selectionBorder = findViewById(R.id.selectionBorder)


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

            val inputUri = Uri.parse(intent.getStringExtra("VIDEO_URI"))

            val outputFile = File(cacheDir, "trimmed_video.mp4")

            VideoExporter.trimVideo(
                context = this,
                inputUri = inputUri,
                startMs = trimStart,
                endMs = trimEnd,
                outputFile = outputFile,

                onSuccess = {

                    runOnUiThread {

                        val data = Intent()
                        data.putExtra("TRIMMED_VIDEO", outputFile.absolutePath)

                        if (trimEnd <= trimStart) {
                            trimEnd = videoDuration
                        }

                        setResult(RESULT_OK, data)
                        finish()
                    }
                },

                onError = {

                    runOnUiThread {

                        it.printStackTrace()
                    }
                }
            )
        }
        thumbRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        thumbRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {

                val offset = recyclerView.computeHorizontalScrollOffset()
                val range = recyclerView.computeHorizontalScrollRange()
                val extent = recyclerView.computeHorizontalScrollExtent()

                val progress =
                    offset.toFloat() / (range - extent).coerceAtLeast(1)

                val position =
                    (progress * videoDuration).toLong()

                player.seekTo(position)
            }
        })

        player = ExoPlayer.Builder(this).build()

        playerView.player = player

        btnPlayPause.setOnClickListener {

            if (isPlaying) {

                player.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                isPlaying = false

            } else {

                player.seekTo(trimStart)
                player.play()

                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                isPlaying = true
            }
        }
        playHead.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_MOVE -> {

                    var x = event.rawX

                    if (x < leftHandle.x)
                        x = leftHandle.x

                    if (x > rightHandle.x)
                        x = rightHandle.x

                    playHead.x = x

                    val time =
                        (
                                playHead.x /
                                        trimContainer.width *
                                        videoDuration
                                ).toLong()

                    player.seekTo(time)
                }
            }

            true
        }

        playerView.useController = false

        val uri = Uri.parse(intent.getStringExtra("VIDEO_URI"))

        player.setMediaItem(MediaItem.fromUri(uri))

        player.prepare()

        if (videoDuration > 0) {
            val progress =
                player.currentPosition.toFloat() / videoDuration

            val x = progress * trimContainer.width

            playHead.x = x.coerceIn(
                leftHandle.x + leftHandle.width / 2f,
                rightHandle.x + rightHandle.width / 2f
            )
        }
        val handler = Handler(Looper.getMainLooper())

        handler.post(object : Runnable {

            override fun run() {

                if (::player.isInitialized) {

                    val progress =
                        player.currentPosition.toFloat() / videoDuration

                    val x = progress * trimContainer.width

                            playHead.x = x.coerceIn(
                        leftHandle.x,
                        rightHandle.x
                    )
                    if (player.currentPosition >= trimEnd) {

                        player.pause()
                        player.seekTo(trimStart)

                        playHead.x = leftHandle.x

                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        isPlaying = false
                    }

                }

                handler.postDelayed(this, 16)
            }

        })
        player.pause()      // Don't autoplay
        player.seekTo(0)
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {

                if (state == Player.STATE_READY) {

                    videoDuration = player.duration

                    if (trimEnd == 0L) {
                        trimStart = 0L
                        trimEnd = videoDuration
                    }

                    timeText.text =
                        "${trimStart / 1000}s - ${trimEnd / 1000}s"
                }
            }
        })
        generateThumbnails(uri)
        leftHandle.bringToFront()
        rightHandle.bringToFront()

        selectionBorder.bringToFront()
        leftHandle.bringToFront()
        rightHandle.bringToFront()
        playHead.bringToFront()

        leftHandle.setOnTouchListener { _, event ->


            when (event.action) {

                MotionEvent.ACTION_UP -> {

                    player.pause()
                    player.seekTo(trimStart)
                }

                MotionEvent.ACTION_MOVE -> {

                    val location = IntArray(2)
                    trimContainer.getLocationOnScreen(location)

                    var newX = event.rawX - location[0] - leftHandle.width / 2f

                    // Don't go outside the timeline
                    if (newX < 0f) {
                        newX = 0f
                    }

                    // Don't cross the right handle
                    val minGap =
                        trimContainer.width * 1000f / videoDuration

                    val maxX =
                        rightHandle.x - minGap

                    if (newX > maxX) {
                        newX = maxX
                    }

                    leftHandle.x = newX


                    updateShades()

                    selectionBorder.invalidate()

                    trimStart = (
                            leftHandle.x /
                                    trimContainer.width.toFloat() *
                                    videoDuration
                            ).toLong()

                    // Show the frame at the selected start time
                    player.seekTo(trimStart)

                    timeText.text =
                        "${trimStart / 1000}s - ${trimEnd / 1000}s"
                }

                MotionEvent.ACTION_UP -> {
                    // Keep the video paused
                    player.pause()
                    player.seekTo(trimStart)
                }
            }

            true
        }
        rightHandle.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_UP -> {

                    player.pause()
                    player.seekTo(trimEnd)
                }

                MotionEvent.ACTION_MOVE -> {

                    val location = IntArray(2)
                    trimContainer.getLocationOnScreen(location)

                    var newX = event.rawX - location[0] - rightHandle.width / 2f

                    // Prevent crossing the left handle
                    val minGap =
                        trimContainer.width * 1000f / videoDuration

                    if (newX < leftHandle.x + minGap) {
                        newX = leftHandle.x + minGap
                    }

                    // Prevent going outside the timeline
                    val maxX = (trimContainer.width - rightHandle.width).toFloat()
                    if (newX > maxX) {
                        newX = maxX
                    }

                    rightHandle.x = newX

                    updateShades()

                    trimEnd = (
                            (rightHandle.x + rightHandle.width) /
                                    trimContainer.width.toFloat() *
                                    videoDuration
                            ).toLong()

                    if (trimEnd > videoDuration) {
                        trimEnd = videoDuration
                    }

                    // Preview the selected end frame
                    player.seekTo(trimEnd)
                    playHead.x = rightHandle.x

                    timeText.text =
                        "${trimStart / 1000}s - ${trimEnd / 1000}s"
                }

                MotionEvent.ACTION_UP -> {
                    // Stay paused and show the selected frame
                    player.pause()
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
            updateShades()
            selectionBorder.bringToFront()
            leftHandle.bringToFront()
            rightHandle.bringToFront()
            playHead.bringToFront()
        }

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

    private fun updateShades() {

        // Left dark area
        leftShade.x = 0f
        leftShade.layoutParams.width = leftHandle.x.toInt()

        // Right dark area
        rightShade.x = rightHandle.x + rightHandle.width
        rightShade.layoutParams.width =
            (trimContainer.width - rightShade.x).toInt()

        // White selection border
        selectionBorder.x = leftHandle.x
        selectionBorder.layoutParams.width =
            (rightHandle.x - leftHandle.x + rightHandle.width).toInt()

        leftShade.requestLayout()
        rightShade.requestLayout()
        selectionBorder.requestLayout()
    }
}