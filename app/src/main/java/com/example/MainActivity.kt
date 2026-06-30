package com.example

import android.os.Bundle
import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.view.HapticFeedbackConstants
import android.media.AudioAttributes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameState
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FloatingEffect
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.GameViewModelFactory
import com.example.viewmodel.UpgradeItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        val context = LocalContext.current
        val application = context.applicationContext as CandyApplication
        val repository = application.repository
        
        val factory = remember { GameViewModelFactory(repository) }
        val viewModel: GameViewModel = viewModel(factory = factory)
        
        CandyClickerApp(viewModel)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandyClickerApp(viewModel: GameViewModel) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val floatingEffects by viewModel.floatingEffects.collectAsStateWithLifecycle()
    val offlineEarnings by viewModel.offlineEarnings.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val lollipopBitmap = remember(context) {
        loadTransparentLollipop(context)
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var scaleTarget by remember { mutableStateOf(1.0f) }
    var activeTab by rememberSaveable { mutableStateOf("game") }
    var shopSubcategory by rememberSaveable { mutableStateOf("buildings") }
    var buyQuantity by rememberSaveable { mutableStateOf(1) }
    var isSellMode by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showAchievements by rememberSaveable { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<CandyAchievementItem?>(null) }

    // Physical tap-spring animation for squash on click
    val scale by animateFloatAsState(
        targetValue = if (state.lollipopMovementOn) scaleTarget else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "click_scale"
    )

    // Pulse animation for central halo indicator
    val infTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseSize by infTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_size"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().diagonalStripesBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // --- BOTTOM NAVIGATION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCCFEDADF))
                    .border(1.dp, Color(0xFFF4DDDE))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game Tab
                val isGameSelected = activeTab == "game"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "game" }
                        .alpha(if (isGameSelected) 1.0f else 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGameSelected) Color(0xFFFFDADB) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🍬", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Game",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2B1516),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Candy Store Tab
                val isBuildingsSelected = activeTab == "buildings"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "buildings" }
                        .alpha(if (isBuildingsSelected) 1.0f else 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBuildingsSelected) Color(0xFFFFDADB) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏪", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Candy Store",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2B1516),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secret Sugars Tab
                val isSecretSelected = activeTab == "secret_sugars"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "secret_sugars" }
                        .alpha(if (isSecretSelected) 1.0f else 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSecretSelected) Color(0xFFFFDADB) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🤫", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Secret Sugars",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2B1516),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings Tab
                val isSettingsSelected = activeTab == "settings"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "settings" }
                        .alpha(if (isSettingsSelected) 1.0f else 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSettingsSelected) Color(0xFFFFDADB) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚙️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2B1516),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


        // --- GRID MAIN AREA ---
        Column(
            modifier = Modifier
                .weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(if (activeTab == "game") 12.dp else 0.dp)
        ) {
            // Card 1: Centered Total Candies and CPS Count (Background Removed)
            if (activeTab == "game") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TOTAL CANDIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF917576)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatValue(state.candies),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2B1516)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${viewModel.formatValue(viewModel.getCps(state))} /s",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF547D)
                    )
                }
            }

            if (activeTab == "game") {
                // Interactive Tap Target (Occupying full area of the game tab)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    val workingSize = minOf(containerWidth, containerHeight)
                    val lollipopSize = workingSize * 1.225f // Scaled lollipop (scaled up by 75%)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(workingSize).offset(x = 20.dp)
                        ) {
                            Image(
                                bitmap = lollipopBitmap,
                                contentDescription = "Giant Candy click target",
                                modifier = Modifier
                                    .size(lollipopSize)
                                    .scale(scale)
                                    .rotate(-45f)
                                    .testTag("candy_click_target")
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (state.lollipopMovementOn) {
                                            coroutineScope.launch {
                                                scaleTarget = 0.82f
                                                delay(70)
                                                scaleTarget = 1.0f
                                            }
                                        }
                                        if (state.soundOn) {
                                            CrunchSoundPlayer.play()
                                        }
                                        if (state.vibrationOn) {
                                            try {
                                                // hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (e: Exception) {}
                                            try {
                                                // view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                // view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            } catch (e: Exception) {}
                                            try {
                                                val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                     context.applicationContext.createAttributionContext("vibrate")
                                                 } else {
                                                     context.applicationContext
                                                 }
                                                 val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val vibratorManager = attributionContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                                    vibratorManager?.defaultVibrator
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                                }
                                                if (vibrator != null && vibrator.hasVibrator()) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        val attrs = AudioAttributes.Builder()
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                                            .build()
                                                        vibrator.vibrate(VibrationEffect.createOneShot(40, 255), attrs)
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator.vibrate(40)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // ignore fallback
                                            }
                                        }
                                        
                                        // Random polar coordinates to distribute the indicators in a ring around the scaled lollipop candy center
                                        val angle = (0..359).random() * (Math.PI / 180.0)
                                        val minRadius = workingSize.value * 0.31f * 1.75f
                                        val maxRadius = workingSize.value * 0.44f * 1.75f
                                        val radius = (minRadius.toInt()..maxRadius.toInt()).random().toFloat()
                                        val randomX = (kotlin.math.cos(angle) * radius).toFloat() - 35f
                                        val randomY = (kotlin.math.sin(angle) * radius).toFloat() - 61.25f
                                        
                                        viewModel.onCandyClicked(randomX, randomY)
                                    },
                                contentScale = ContentScale.Fit
                            )

                            // Render rotating oven mitt outer ring
                            val numVisibleMitts = minOf(state.mittLevel, 16)
                            if (numVisibleMitts > 0) {
                                val infiniteTransition = rememberInfiniteTransition(label = "mitt_rotation")
                                val angleOffset by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 2f * Math.PI.toFloat(),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 40000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "angleOffset"
                                )

                                for (i in 0 until numVisibleMitts) {
                                    val baseAngle = (i.toFloat() / numVisibleMitts.toFloat()) * 2f * Math.PI.toFloat()
                                    val totalAngle = baseAngle + angleOffset

                                    // Place mitts perfectly centered and rotating around the scaled up lollipop candy center
                                    val radiusDp = (((workingSize * 0.41f) - 38.dp) * 1.75f) - 40.dp
                                    val xOffset = (kotlin.math.cos(totalAngle.toDouble()) * radiusDp.value).toFloat().dp - 30.dp
                                    val yOffset = (kotlin.math.sin(totalAngle.toDouble()) * radiusDp.value).toFloat().dp - 36.25.dp

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = xOffset, y = yOffset)
                                            .size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🧤",
                                            fontSize = 32.sp,
                                            modifier = Modifier.rotate((totalAngle * 180f / Math.PI.toFloat()) + 90f)
                                        )
                                    }
                                }
                            }

                            // Render floating indicators overlays right around the lollipop
                            floatingEffects.forEach { effect ->
                                FloatingScoreText(effect = effect)
                            }
                        }
                    }
                }
            } else if (activeTab == "secret_sugars") {
                // --- SECRET SUGARS TAB (Full Screen) ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .background(Color(0xFF230F10)) // Solid deep burgundy background covering full screen
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Secret Sugars Status Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1D0E0F)) // Darker warm chocolate header accent
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOTAL CANDIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color(0xFFF9BCBC)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.formatValue(state.candies),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${viewModel.formatValue(viewModel.getCps(state))} /s",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF547D)
                            )
                        }

                        // Scrollable/Control Area with inner padding for the cards
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Title
                            Text(
                                text = "Secret Sugars 🤫",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "Unlock high-tier click scaling and ascend to new cookie dimensions here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF9BCBC),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Column 1: Click Boost Display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF1D0E0F))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CLICK BOOST",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF9BCBC)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "+${state.prestigePoints}%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Prestige click power multiplier bonus",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF917576),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(text = "⭐", fontSize = 28.sp)
                            }

                            // Column 2: Prestige Progress
                            val prestigeGoal = 1_000_000_000_000.0
                            val totalPrestigeCalculated = (state.totalCandiesEarned / prestigeGoal).toLong()
                            val pointsToClaim = (totalPrestigeCalculated - state.prestigePoints).coerceAtLeast(0L)
                            val canPrestige = pointsToClaim > 0
                            
                            val currentMilestone = state.prestigePoints * prestigeGoal
                            val candiesTowardNext = (state.totalCandiesEarned - currentMilestone).coerceAtLeast(0.0)
                            val progressRatio = (candiesTowardNext / prestigeGoal).coerceIn(0.0, 1.0).toFloat()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (canPrestige) Color(0xFFFF547D) else Color(0xFF1D0E0F))
                                    .border(1.dp, Color(0xFFF9BCBC).copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                    .clickable(enabled = canPrestige) {
                                        viewModel.prestige()
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (canPrestige) "READY TO PRESTIGE! ⭐" else "PRESTIGE PROGRESS ⏳",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canPrestige) Color.White else Color(0xFFF9BCBC)
                                        )
                                        if (state.prestigePoints > 0) {
                                            Text(
                                                text = "⭐ x${state.prestigePoints}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (canPrestige) {
                                        Text(
                                            text = "TAP HERE TO CLAIM +$pointsToClaim ${if (pointsToClaim == 1L) "PT" else "PTS"}!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Your sweets will reset, but you will unlock permanent passive multiplier bonuses per star!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            progress = { progressRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFFFF547D),
                                            trackColor = Color(0xFF2B1516)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${viewModel.formatValue(candiesTowardNext)} / 1.00 T",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF9BCBC)
                                            )
                                            Text(
                                                text = "${(progressRatio * 100).toInt()}% Done",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF547D)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Achievements Button Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                    .clickable {
                                        showAchievements = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1D0E0F)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2B1516)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🏆", fontSize = 24.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Candy Store Achievements",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Check display shelves for unlocked upgrade jars!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF917576)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open Achievements",
                                        tint = Color(0xFFFFD54F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // --- CANDY SHELF ACHIEVEMENTS OVERLAY (Animated Slide-up) ---
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showAchievements,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF230F10)) // matching deep burgundy background
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Overlay Header with backward navigation
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1D0E0F))
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { showAchievements = false }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Candy Store Shelves 🏪",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Milestones achieved by upgrading store properties",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFF9BCBC),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Scrollable shelf rows representation
                                val achievements = remember(state) { getAchievementsList(state) }
                                
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Let's group achievements by category and make a shelf for each category!
                                    val categories = listOf(
                                        "Sugar Towers" to "🗼",
                                        "Candy Drones" to "🐝",
                                        "Gingerbread Mills" to "🍪",
                                        "Cotton Clouds" to "☁️",
                                        "Chocolate Volcanoes" to "🌋",
                                        "Sugar Earths" to "🌍",
                                        "Lollipop Galaxies" to "🌌",
                                        "Catalyst Tools" to "🧪"
                                    )

                                    items(categories) { (cat, icon) ->
                                        val catAchievements = achievements.filter { it.category == cat }
                                        CandyStoreShelf(
                                            shelfName = cat,
                                            shelfIcon = icon,
                                            achievements = catAchievements,
                                            onAchievementSelected = { selectedAchievement = it }
                                        )
                                    }
                                    
                                    item {
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                            
                            // Bottom popup detail card for the clicked achievement
                            selectedAchievement?.let { achievement ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { selectedAchievement = null },
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = false) {} // block internal click
                                            .padding(16.dp)
                                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF1D0E0F)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(20.dp)
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = achievement.tier.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = getTierColor(achievement.tier)
                                                )
                                                
                                                val isCompleted = achievement.currentLevel >= achievement.targetLevel
                                                Text(
                                                    text = if (isCompleted) "🏆 COMPLETED" else "🔒 LOCKED",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFFF547D)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Big Icon Jar
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color(0xFF2B1516))
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = achievement.emoji, fontSize = 42.sp)
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = achievement.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text(
                                                text = achievement.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFFFDADB),
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // Progress Indicator
                                            val progressRatio = (achievement.currentLevel.toFloat() / achievement.targetLevel.toFloat()).coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { progressRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = Color(0xFFFF547D),
                                                trackColor = Color(0xFF2B1516)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text(
                                                text = "Progress: ${achievement.currentLevel} / ${achievement.targetLevel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF9BCBC)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Button(
                                                onClick = { selectedAchievement = null },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFF547D)
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Close details", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (activeTab == "buildings") {
                // --- CANDY STORE TAB (Full Screen) ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .background(Color(0xFF230F10)) // Solid deep burgundy background covering full screen
                ) {
                    // Scrollable/Control Area with inner padding for the cards
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                            // Shop Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sugar Shop",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (shopSubcategory == "buildings") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // BUY/SELL Toggle Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSellMode) Color(0xFFC62828) else Color(0xFF2E7D32))
                                            .clickable { isSellMode = !isSellMode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSellMode) "SELL" else "BUY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    // Single Quantity Selector Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFF547D))
                                            .clickable {
                                                val quantities = listOf(1, 10, 25, 50, 100, -1)
                                                val currentIndex = quantities.indexOf(buyQuantity)
                                                val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % quantities.size
                                                buyQuantity = quantities[nextIndex]
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (buyQuantity == -1) "MAX" else "x$buyQuantity",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                val categoryFilteredItems = viewModel.getUpgradeItems(state).filter {
                                    val isSubMatching = if (shopSubcategory == "upgrades") !it.isBuilding else it.isBuilding
                                    isSubMatching
                                }
                                val availableCount = categoryFilteredItems.count { item ->
                                    state.candies >= item.cost
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF547D))
                                        .clickable(enabled = availableCount > 0) {
                                            viewModel.buyMaxAffordableUpgrades()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "BUY MAX ($availableCount BUYABLE)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (availableCount > 0) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subcategory tabs: Buildings and Upgrades
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1D0E0F)) // Darker contrast fill for tabs background
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isSubBuildings = shopSubcategory == "buildings"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSubBuildings) Color(0xFFFF547D) else Color.Transparent)
                                    .clickable { shopSubcategory = "buildings" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Buildings",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubBuildings) Color.White else Color(0xFFF9BCBC)
                                )
                            }

                            val isSubUpgrades = shopSubcategory == "upgrades"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSubUpgrades) Color(0xFFFF547D) else Color.Transparent)
                                    .clickable { shopSubcategory = "upgrades" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Upgrades",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubUpgrades) Color.White else Color(0xFFF9BCBC)
                                )
                            }

                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Embedded upgrades list in the Bento box
                        val upgrades = viewModel.getUpgradeItems(state).filter {
                            val isSubMatching = if (shopSubcategory == "upgrades") !it.isBuilding else it.isBuilding
                            isSubMatching
                        }.map { item ->
                            if (item.isBuilding) {
                                if (isSellMode) {
                                    val actualCount = if (buyQuantity == -1) item.level else minOf(buyQuantity, item.level)
                                    val refund = if (actualCount > 0) {
                                        viewModel.calculateMultiCost(item.baseCost, item.level - actualCount, actualCount, item.costMultiplier) * 0.5
                                    } else {
                                        0.0
                                    }
                                    item.copy(cost = refund)
                                } else {
                                    if (buyQuantity == -1) {
                                        val maxAffordable = viewModel.getMaxAffordableCount(item.baseCost, item.level, state.candies, item.costMultiplier)
                                        val countToBuy = maxOf(1, maxAffordable)
                                        val multiCost = viewModel.calculateMultiCost(item.baseCost, item.level, countToBuy, item.costMultiplier)
                                        item.copy(cost = multiCost)
                                    } else if (buyQuantity > 1) {
                                        val multiCost = viewModel.calculateMultiCost(item.baseCost, item.level, buyQuantity, item.costMultiplier)
                                        item.copy(cost = multiCost)
                                    } else {
                                        item
                                    }
                                }
                            } else {
                                item
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.0f)
                                .testTag("upgrades_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(upgrades, key = { it.id }) { item ->
                                BentoStoreCard(
                                    item = item,
                                    currentCandies = state.candies,
                                    onBuy = {
                                        val count = if (buyQuantity == -1) {
                                            viewModel.getMaxAffordableCount(item.baseCost, item.level, state.candies, item.costMultiplier)
                                        } else {
                                            buyQuantity
                                        }
                                        if (count > 0) {
                                            viewModel.buyUpgrade(item.id, if (item.isBuilding) count else 1)
                                        }
                                    },
                                    viewModel = viewModel,
                                    buyQuantity = if (item.isBuilding) buyQuantity else 1,
                                    isSellMode = isSellMode && item.isBuilding,
                                    onSell = {
                                        val count = if (buyQuantity == -1) item.level else buyQuantity
                                        if (count > 0) {
                                            viewModel.sellUpgrade(item.id, if (item.isBuilding) count else 1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (activeTab == "settings") {
                // --- SETTINGS TAB (Full Screen) ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .background(Color(0xFF230F10)) // Solid deep burgundy background covering full screen
                ) {
                    // Scrollable/Control Area with inner padding for the settings rows
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                            // Title
                            Text(
                                text = "Sugar Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Sound Option Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1D0E0F))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🔊", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "Sound Effects",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (state.soundOn) "Crunchy clicks enabled" else "Muted",
                                            color = Color(0xFFF9BCBC),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = state.soundOn,
                                    onCheckedChange = { viewModel.toggleSound() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF547D),
                                        uncheckedThumbColor = Color(0xFF564344),
                                        uncheckedTrackColor = Color(0xFFF4DDDE)
                                    )
                                )
                            }

                            // Vibration Option Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1D0E0F))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "📳", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "Haptic Feedback",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (state.vibrationOn) "Sweet vibrations active" else "Silent taps",
                                            color = Color(0xFFF9BCBC),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = state.vibrationOn,
                                    onCheckedChange = { viewModel.toggleVibration() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF547D),
                                        uncheckedThumbColor = Color(0xFF564344),
                                        uncheckedTrackColor = Color(0xFFF4DDDE)
                                    )
                                )
                            }

                            // Lollipop Movement Option Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1D0E0F))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🍭", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "Lollipop Movement",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (state.lollipopMovementOn) "Bounce animation active" else "Static mode",
                                            color = Color(0xFFF9BCBC),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = state.lollipopMovementOn,
                                    onCheckedChange = { viewModel.toggleLollipopMovement() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF547D),
                                        uncheckedThumbColor = Color(0xFF564344),
                                        uncheckedTrackColor = Color(0xFFF4DDDE)
                                    ),
                                    modifier = Modifier.testTag("lollipop_movement_toggle")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Premium Cover Banner Display
                            Text(
                                text = "Official Game Cover Art",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF9BCBC)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color(0xFFFF547D), RoundedCornerShape(16.dp))
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.candy_clicker_cover),
                                    contentDescription = "Candy Clicker Game Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Reset progress Button
                            Button(
                                onClick = { showResetDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF547D)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Reset Game Progress",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Offline progress dialog
    offlineEarnings?.let { earnings ->
        OfflineEarningsDialog(
            earnedAmount = earnings,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissOfflineEarnings() }
        )
    }

    // Reset progress confirm dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Progress?") },
            text = { Text(text = "Are you sure you want to sweep away all your candies, upgrades, and factory machines? This cannot be undone!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetGame()
                        showResetDialog = false
                    }
                ) {
                    Text(text = "RESET", color = Color(0xFFFF547D), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "CANCEL", color = Color.White)
                }
            },
            containerColor = Color(0xFF2B1516),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFF4DDDE)
        )
    }
}

