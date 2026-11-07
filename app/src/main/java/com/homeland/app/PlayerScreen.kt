package com.homeland.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    title: String,
    videoUrl: String,
    episodes: List<Episode> = emptyList(),
    startPosition: Long = 0L,
    subtitleUrl: String? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(startPosition) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLandscape by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(subtitleUrl != null) }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }

    // 🔥 DATA METER STATE
    var dataUsedMb by remember { mutableDoubleStateOf(0.0) }

    val trackSelector = remember { DefaultTrackSelector(context) }
    var currentQuality by remember { mutableStateOf("Auto") }

    val focusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build().apply {
                if (episodes.isNotEmpty()) {
                    val mediaItems = episodes.map { ep ->
                        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder().setTitle("${ep.season} ${ep.episode}").build()
                        MediaItem.Builder().setUri(ep.url).setMediaMetadata(mediaMetadata).build()
                    }
                    setMediaItems(mediaItems)
                } else {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                }
                seekTo(startPosition)
                prepare()
                playWhenReady = true
            }
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)

        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // 🔥 DATA METER CALCULATION LOOP
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                // Get current bitrate. If it's 0 or null, default to 1.2 Mbps
                val formatBitrate = exoPlayer.videoFormat?.bitrate
                val currentBitrate = if (formatBitrate != null && formatBitrate > 0) formatBitrate else 1200000
                
                val bytesPerSecond = currentBitrate / 8
                val mbPerSecond = bytesPerSecond.toDouble() / (1024 * 1024)
                dataUsedMb += mbPerSecond
            }
            delay(1000) // Update every second
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { 
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent: KeyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.MediaPlayPause, Key.Spacebar, Key.Enter, Key.DirectionCenter -> {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                            true
                        }
                        Key.MediaFastForward, Key.DirectionRight -> {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                            showControls = true
                            true
                        }
                        Key.MediaRewind, Key.DirectionLeft -> {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                            showControls = true
                            true
                        }
                        Key.Back, Key.Escape -> {
                            exoPlayer.pause()
                            onClose()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false 
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
                view.player?.setPlaybackSpeed(playbackSpeed)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 🔥 PERMANENT DATA METER (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp, start = 20.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = String.format("%.2f MB", dataUsedMb),
                color = Color(0xFF10B981), // Green text
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 🔥 PERMANENT WATERMARK (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("HOMELAND APP", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // CONTROLS OVERLAY
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                
                // Top bar (Title & Close)
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)).padding(horizontal = 16.dp, vertical = 12.dp).align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exoPlayer.pause(); onClose() }) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                    // Left space empty since Watermark is now permanent
                    Spacer(modifier = Modifier.width(80.dp))
                }

                IconButton(
                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.size(80.dp).align(Alignment.Center).background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp)) }

                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                        Slider(value = currentPosition.toFloat(), onValueChange = { exoPlayer.seekTo(it.toLong()) }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFFE50914), activeTrackColor = Color(0xFFE50914), inactiveTrackColor = Color.DarkGray), modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                        Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        if (subtitleUrl != null) {
                            IconButton(onClick = { subtitlesEnabled = !subtitlesEnabled }) { 
                                Icon(Icons.Default.ClosedCaption, "CC", tint = if (subtitlesEnabled) Color(0xFFE50914) else Color.White) 
                            }
                        }

                        Box {
                            IconButton(onClick = { showSpeedMenu = !showSpeedMenu; showQualityMenu = false; showAspectMenu = false }) { Icon(Icons.Default.Speed, "Speed", tint = Color.White) }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                listOf(0.5f, 1f, 1.5f, 2f).forEach { speed -> DropdownMenuItem(text = { Text("${speed}x", color = if(playbackSpeed == speed) Color(0xFFE50914) else Color.White) }, onClick = { playbackSpeed = speed; showSpeedMenu = false }) }
                            }
                        }

                        Box {
                            IconButton(onClick = { showQualityMenu = !showQualityMenu; showSpeedMenu = false; showAspectMenu = false }) { Icon(Icons.Default.HighQuality, "Quality", tint = Color.White) }
                            DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                listOf("Auto", "1080p", "720p", "480p").forEach { quality ->
                                    DropdownMenuItem(text = { Text(quality, color = if(currentQuality == quality) Color(0xFFE50914) else Color.White) }, onClick = { 
                                        currentQuality = quality
                                        showQualityMenu = false
                                        val params = trackSelector.buildUponParameters()
                                        if (quality == "Auto") { params.clearVideoSizeConstraints() } 
                                        else if (quality == "1080p") { params.setMaxVideoSize(1920, 1080).setMaxVideoBitrate(5000000) }
                                        else if (quality == "720p") { params.setMaxVideoSize(1280, 720).setMaxVideoBitrate(2500000) }
                                        else if (quality == "480p") { params.setMaxVideoSize(854, 480).setMaxVideoBitrate(1000000) }
                                        trackSelector.parameters = params.build()
                                    })
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { showAspectMenu = !showAspectMenu; showSpeedMenu = false; showQualityMenu = false }) { Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White) }
                            DropdownMenu(expanded = showAspectMenu, onDismissRequest = { showAspectMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                listOf("Fit", "Fill", "Zoom").forEach { aspect ->
                                    DropdownMenuItem(text = { Text(aspect, color = Color.White) }, onClick = { 
                                        resizeMode = when(aspect) { "Fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT; "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL; else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM }
                                        showAspectMenu = false
                                    })
                                }
                            }
                        }

                        // 🔥 PIP BUTTON (Fixed)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                try {
                                    val aspectRatio = Rational(16, 9)
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(aspectRatio)
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                    showControls = false // Hide controls when entering PiP
                                } catch (e: Exception) {
                                    // Catch error if manifest is not updated
                                }
                            }) { 
                                Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White) 
                            }
                        }

                        IconButton(onClick = {
                            if (isLandscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            isLandscape = !isLandscape
                        }) { Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Rotate", tint = Color.White) }
                    }
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
