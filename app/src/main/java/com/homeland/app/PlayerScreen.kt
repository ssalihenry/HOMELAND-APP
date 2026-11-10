package com.homeland.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

// 🔥 FIXED: Global Cache Provider (Prevents the "Clear Cache" crash)
object CacheProvider {
    private var simpleCache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "homeland_media_cache")
            simpleCache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(500L * 1024 * 1024)) // 500MB Limit
        }
        return simpleCache!!
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    title: String,
    videoUrl: String,
    episodes: List<Episode> = emptyList(),
    startPosition: Long = 0L,
    subtitleUrl: String? = null,
    tvSessionCode: String? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    
    var showResumeDialog by remember { mutableStateOf(startPosition > 5000L) }

    var currentVideoUrl by remember { mutableStateOf(videoUrl) }
    var currentTitle by remember { mutableStateOf(title) }
    var isLocked by remember { mutableStateOf(false) }
    var showChannelSwitcher by remember { mutableStateOf(false) }
    var tvChannels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    
    var showSeekRipple by remember { mutableStateOf(false) }
    var seekRippleLeft by remember { mutableStateOf(true) }

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
    
    var showSleepMenu by remember { mutableStateOf(false) }
    var sleepTimerMillis by remember { mutableLongStateOf(0L) }

    var isAudioOnly by remember { mutableStateOf(false) }

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) } 

    var hasStartedPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    var dataUsedMb by remember { mutableDoubleStateOf(0.0) }

    val trackSelector = remember { DefaultTrackSelector(context) }
    var currentQuality by remember { mutableStateOf("Auto") }

    val focusRequester = remember { FocusRequester() }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureType by remember { mutableStateOf("") } 
    var gestureValue by remember { mutableFloatStateOf(0f) } 
    var seekPreviewTime by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        try {
            val simpleCache = CacheProvider.getCache(context)

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val isM3u8 = currentVideoUrl.contains("m3u8", ignoreCase = true)
            val minBuffer = if (isM3u8) 5000 else 10000
            val maxBuffer = if (isM3u8) 15000 else 20000 // 🔥 FIXED: Reduced from 50s to 20s to prevent crashes
            
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(minBuffer, maxBuffer, 2500, 5000)
                .build()

            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
                .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))

            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true 
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK) 
                .build().apply {
                    if (episodes.isNotEmpty()) {
                        val mediaItems = episodes.map { ep ->
                            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder().setTitle("${ep.season} ${ep.episode}").build()
                            MediaItem.Builder().setUri(ep.url).setMediaMetadata(mediaMetadata).build()
                        }
                        setMediaItems(mediaItems)
                    } else {
                        val mediaItemBuilder = MediaItem.Builder().setUri(currentVideoUrl)
                        
                        if (isM3u8) {
                            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                            
                            val liveConfig = MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(8000) // 8 Seconds delay
                                .setMaxPlaybackSpeed(1.1f) 
                                .setMinPlaybackSpeed(0.9f) 
                                .build()
                            mediaItemBuilder.setLiveConfiguration(liveConfig)
                        }
                        
                        setMediaItem(mediaItemBuilder.build())
                    }
                    
                    if (startPosition <= 5000L) {
                        seekTo(startPosition)
                        playWhenReady = true
                    } else {
                        seekTo(0)
                        playWhenReady = false
                    }
                    prepare()
                }
        } catch (e: Exception) {
            // Safe fallback for any initialization errors
            ExoPlayer.Builder(context).build()
        }
    }

    // 🔥 FEATURE: Auto-Resume on Network Switch
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                coroutineScope.launch {
                    delay(1000)
                    if (!exoPlayer.isPlaying && !hasError) {
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        onDispose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    // 🔥 FEATURE: Fetch Channels for Switcher
    LaunchedEffect(showChannelSwitcher) {
        if (showChannelSwitcher) {
            db.collection("homeland_tv").orderBy("name", Query.Direction.ASCENDING).get().addOnSuccessListener { snap ->
                tvChannels = snap.documents.map { doc ->
                    TvChannel(id = doc.id, name = doc.getString("name") ?: "", logo = doc.getString("logo") ?: "", streamUrl = doc.getString("streamUrl") ?: "", category = doc.getString("category") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false)
                }
            }
        }
    }

    DisposableEffect(tvSessionCode, exoPlayer) {
        if (tvSessionCode == null) {
            return@DisposableEffect onDispose { }
        }
        val startTs = System.currentTimeMillis() - 1000
        val processedIds = mutableSetOf<String>()

        val listener: ListenerRegistration = db.collection("tv_sessions").document(tvSessionCode)
            .collection("commands")
            .whereGreaterThan("timestamp", startTs)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.forEach { cmdDoc ->
                    if (!processedIds.add(cmdDoc.id)) return@forEach
                    val type = cmdDoc.getString("type") ?: return@forEach
                    val value = cmdDoc.getString("value") ?: ""
                    when (type) {
                        "play" -> { exoPlayer.play(); showControls = true }
                        "pause" -> { exoPlayer.pause(); showControls = true }
                        "toggle_play" -> { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play(); showControls = true }
                        "seek_forward" -> { exoPlayer.seekTo(exoPlayer.currentPosition + 10000); showControls = true }
                        "seek_backward" -> { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)); showControls = true }
                        "seek_to" -> { val pos = value.toLongOrNull() ?: 0L; exoPlayer.seekTo(pos); showControls = true }
                    }
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(isAudioOnly) {
        val params = trackSelector.buildUponParameters()
        if (isAudioOnly) params.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        else params.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        trackSelector.parameters = params.build()
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        hasStartedPlaying = true
                        hasError = false 
                        retryCount = 0 
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        if (episodes.isEmpty()) {
                            exoPlayer.seekTo(0)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                
                if (retryCount < 5 && episodes.isEmpty()) {
                    retryCount += 1 
                    isBuffering = true
                    hasError = false
                    
                    coroutineScope.launch {
                        delay(2000)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                } else {
                    hasError = true
                    isBuffering = false
                    errorMessage = "Connection lost after multiple attempts."
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val formatBitrate = exoPlayer.videoFormat?.bitrate
                val currentBitrate = if (formatBitrate != null && formatBitrate > 0) formatBitrate else 1200000
                val effectiveBitrate = if (isAudioOnly) 128000 else currentBitrate
                val bytesPerSecond = effectiveBitrate / 8
                val mbPerSecond = bytesPerSecond.toDouble() / (1024 * 1024)
                dataUsedMb += mbPerSecond
            }
            delay(1000)
        }
    }
    
    LaunchedEffect(sleepTimerMillis) {
        if (sleepTimerMillis > 0) {
            delay(1000)
            sleepTimerMillis -= 1000
            if (sleepTimerMillis <= 0) exoPlayer.pause()
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !showResumeDialog && !hasError && hasStartedPlaying && !isLocked) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                if (keyEvent.type == KeyEventType.KeyDown && !isLocked) {
                    when (keyEvent.key) {
                        Key.MediaPlayPause, Key.Spacebar, Key.Enter, Key.DirectionCenter -> {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                            true
                        }
                        Key.MediaFastForward, Key.DirectionRight -> { exoPlayer.seekTo(exoPlayer.currentPosition + 10000); showControls = true; true }
                        Key.MediaRewind, Key.DirectionLeft -> { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)); showControls = true; true }
                        Key.Back, Key.Escape -> { exoPlayer.pause(); onClose(); true }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyDown && isLocked) {
                    if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape) { isLocked = false; showControls = true; true } else false
                } else false
            }
            .pointerInput(isLocked, hasError, hasStartedPlaying) {
                detectTapGestures(
                    onTap = { if (!isLocked) showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isLocked || hasError || !hasStartedPlaying) return@detectTapGestures
                        val isLeftSide = offset.x < configuration.screenWidthDp.dp.toPx() / 2
                        if (isLeftSide) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                            seekRippleLeft = true
                        } else {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                            seekRippleLeft = false
                        }
                        showSeekRipple = true
                        coroutineScope.launch {
                            delay(500)
                            showSeekRipple = false
                        }
                        showControls = true
                    }
                )
            }
    ) {
        if (!isAudioOnly) {
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
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF1A0002), Color(0xFF0B0C10)), radius = 1200f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Headphones, contentDescription = "Audio", tint = Color(0xFFE50914), modifier = Modifier.size(70.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(currentTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Audio-Only Mode • Saving Data", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        if (showSeekRipple) {
            val rippleAlpha by animateFloatAsState(targetValue = if (showSeekRipple) 1f else 0f, animationSpec = tween(300), label = "ripple")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .align(if (seekRippleLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            if (seekRippleLeft) listOf(Color.White.copy(alpha = 0.2f * rippleAlpha), Color.Transparent)
                            else listOf(Color.Transparent, Color.White.copy(alpha = 0.2f * rippleAlpha))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (seekRippleLeft) Icons.Default.FastRewind else Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Text("10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAudioOnly, hasError, hasStartedPlaying, isLocked) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (showControls || isAudioOnly || hasError || !hasStartedPlaying || isLocked) return@detectDragGestures 
                            val isLeftSide = offset.x < configuration.screenWidthDp / 2
                            gestureType = if (isLeftSide) "brightness" else "volume"
                            showGestureOverlay = true
                            if (gestureType == "brightness") gestureValue = activity?.window?.attributes?.screenBrightness ?: 0.5f
                            else gestureValue = currentVolume.toFloat() / maxVolume.toFloat()
                            seekPreviewTime = exoPlayer.currentPosition
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                gestureType = "seek"
                                val seekSensitivity = 500f 
                                val seekDelta = (dragAmount.x * seekSensitivity).toLong()
                                seekPreviewTime = (seekPreviewTime + seekDelta).coerceIn(0L, duration)
                                gestureValue = if (duration > 0) seekPreviewTime.toFloat() / duration.toFloat() else 0f
                            } else {
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
                            if (gestureType == "seek") exoPlayer.seekTo(seekPreviewTime)
                            showGestureOverlay = false
                        },
                        onDragCancel = { showGestureOverlay = false }
                    )
                }
        )

        if (showGestureOverlay && !isLocked) {
            Box(modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.6f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).padding(horizontal = 32.dp, vertical = 24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (gestureType) {
                        "volume" -> { Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(40.dp)); Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                        "brightness" -> { Icon(Icons.Default.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(40.dp)); Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                        "seek" -> { Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(40.dp)); Text(formatTime(seekPreviewTime), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp, start = 20.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = String.format("%.2f MB", dataUsedMb), color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text("HOMELAND APP", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        if (!hasStartedPlaying && !hasError) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "load")
                    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(animation = tween(700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse")
                    Box(modifier = Modifier.size(100.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(24.dp)).background(Color(0xFFE50914)), contentAlignment = Alignment.Center) { Text("H", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black) }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(currentTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(28.dp))
                    CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Preparing your stream...", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (hasStartedPlaying && isBuffering && !hasError && !isLocked) {
            Box(modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp)).background(Color(0xFF15171E).copy(alpha = 0.85f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)).padding(horizontal = 40.dp, vertical = 28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 3.dp, modifier = Modifier.size(46.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if (retryCount > 0) "Reconnecting... (Attempt $retryCount/5)" else "Buffering...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().clickable { showControls = true }, contentAlignment = Alignment.Center) {
                AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { isLocked = false; showControls = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            LaunchedEffect(Unit) {
                delay(3000)
                showControls = false
            }
        }

        AnimatedVisibility(visible = showControls && !hasError && hasStartedPlaying && !isLocked, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))).align(Alignment.TopCenter))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { exoPlayer.pause(); onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    
                    if (episodes.isEmpty()) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { showChannelSwitcher = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlaylistPlay, "Channels", tint = Color.White)
                        }
                    }
                }

                Box(modifier = Modifier.size(80.dp).align(Alignment.Center).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }, contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp)) }

                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black))).align(Alignment.BottomCenter))

                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Slider(value = currentPosition.toFloat(), onValueChange = { exoPlayer.seekTo(it.toLong()) }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFFE50914), activeTrackColor = Color(0xFFE50914), inactiveTrackColor = Color.White.copy(alpha = 0.2f)), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                        Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { isLocked = true; showControls = false }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "Lock", tint = Color.White, modifier = Modifier.size(22.dp)) }

                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isAudioOnly) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)).border(1.dp, if (isAudioOnly) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f), CircleShape).clickable { isAudioOnly = !isAudioOnly }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Headphones, "Audio Only", tint = if (isAudioOnly) Color(0xFFE50914) else Color.White, modifier = Modifier.size(22.dp)) }
                        if (subtitleUrl != null && !isAudioOnly) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (subtitlesEnabled) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)).border(1.dp, if (subtitlesEnabled) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f), CircleShape).clickable { subtitlesEnabled = !subtitlesEnabled }, contentAlignment = Alignment.Center) { Icon(Icons.Default.ClosedCaption, "CC", tint = if (subtitlesEnabled) Color(0xFFE50914) else Color.White, modifier = Modifier.size(22.dp)) }
                        }
                        Box {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (sleepTimerMillis > 0) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)).border(1.dp, if (sleepTimerMillis > 0) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f), CircleShape).clickable { showSleepMenu = !showSleepMenu; showSpeedMenu = false; showQualityMenu = false; showAspectMenu = false }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Timer, "Sleep", tint = if (sleepTimerMillis > 0) Color(0xFFE50914) else Color.White, modifier = Modifier.size(22.dp)) }
                            DropdownMenu(expanded = showSleepMenu, onDismissRequest = { showSleepMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                DropdownMenuItem(text = { Text("Off", color = if(sleepTimerMillis == 0L) Color(0xFFE50914) else Color.White) }, onClick = { sleepTimerMillis = 0L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("15 mins", color = Color.White) }, onClick = { sleepTimerMillis = 15 * 60 * 1000L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("30 mins", color = Color.White) }, onClick = { sleepTimerMillis = 30 * 60 * 1000L; showSleepMenu = false })
                                DropdownMenuItem(text = { Text("60 mins", color = Color.White) }, onClick = { sleepTimerMillis = 60 * 60 * 1000L; showSleepMenu = false })
                            }
                        }
                        Box {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { showSpeedMenu = !showSpeedMenu; showQualityMenu = false; showAspectMenu = false; showSleepMenu = false }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Speed, "Speed", tint = Color.White, modifier = Modifier.size(22.dp)) }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) { 
                                listOf(0.5f, 1f, 1.5f, 2f).forEach { speed -> 
                                    DropdownMenuItem(
                                        text = { Text("${speed}x", color = if(playbackSpeed == speed) Color(0xFFE50914) else Color.White) }, 
                                        onClick = { 
                                            playbackSpeed = speed
                                            exoPlayer.playbackParameters = PlaybackParameters(speed, 1.0f)
                                            showSpeedMenu = false 
                                        }
                                    ) 
                                } 
                            }
                        }
                        if (!isAudioOnly) {
                            Box {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { showQualityMenu = !showQualityMenu; showSpeedMenu = false; showAspectMenu = false; showSleepMenu = false }, contentAlignment = Alignment.Center) { Icon(Icons.Default.HighQuality, "Quality", tint = Color.White, modifier = Modifier.size(22.dp)) }
                                DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                    listOf("Auto", "1080p", "720p", "480p").forEach { quality ->
                                        DropdownMenuItem(text = { Text(quality, color = if(currentQuality == quality) Color(0xFFE50914) else Color.White) }, onClick = { 
                                            currentQuality = quality; showQualityMenu = false
                                            val params = trackSelector.buildUponParameters()
                                            if (quality == "Auto") params.clearVideoSizeConstraints() 
                                            else if (quality == "1080p") params.setMaxVideoSize(1920, 1080).setMaxVideoBitrate(5000000)
                                            else if (quality == "720p") params.setMaxVideoSize(1280, 720).setMaxVideoBitrate(2500000)
                                            else if (quality == "480p") params.setMaxVideoSize(854, 480).setMaxVideoBitrate(1000000)
                                            trackSelector.parameters = params.build()
                                        })
                                    }
                                }
                            }
                        }
                        if (!isAudioOnly) {
                            Box {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { showAspectMenu = !showAspectMenu; showSpeedMenu = false; showQualityMenu = false; showSleepMenu = false }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White, modifier = Modifier.size(22.dp)) }
                                DropdownMenu(expanded = showAspectMenu, onDismissRequest = { showAspectMenu = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                    listOf("Fit", "Fill", "Zoom").forEach { aspect ->
                                        DropdownMenuItem(text = { Text(aspect, color = Color.White) }, onClick = { resizeMode = when(aspect) { "Fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT; "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL; else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM }; showAspectMenu = false })
                                    }
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isAudioOnly) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { try { val aspectRatio = Rational(16, 9); val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build(); activity?.enterPictureInPictureMode(params); showControls = false } catch (e: Exception) { } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White, modifier = Modifier.size(22.dp)) }
                        }
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).clickable { if (isLandscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; isLandscape = !isLandscape }, contentAlignment = Alignment.Center) { Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Rotate", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    }
                }
            }
        }
        
        if (sleepTimerMillis > 0 && !showControls && !hasError && hasStartedPlaying && !isLocked) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                val mins = sleepTimerMillis / 60000
                val secs = (sleepTimerMillis % 60000) / 1000
                Text(text = "Sleep in: %02d:%02d".format(mins, secs), color = Color(0xFFFCDC04), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (hasError && !isLocked) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF15171E).copy(alpha = 0.85f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)).padding(32.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "signal")
                    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.35f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "pulse")
                    Box(modifier = Modifier.size(130.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.WifiOff, contentDescription = "Signal Lost", tint = Color(0xFFE50914).copy(alpha = pulseAlpha), modifier = Modifier.size(70.dp)) }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Signal Lost", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(onClick = { 
                        hasError = false
                        hasStartedPlaying = false
                        isBuffering = true
                        retryCount = 0
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(50), modifier = Modifier.height(56.dp).fillMaxWidth(0.85f).shadow(12.dp, RoundedCornerShape(50), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914))) { 
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White); Spacer(modifier = Modifier.width(10.dp)); Text("Retry", fontSize = 17.sp, fontWeight = FontWeight.Bold) 
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { exoPlayer.pause(); onClose() }) { Text("Close Player", color = Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }

        if (showResumeDialog) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false; exoPlayer.play() },
                title = { Text("Resume Playback?", color = Color.White) },
                text = { Text("Do you want to resume from where you left off (${formatTime(startPosition)})?", color = Color.Gray) },
                confirmButton = { Button(onClick = { exoPlayer.seekTo(startPosition); exoPlayer.play(); showResumeDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Resume") } },
                dismissButton = { TextButton(onClick = { exoPlayer.seekTo(0); exoPlayer.play(); showResumeDialog = false }) { Text("Start Over", color = Color.White) } },
                containerColor = Color(0xFF15171E)
            )
        }
    }

    if (showChannelSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showChannelSwitcher = false },
            containerColor = Color(0xFF15171E),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Text("Switch Channel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn {
                    items(tvChannels) { channel ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).clickable {
                                currentVideoUrl = channel.streamUrl
                                currentTitle = channel.name
                                showChannelSwitcher = false
                                hasStartedPlaying = false
                                isBuffering = true
                                
                                val mediaItemBuilder = MediaItem.Builder().setUri(channel.streamUrl)
                                if (channel.streamUrl.contains("m3u8", ignoreCase = true)) {
                                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                                    mediaItemBuilder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(8000).setMaxPlaybackSpeed(1.1f).setMinPlaybackSpeed(0.9f).build())
                                }
                                exoPlayer.setMediaItem(mediaItemBuilder.build())
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Tv, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(channel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
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