// Spark Floating Click Animation Content
@Composable
fun FloatingScoreText(effect: FloatingEffect) {
    val alphaAnim = remember { Animatable(1.0f) }
    val yAnim = remember { Animatable(0f) }

    LaunchedEffect(effect.id) {
        launch {
            yAnim.animateTo(
                targetValue = -70f,
                animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)
            )
        }
    }

    Text(
        text = effect.text,
        style = MaterialTheme.typography.headlineLarge.copy(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.6f),
                offset = Offset(3f, 3f),
                blurRadius = 6f
            )
        ),
        fontWeight = FontWeight.Black,
        color = Color(0xFF42000A), // Extremely deep, dark rich chocolate-burgundy for absolute contrast on bright backgrounds
        modifier = Modifier
            .offset { IntOffset(effect.offsetX.toInt(), effect.offsetY.toInt() + yAnim.value.toInt()) }
            .alpha(alphaAnim.value)
    )
}

// Upgrade item representing store element inside the deep burgundy shop layout
@Composable
fun BentoStoreCard(
    item: UpgradeItem,
    currentCandies: Double,
    onBuy: () -> Unit,
    viewModel: GameViewModel,
    buyQuantity: Int = 1,
    isSellMode: Boolean = false,
    onSell: () -> Unit = {}
) {
    val maxAffordable = if (item.isBuilding) {
        viewModel.getMaxAffordableCount(item.baseCost, item.level, currentCandies, item.costMultiplier)
    } else {
        0
    }
    val actualSellCount = if (isSellMode && item.isBuilding) {
        if (buyQuantity == -1) item.level else minOf(buyQuantity, item.level)
    } else {
        0
    }
    val canAct = if (isSellMode) {
        actualSellCount > 0
    } else {
        if (item.isBuilding && buyQuantity == -1) {
            maxAffordable > 0
        } else {
            currentCandies >= item.cost
        }
    }

    val icon = when {
        item.id == "mitts" -> "🧤"
        item.id.startsWith("mitt_up_") -> "🧤✨"
        item.id == "click" -> "🗼"
        item.id.startsWith("click_") -> "🗼✨"
        item.id == "drone" -> "🤖"
        item.id.startsWith("drone_") -> "🤖✨"
        item.id == "gingerbread" -> "🏡"
        item.id.startsWith("gingerbread_") -> "🏡✨"
        item.id == "cotton" -> "☁️"
        item.id.startsWith("cotton_") -> "☁️✨"
        item.id == "volcano" -> "🌋"
        item.id.startsWith("volcano_") -> "🌋✨"
        item.id == "earth" -> "🌍"
        item.id.startsWith("earth_") -> "🌍✨"
        item.id == "galaxy" -> "🌌"
        item.id.startsWith("galaxy_") -> "🌌✨"
        item.id == "spatula_wooden" -> "🪵🍳"
        item.id == "spatula_plastic" -> "🥤🍳"
        item.id == "spatula_silicon" -> "🔵🍳"
        item.id == "spatula_steel" -> "🔩🍳"
        item.id == "spatula_aluminum" -> "🪙🍳"
        item.id == "spatula_titanium" -> "🛡️🍳"
        item.id == "spatula_iridium" -> "☄️🍳"
        item.id == "spatula_gold" -> "🏆🍳"
        item.id.startsWith("spatula_") -> "🍳"
        else -> "🌌"
    }

    val actionColor = if (isSellMode) Color(0xFFE53935) else Color(0xFFFF547D)
    var showInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF)) // White 10%
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            .clickable(enabled = canAct, onClick = if (isSellMode) onSell else onBuy)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left icon Emoji badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x15FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Info middle
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = if (item.isBuilding) {
                                MaterialTheme.typography.bodyMedium.copy(fontSize = 21.sp)
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (!item.isBuilding) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xCCFFFFFF),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSellMode) {
                                "Refund: +${viewModel.formatValue(item.cost)}"
                            } else {
                                "Cost: ${viewModel.formatValue(item.cost)}"
                            },
                            style = if (item.isBuilding) {
                                MaterialTheme.typography.labelSmall.copy(fontSize = 8.25.sp)
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isSellMode) {
                                if (canAct) Color(0xFF81C784) else Color(0x80FFFFFF)
                            } else {
                                if (canAct) Color(0xFFFFDADB) else Color(0x80FFFFFF)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Info tab/button next to buy button
                if (item.isBuilding) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (showInfo) Color(0x50FFFFFF)
                                else Color(0x15FFFFFF)
                            )
                            .clickable { showInfo = !showInfo }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "INFO ℹ️",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Right Buy/Sell button/indication
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (canAct) actionColor
                            else Color(0x15FFFFFF)
                        )
                        .clickable(enabled = canAct, onClick = if (isSellMode) onSell else onBuy)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSellMode) {
                            if (buyQuantity == -1) {
                                if (actualSellCount > 0) "SELL ALL (x$actualSellCount)" else "SELL"
                            } else {
                                if (actualSellCount > 1) "SELL x$actualSellCount" else "SELL"
                            }
                        } else {
                            if (item.isBuilding) {
                                if (buyQuantity == -1) {
                                    if (maxAffordable > 0) "BUY x$maxAffordable" else "BUY"
                                } else if (buyQuantity > 1) {
                                    "BUY x$buyQuantity"
                                } else {
                                    "BUY"
                                }
                            } else {
                                "BUY"
                            }
                        },
                        fontWeight = FontWeight.Black,
                        color = if (canAct) Color.White else Color(0x40FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }

            if (showInfo && item.isBuilding) {
                val state by viewModel.gameState.collectAsStateWithLifecycle()
                val towerId = when (item.id) {
                    "mitts" -> "mitt"
                    "click" -> "click"
                    "drone" -> "drone"
                    "gingerbread" -> "gingerbread"
                    "cotton" -> "cotton"
                    "volcano" -> "volcano"
                    "earth" -> "earth"
                    "galaxy" -> "galaxy"
                    else -> ""
                }
                if (towerId.isNotEmpty()) {
                    val activeCps = viewModel.getTowerCps(state, towerId)
                    val totalProduced = when (towerId) {
                        "mitt" -> state.mittsTotalEarned
                        "click" -> state.clickTotalEarned
                        "drone" -> state.droneTotalEarned
                        "gingerbread" -> state.gingerbreadTotalEarned
                        "cotton" -> state.cottonTotalEarned
                        "volcano" -> state.volcanoTotalEarned
                        "earth" -> state.earthTotalEarned
                        "galaxy" -> state.galaxyTotalEarned
                        else -> 0.0
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20000000))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xEEFFFFFF),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x15FFFFFF))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "ACTIVE CPS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9EAF)
                                )
                                Text(
                                    text = "producing ${viewModel.formatValue(activeCps)} per second",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL CANDY PRODUCED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9EAF)
                                )
                                Text(
                                    text = viewModel.formatValue(totalProduced),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Welcome back dialog celebrating offline production earnings
@Composable
fun OfflineEarningsDialog(
    earnedAmount: Double,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🍭 Welcome Back!",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your automated candy factories kept boiling sugar while you were away!",
                    color = Color(0xFFF4DDDE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "+${viewModel.formatValue(earnedAmount)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF547D),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "candies cooked in your absence",
                    fontSize = 12.sp,
                    color = Color(0xFFFFDADB),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF547D))
            ) {
                Text(text = "Delicious!", color = Color.White)
            }
        },
        containerColor = Color(0xFF2B1516),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFF4DDDE)
    )
}

