package com.homeland.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs

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
    val configuration = LocalConfiguration.current
    
    // 🔥 NEW: RESUME DIALOG STATE
    var showResumeDialog by remember { mutableStateOf(startPosition > 5000L) }

    var isPlaying by remember { mutableStateOf(startPosition <= 5000L) }
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
    
    // 🔥 NEW: SLEEP TIMER STATE
    var showSleepMenu by remember { mutableStateOf(false) }
    var sleepTimerMillis by remember { mutableLongStateOf(0L) }

    // 🔥 DATA METER STATE
    var dataUsedMb by remember { mutableDoubleStateOf(0.0) }

    val trackSelector = remember { DefaultTrackSelector(context) }
    var currentQuality by remember { mutableStateOf("Auto") }

    val focusRequester = remember { FocusRequester() }

    // 🔥 NEW: AUDIO MANAGER FOR VOLUME GESTURES
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // 🔥 NEW: GESTURE STATES
    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureType by remember { mutableStateOf("") } // "volume", "brightness", "seek"
    var gestureValue by remember { mutableFloatStateOf(0f) } // 0f to 1f
    var seekPreviewTime by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true // handles audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK) // 🔥 Keeps playing when screen is locked
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
                
                if (startPosition <= 5000L) {
                    seekTo(startPosition)
                    playWhenReady = true
                } else {
                    seekTo(0) // Start at 0, wait for user prompt
                    playWhenReady = false
                }
                prepare()
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
                val formatBitrate = exoPlayer.videoFormat?.bitrate
                val currentBitrate = if (formatBitrate != null && formatBitrate > 0) formatBitrate else 1200000
                val bytesPerSecond = currentBitrate / 8
                val mbPerSecond = bytesPerSecond.toDouble() / (1024 * 1024)
                dataUsedMb += mbPerSecond
            }
            delay(1000)
        }
    }
    
    // 🔥 SLEEP TIMER LOOP
    LaunchedEffect(sleepTimerMillis) {
        if (sleepTimerMillis > 0) {
            delay(1000)
            sleepTimerMillis -= 1000
            if (sleepTimerMillis <= 0) {
                exoPlayer.pause()
            }
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !showResumeDialog) {
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

        // 🔥 GESTURE DETECTOR OVERLAY
        // This Box sits invisibly over the video to catch swipes without blocking the UI clicks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (showControls) return@detectDragGestures // Ignore gestures if controls are open
                            
                            val isLeftSide = offset.x < configuration.screenWidthDp / 2
                            gestureType = if (isLeftSide) "brightness" else "volume"
                            showGestureOverlay = true
                            
                            if (gestureType == "brightness") {
                                gestureValue = activity?.window?.attributes?.screenBrightness ?: 0.5f
                            } else {
                                gestureValue = currentVolume.toFloat() / maxVolume.toFloat()
                            }
                            seekPreviewTime = exoPlayer.currentPosition
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            // Determine if it's a horizontal seek or vertical volume/brightness
                            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                // Horizontal Seek
                                gestureType = "seek"
                                val seekSensitivity = 500f 
                                val seekDelta = (dragAmount.x * seekSensitivity).toLong()
                                seekPreviewTime = (seekPreviewTime + seekDelta).coerceIn(0L, duration)
                                gestureValue = if (duration > 0) seekPreviewTime.toFloat() / duration.toFloat() else 0f
                            } else {
                                // Vertical Volume or Brightness
                                val delta = -dragAmount.y / 300f 
                                gestureValue = (gestureValue + delta).coerceIn(0f, 1f)
                                
                                if (gestureType == "volume") {
                                    val newVol = (gestureValue * maxVolume).toInt()
                                    if (newVol != currentVolume) {
                                        currentVolume = newVol
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    }
                                } else if (gestureType == "brightness") {
                                    activity?.let { act ->
                                        val params = act.window.attributes
                                        params.screenBrightness = gestureValue
                                        act.window.attributes = params
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (gestureType == "seek") {
                                exoPlayer.seekTo(seekPreviewTime)
                            }
                            showGestureOverlay = false
                        },
                        onDragCancel = {
                            showGestureOverlay = false
                        }
                    )
                }
        )

        // 🔥 NEW: GESTURE OVERLAY UI (Shows icon and value while swiping)
        if (showGestureOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (gestureType) {
                        "volume" -> {
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        "brightness" -> {
                            Icon(Icons.Default.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        "seek" -> {
                            Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text(formatTime(seekPreviewTime), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

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
                color = Color(0xFF10B981),
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

                        // 🔥 NEW SLEEP TIMER BUTTON
                        Box {
                            IconButton(onClick = { showSleepMenu = !showSleepMenu; showSpeedMenu = false; showQualityMenu = false; showAspectMenu = false }) { 
                                Icon(Icons.Default.Timer, "Sleep", tint = if (sleepTimerMillis > 0) Color(0xFFE50914) else Color.White) 
                            }
                            DropdownMenu(expanded = showSleepMenu, onDismissRequest = { showSleepMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                DropdownMenuItem(text = { Text("Off", color = if(sleepTimerMillis == 0L) Color(0xFFE50914) else Color.White) }, onClick = { sleepTimerMillis = 0L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("15 mins", color = Color.White) }, onClick = { sleepTimerMillis = 15 * 60 * 1000L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("30 mins", color = Color.White) }, onClick = { sleepTimerMillis = 30 * 60 * 1000L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("60 mins", color = Color.White) }, onClick = { sleepTimerMillis = 60 * 60 * 1000L; showSleepMenu = false })
                            }
                        }

                        Box {
                            IconButton(onClick = { showSpeedMenu = !showSpeedMenu; showQualityMenu = false; showAspectMenu = false; showSleepMenu = false }) { Icon(Icons.Default.Speed, "Speed", tint = Color.White) }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                listOf(0.5f, 1f, 1.5f, 2f).forEach { speed -> DropdownMenuItem(text = { Text("${speed}x", color = if(playbackSpeed == speed) Color(0xFFE50914) else Color.White) }, onClick = { playbackSpeed = speed; showSpeedMenu = false }) }
                            }
                        }

                        Box {
                            IconButton(onClick = { showQualityMenu = !showQualityMenu; showSpeedMenu = false; showAspectMenu = false; showSleepMenu = false }) { Icon(Icons.Default.HighQuality, "Quality", tint = Color.White) }
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
                            IconButton(onClick = { showAspectMenu = !showAspectMenu; showSpeedMenu = false; showQualityMenu = false; showSleepMenu = false }) { Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White) }
                            DropdownMenu(expanded = showAspectMenu, onDismissRequest = { showAspectMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                listOf("Fit", "Fill", "Zoom").forEach { aspect ->
                                    DropdownMenuItem(text = { Text(aspect, color = Color.White) }, onClick = { 
                                        resizeMode = when(aspect) { "Fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT; "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL; else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM }
                                        showAspectMenu = false
                                    })
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                try {
                                    val aspectRatio = Rational(16, 9)
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(aspectRatio)
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                    showControls = false 
                                } catch (e: Exception) { }
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
        
        // 🔥 NEW: SLEEP TIMER ACTIVE INDICATOR (Shows remaining time at top center)
        if (sleepTimerMillis > 0 && !showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val mins = sleepTimerMillis / 60000
                val secs = (sleepTimerMillis % 60000) / 1000
                Text(
                    text = "Sleep in: %02d:%02d".format(mins, secs),
                    color = Color(0xFFFCDC04),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 🔥 NEW: RESUME DIALOG
        if (showResumeDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showResumeDialog = false 
                    exoPlayer.play()
                },
                title = { Text("Resume Playback?", color = Color.White) },
                text = { Text("Do you want to resume from where you left off (${formatTime(startPosition)})?", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            exoPlayer.seekTo(startPosition)
                            exoPlayer.play()
                            showResumeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("Resume") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            showResumeDialog = false
                        }
                    ) { Text("Start Over", color = Color.White) }
                },
                containerColor = Color(0xFF15171E)
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
}