fun loadTransparentLollipop(context: android.content.Context): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        inMutable = true
    }
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_lollipop, options)
        ?: return androidx.core.graphics.createBitmap(1, 1).asImageBitmap()
    
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    
    val scaleY961 = (height * 961 / 1024)
    val scaleY600 = (height * 600 / 1024)
    
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = y * width + x
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            var isTransparent = false
            if (y >= scaleY961) {
                // Completely clear everything below the stick tip
                isTransparent = true
            } else if (y > scaleY600) {
                // Below the candy swirl (where only the stick and background reside),
                // we can safely clean up all off-white/light gray shadow background
                if (r > 180 && g > 180 && b > 180) {
                    isTransparent = true
                }
            } else {
                // Inside the candy swirl region, clear only the bright white background securely
                if (r > 240 && g > 240 && b > 240) {
                    isTransparent = true
                }
            }
            
            if (isTransparent) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            }
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}

fun Modifier.diagonalStripesBackground(): Modifier = this.drawBehind {
    val stripeWidth = 24.dp.toPx() // Elegant stripes thickness
    val stripeColorLight = Color(0xFFFEDADF) // Soft pastel pink
    val stripeColorDark = Color(0xFFFFAEBC)  // Slightly darker pink for contrast
    
    // Fill background with light pink first
    drawRect(color = stripeColorLight)
    
    // Draw diagonal lines at 45 degree angle
    val width = size.width
    val height = size.height
    val step = stripeWidth * 2f
    
    var startX = -height
    while (startX < width) {
        drawLine(
            color = stripeColorDark,
            start = Offset(startX, 0f),
            end = Offset(startX + height, height),
            strokeWidth = stripeWidth
        )
        startX += step
    }
}

object CrunchSoundPlayer {
    fun play() {
        kotlin.concurrent.thread {
            try {
                val sampleRate = 22050
                val durationMs = 120
                val numSamples = sampleRate * durationMs / 1000
                val buffer = ShortArray(numSamples)
                
                val random = java.util.Random()
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val envelope = (1.0f - progress) * (1.0f - progress)
                    
                    val noiseOffset = if (progress < 0.35f) {
                        (random.nextFloat() * 2.0f - 1.0f) * 0.7f
                    } else {
                        (random.nextFloat() * 2.0f - 1.0f) * 0.15f
                    }
                    
                    val crackle = if (progress < 0.5f && random.nextFloat() > 0.94f) {
                        (random.nextFloat() * 2.0f - 1.0f) * 0.8f
                    } else {
                        0.0f
                    }
                    
                    val t = i.toFloat() / sampleRate
                    val freq = 160.0 - (progress * 110.0)
                    val thud = kotlin.math.sin(2.0 * Math.PI * freq * t) * 0.45f
                    
                    val signal = (noiseOffset * 0.4f + crackle * 0.5f + thud * 0.4f) * envelope
                    val sampleValue = Math.max(-1.0f, Math.min(1.0f, signal.toFloat()))
                    
                    buffer[i] = (sampleValue * Short.MAX_VALUE).toInt().toShort()
                }
                
                val audioTrack = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_GAME)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                    .build()
                
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                Thread.sleep(durationMs + 30L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// Achievements models and helper components
data class CandyAchievementItem(
    val id: String,
    val name: String,
    val category: String,
    val upgradeId: String,
    val targetLevel: Int,
    val emoji: String,
    val description: String,
    val tier: String,
    val currentLevel: Int
)

fun getTierColor(tier: String): Color {
    return when(tier) {
        "bronze" -> Color(0xFFCD7F32)   // Bronze
        "silver" -> Color(0xFFC0C0C0)   // Silver
        "gold" -> Color(0xFFFFD700)     // Gold
        "diamond" -> Color(0xFF26C6DA)  // Diamond cyan
        "platinum" -> Color(0xFFE5E4E2) // Platinum silver-white
        "sapphire" -> Color(0xFF2196F3) // Sapphire blue
        "emerald" -> Color(0xFF00E676)  // Emerald green
        "ruby" -> Color(0xFFFF1744)     // Ruby red
        "amethyst" -> Color(0xFFD500F9) // Amethyst purple
        "obsidian" -> Color(0xFF555555) // Dark obsidian grey
        "cosmic" -> Color(0xFFBB86FC)   // Cosmic dream purple
        "godly" -> Color(0xFFFFD54F)    // Divine radiant yellow
        else -> Color.White
    }
}

fun getTierLabelColor(tier: String, isCompleted: Boolean): Color {
    if (!isCompleted) return Color(0xFF917576)
    return when(tier) {
        "bronze" -> Color(0xFFD7CCC8)
        "silver" -> Color(0xFFECEFF1)
        "gold" -> Color(0xFFFFF9C4)
        "diamond" -> Color(0xFFE0F7FA)
        "platinum" -> Color(0xFFECEFF1)
        "sapphire" -> Color(0xFFBBDEFB)
        "emerald" -> Color(0xFFE8F5E9)
        "ruby" -> Color(0xFFFFEBEE)
        "amethyst" -> Color(0xFFE1BEE7)
        "obsidian" -> Color(0xFFCFD8DC)
        "cosmic" -> Color(0xFFF3E5F5)
        "godly" -> Color(0xFFFFE082)
        else -> Color.White
    }
}

fun getAchievementsList(state: GameState): List<CandyAchievementItem> {
    val items = mutableListOf<CandyAchievementItem>()
    
    // 1. Sugar Towers
    val towers = listOf(
        // Level, Tier, Title, Description
        10 to ("bronze" to Triple("Glazed Foundation", "🗼", "Build a stable foundation with 10 Sugar Towers.")),
        25 to ("silver" to Triple("Glazed Foundations", "🗼", "Let your sugar reach the sky with 25 Sugar Towers.")),
        50 to ("gold" to Triple("Crystalline Spoke", "🗼", "Fusing crystals into 50 towering monuments of glaze.")),
        100 to ("diamond" to Triple("Atmospheric Spun Spire", "🗼", "A historical milestone: 100 high-gravity towers of pure sugar.")),
        150 to ("platinum" to Triple("Sub-Orbital Confection", "🗼", "Erect 150 towers that break into sub-orbital space.")),
        200 to ("sapphire" to Triple("Stratospheric Sweets", "🗼", "Power 200 towers that reach into the cold stratosphere.")),
        250 to ("emerald" to Triple("Ionosphere Crunch", "🗼", "Spark electrostatic sweet storms at 250 towers.")),
        300 to ("ruby" to Triple("Mesosphere Malt", "🗼", "Solidify 300 towers in the mesosphere.")),
        350 to ("amethyst" to Triple("Thermosphere Toffee", "🗼", "Glaze 350 towers under thermosphere heat.")),
        400 to ("obsidian" to Triple("Exosphere Eclipse", "🗼", "Black out the sun with 400 space towers.")),
        450 to ("cosmic" to Triple("Cosmic Caramel Core", "🗼", "Connect 450 towers to cosmic gravity.")),
        500 to ("godly" to Triple("Universal Sugar Singularity", "🗼", "500 towers distort space-time into sweet candy."))
    )
    towers.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "tower_$lvl",
                name = info.first,
                category = "Sugar Towers",
                upgradeId = "click",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.clickPowerLevel
            )
        )
    }

    // 2. Candy Drones
    val drones = listOf(
        10 to ("bronze" to Triple("Bee Buzzers", "🐝", "Purchase 10 drones to hum sweet sugar songs.")),
        25 to ("silver" to Triple("Precision Buzzers", "🐝", "Establish active syrup exploration with 25 Candy Drones.")),
        50 to ("gold" to Triple("Honeycomb Hull", "🐝", "Beaming sugar coords from 50 autonomous mechanical bees.")),
        100 to ("diamond" to Triple("Advanced Radar Honey", "🐝", "100 super-drones synchronized into one cosmic confectionery hive.")),
        150 to ("platinum" to Triple("Sweet Propeller Blades", "🐝", "Equip 150 drones with elite silent propellers.")),
        200 to ("sapphire" to Triple("Quantum Swarms", "🐝", "Run 200 drones exhibiting quantum entanglement.")),
        250 to ("emerald" to Triple("Stardust Wings", "🐝", "Glow in space with 250 stardust-winged scouts.")),
        300 to ("ruby" to Triple("Nano-Confection Thrusters", "🐝", "Drive 300 drones using microscopic candy thrusters.")),
        350 to ("amethyst" to Triple("Interstellar Syrup Guidance", "🐝", "Guide 350 drones across star systems.")),
        400 to ("obsidian" to Triple("Antimatter Sugar Propellers", "🐝", "Launch 400 antimatter-propelled sweet drones.")),
        450 to ("cosmic" to Triple("Dimension-Flipping Wings", "🐝", "Fleet of 450 drones warping between dimensions.")),
        500 to ("godly" to Triple("Infinite Hivemind Drone", "🐝", "Achieve 500 drones forming an omniscient collective mind."))
    )
    drones.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "drone_$lvl",
                name = info.first,
                category = "Candy Drones",
                upgradeId = "drone",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.candyDroneLevel
            )
        )
    }

    // 3. Gingerbread Mills
    val mills = listOf(
        10 to ("bronze" to Triple("Sweet Butter Cogs", "🍪", "Set 10 warm ovens spinning.")),
        25 to ("silver" to Triple("Reinforced Sails", "🍪", "Bake crispy cookies non-stop with 25 Gingerbread Mills.")),
        50 to ("gold" to Triple("Flour-Power Gears", "🍪", "50 high-pressure sugar ovens blasting gingerbread heat.")),
        100 to ("diamond" to Triple("Sweet Butter Axles", "🍪", "100 Gingerbread Mills fused into a tasty matrix of warm cinnamon.")),
        150 to ("platinum" to Triple("Superheated Sugar Ovens", "🍪", "Bake at extreme temperatures with 150 ovens.")),
        200 to ("sapphire" to Triple("Nuclear Yeasting Chambers", "🍪", "Accelerate rising with 200 nuclear bakers.")),
        250 to ("emerald" to Triple("Quantum Flour Grinders", "🍪", "Feed 250 mills with subatomic flour grinding.")),
        300 to ("ruby" to Triple("Hyperdimensional Crust", "🍪", "Form 300 mills with folding multi-dimensional crusts.")),
        350 to ("amethyst" to Triple("Cosmic Molasses Turbines", "🍪", "Spin 350 heavy molasses turbos.")),
        400 to ("obsidian" to Triple("Singularity Spicers", "🍪", "Spice up 400 mills using gravity pinch-wells.")),
        450 to ("cosmic" to Triple("Dark Energy Bakeries", "🍪", "Infuse 450 mills with expanding dark sugar heat.")),
        500 to ("godly" to Triple("God-Tier Gingerbread Matrix", "🍪", "Transcend baking: 500 mills generating infinite dough."))
    )
    mills.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "ginger_$lvl",
                name = info.first,
                category = "Gingerbread Mills",
                upgradeId = "gingerbread",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.gingerbreadLevel
            )
        )
    }

    // 4. Cotton Clouds
    val clouds = listOf(
        10 to ("bronze" to Triple("Spun Sugar Wisps", "☁️", "Float 10 sugar vaporizers into the atmosphere.")),
        25 to ("silver" to Triple("Humid Spun Condensers", "☁️", "Unleash pink confectionery storms with 25 Cotton Clouds.")),
        50 to ("gold" to Triple("Cumulus Candy Shifters", "☁️", "50 high-pressure syrup clouds covering cookie horizons.")),
        100 to ("diamond" to Triple("Altocumulus Evaporators", "☁️", "100 clouds generating endless rainstorms of crystallizing sweet syrup.")),
        150 to ("platinum" to Triple("High-Pressure Sugar Vapor", "☁️", "Condense sweet vapor under 150 pressure jets.")),
        200 to ("sapphire" to Triple("Stormy Fudge Fronts", "☁️", "Form 200 turbulent dark fudge fronts.")),
        250 to ("emerald" to Triple("Meteorological Sweetness", "☁️", "Master global sweet weather with 250 clouds.")),
        300 to ("ruby" to Triple("Stratocumulus Crystallizers", "☁️", "Float 300 crystallizing stratocumulus structures.")),
        350 to ("amethyst" to Triple("Supersonic Candy Storms", "☁️", "Blast 350 supersonic sugar winds.")),
        400 to ("obsidian" to Triple("Anticyclonic Sugar Jets", "☁️", "Counter-rotate 400 cyclones of whipped topping.")),
        450 to ("cosmic" to Triple("Tropospheric Syrup Rain", "☁️", "Pour torrential caramel from 450 clouds.")),
        500 to ("godly" to Triple("Nebula Sweet Mist Synthesis", "☁️", "Generate space-born gas nebulae of pure cotton."))
    )
    clouds.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "cotton_$lvl",
                name = info.first,
                category = "Cotton Clouds",
                upgradeId = "cotton",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.cottonCloudLevel
            )
        )
    }

    // 5. Chocolate Volcanoes
    val volcanoes = listOf(
        10 to ("bronze" to Triple("Warm Cocoa Fountains", "🌋", "Quake the fudge floors with 10 lava chocolate vents.")),
        25 to ("silver" to Triple("Magma Fudge Stirrers", "🌋", "Erupt magma fudge continuously with 25 Chocolate Volcanoes.")),
        50 to ("gold" to Triple("Geothermal Caramel Cracks", "🌋", "Fusing 50 planetary volcano cores into dark cocoa eruptions.")),
        100 to ("diamond" to Triple("Volcanic Syrup Vents", "🌋", "Core deep chocolate extraction powered by 100 infinite volcanoes.")),
        150 to ("platinum" to Triple("Eruptive Chocolate Plumes", "🌋", "Shoot dark chocolate 150 times into space.")),
        200 to ("sapphire" to Triple("Tectonic Fudge Plates", "🌋", "Move continental plates over 200 lava pools.")),
        250 to ("emerald" to Triple("Superheated Volcano Core", "🌋", "Heat 250 volcano cores past smelting temperatures.")),
        300 to ("ruby" to Triple("Pyroclastic Candy Flows", "🌋", "Sweep cookie plains with 300 pyroclastic rivers.")),
        350 to ("amethyst" to Triple("Subterranean Cocoa Chambers", "🌋", "Expand deep caves of cocoa across 350 vents.")),
        400 to ("obsidian" to Triple("Mantle Chocolate Conduits", "🌋", "Tap 400 conduits into planetary syrup mantles.")),
        450 to ("cosmic" to Triple("Core Sweetness Extractors", "🌋", "Mine core magma with 450 ultra-deep wells.")),
        500 to ("godly" to Triple("Supernova Fudge Eruption", "🌋", "Blast 500 volcanoes culminating in a cosmic supernova!"))
    )
    volcanoes.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "volcano_$lvl",
                name = info.first,
                category = "Chocolate Volcanoes",
                upgradeId = "volcano",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.chocolateVolcanoLevel
            )
        )
    }

    // 6. Sugar Earths
    val earths = listOf(
        10 to ("bronze" to Triple("Sugary Continents", "🌍", "Form 10 beautiful chocolate globes of icing soil.")),
        25 to ("silver" to Triple("Thick Caramel Crust", "🌍", "Flow warm caramel oceans over 25 massive Sugar Earth planets.")),
        50 to ("gold" to Triple("Tectonic Sugar Shifts", "🌍", "Protect your confectionery planet biosphere with 50 systems.")),
        100 to ("diamond" to Triple("Continental Sweets", "🌍", "Construct 100 deep-mantle chocolate worlds with perfect sweet orbits.")),
        150 to ("platinum" to Triple("Biosphere Fudge", "🌍", "Support organic frosting life on 150 earths.")),
        200 to ("sapphire" to Triple("Atmospheric Icing Core", "🌍", "Enshroud 200 worlds in sweet icing shells.")),
        250 to ("emerald" to Triple("Magnetic Syrup Fields", "🌍", "Shield 250 planets with magnetic caramel dynamos.")),
        300 to ("ruby" to Triple("Super-Deep Chocolate Mantle", "🌍", "Solidify mantles across 300 chocolate globes.")),
        350 to ("amethyst" to Triple("Sweet Ocean Tides", "🌍", "Pull massive syrup tides across 350 planets.")),
        400 to ("obsidian" to Triple("Heliospheric Sugar Shield", "🌍", "Envelop 400 earths in heliospheric defense.")),
        450 to ("cosmic" to Triple("Gravitational Caramel Pull", "🌍", "Link 450 worlds in perfect orbital sweet locks.")),
        500 to ("godly" to Triple("Gaia Sweet World Alignment", "🌍", "Align 500 planets in a cosmic-scale grand candy design."))
    )
    earths.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "earth_$lvl",
                name = info.first,
                category = "Sugar Earths",
                upgradeId = "earth",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.sugarEarthLevel
            )
        )
    }

    // 7. Lollipop Galaxies
    val galaxies = listOf(
        10 to ("bronze" to Triple("Syrup Star Clusters", "🌌", "Ignite 10 sugar stars in newborn swirl nebulae.")),
        25 to ("silver" to Triple("Solar Syrup Flares", "🌌", "Draw cosmic candy shapes across the void with 25 Lollipop Galaxies.")),
        50 to ("gold" to Triple("Nebular Candy Nurseries", "🌌", "50 swirling black holes pulling dark matter caramel inside.")),
        100 to ("diamond" to Triple("Interstellar Sweet Clusters", "🌌", "100 galaxies linked together into an infinite web of universal confectionery.")),
        150 to ("platinum" to Triple("Lollipop Constellations", "🌌", "Map 150 galactic swirl alignments.")),
        200 to ("sapphire" to Triple("Wormhole Candy Chutes", "🌌", "Fast-travel sugar across 200 dimensional tunnels.")),
        250 to ("emerald" to Triple("Supermassive Sugar Hole", "🌌", "Collapse 250 galactic cores into massive syrup gravity wells.")),
        300 to ("ruby" to Triple("Quasar Confection Jets", "🌌", "Shoot 300 high-energy caramel quasar beams.")),
        350 to ("amethyst" to Triple("Dark Matter Caramel", "🌌", "Discover 350 galaxies of invisible sweet dark matter.")),
        400 to ("obsidian" to Triple("Cosmic String Liquorice", "🌌", "Spin 400 galactic webs using cosmic liquorice threads.")),
        450 to ("cosmic" to Triple("Event Horizon Candies", "🌌", "Harvest candies crossing 450 black hole horizons.")),
        500 to ("godly" to Triple("Multiverse Sweet Singularity", "🌌", "Converge 500 galaxies into an eternal confectionery nexus."))
    )
    galaxies.forEach { (lvl, data) ->
        val (tier, info) = data
        items.add(
            CandyAchievementItem(
                id = "galaxy_$lvl",
                name = info.first,
                category = "Lollipop Galaxies",
                upgradeId = "galaxy",
                targetLevel = lvl,
                emoji = info.second,
                description = info.third,
                tier = tier,
                currentLevel = state.lollipopGalaxyLevel
            )
        )
    }

    return items
}

@Composable
fun CandyStoreShelf(
    shelfName: String,
    shelfIcon: String,
    achievements: List<CandyAchievementItem>,
    onAchievementSelected: (CandyAchievementItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Shelf Title Label (e.g. "SUGAR TOWERS 🗼")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "$shelfIcon ${shelfName.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFFFFD54F)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Horizontal Scrollable Row of achievements sitting on the shelf
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            achievements.forEach { achievement ->
                AchievementJarItem(
                    achievement = achievement,
                    onClick = { onAchievementSelected(achievement) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // The wooden shelf plank itself
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB07D62), // Bevel light highlight
                            Color(0xFF8D5B4C), // Wooden light brown
                            Color(0xFF5C3A21)  // Dark shadow brown
                        )
                    )
                )
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}

@Composable
fun AchievementJarItem(
    achievement: CandyAchievementItem,
    onClick: () -> Unit
) {
    val isCompleted = achievement.currentLevel >= achievement.targetLevel
    val tierColor = getTierColor(achievement.tier)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        // The Glass Candy Jar!
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(
                    if (isCompleted) {
                        Color.White.copy(alpha = 0.15f) // Shiny transparent glass look
                    } else {
                        Color.Black.copy(alpha = 0.25f) // Dusty/dark empty jar look
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = if (isCompleted) tierColor else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Shiny reflection line on the left of glass jar to make it look hyper-polished!
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp, top = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            
            // Jar wood/metallic cap at top
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isCompleted) {
                            tierColor
                        } else {
                            Color(0xFF5C3A21).copy(alpha = 0.5f)
                        }
                    )
            )
            
            // Jar contents (the emoji!)
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Text(
                        text = achievement.emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.scale(1.1f)
                    )
                } else {
                    // Gray silhouette with lock
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = achievement.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.alpha(0.12f)
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label underneath (Target Level)
        Text(
            text = "Lvl ${achievement.targetLevel}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = getTierLabelColor(achievement.tier, isCompleted)
        )
    }
}
