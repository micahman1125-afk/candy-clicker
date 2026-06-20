package com.example

import android.os.Bundle
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
  override fun getAttributionTag(): String? {
    return "audio"
  }

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
    val lollipopBitmap = remember(context) {
        loadTransparentLollipop(context)
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var scaleTarget by remember { mutableStateOf(1.0f) }
    var activeTab by rememberSaveable { mutableStateOf("game") }
    var shopSubcategory by rememberSaveable { mutableStateOf("buildings") }
    val coroutineScope = rememberCoroutineScope()

    // Physical tap-spring animation for squash on click
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
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
                    .height(72.dp)
                    .background(Color(0xCCFEDADF))
                    .border(1.dp, Color(0xFFF4DDDE))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                .weight(1.0f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Centered Total Candies and CPS Count (Background Removed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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

            if (activeTab == "game") {
                // Interactive Tap Target (Occupying full area of the game tab)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(285.dp)
                        ) {
                            Image(
                                bitmap = lollipopBitmap,
                                contentDescription = "Giant Candy click target",
                                modifier = Modifier
                                    .size(210.dp)
                                    .scale(scale)
                                    .rotate(-45f)
                                    .testTag("candy_click_target")
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            scaleTarget = 0.82f
                                            delay(70)
                                            scaleTarget = 1.0f
                                        }
                                        CrunchSoundPlayer.play()
                                        val randomX = (130..190).random().toFloat()
                                        val randomY = (130..190).random().toFloat()
                                        viewModel.onCandyClicked(randomX, randomY)
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Floating scores
                    Box(modifier = Modifier.fillMaxSize()) {
                        floatingEffects.forEach { effect ->
                            FloatingScoreText(effect = effect)
                        }
                    }
                }
            } else if (activeTab == "secret_sugars") {
                // --- SECRET SUGARS TAB ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xDF2B1516)) // Custom classy semi-transparent deep burgundy
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                                    text = "TAP POWER / CLICK BOOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF9BCBC)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${viewModel.formatValue(viewModel.getClickPower(state))}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Each tap yields this extra sweet boost",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF917576),
                                    fontSize = 11.sp
                                )
                            }
                            Text(text = "⚡", fontSize = 28.sp)
                        }

                        // Column 2: Prestige Progress
                        val prestigeGoal = 1_000_000_000_000.0
                        val canPrestige = state.candies >= prestigeGoal
                        val progressRatio = (state.candies / prestigeGoal).coerceIn(0.0, 1.0).toFloat()

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
                                        text = "TAP HERE TO CLAIM +1 PT!",
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
                                            text = "${viewModel.formatValue(state.candies)} / 1.00 T",
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

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else if (activeTab == "buildings") {
                // --- CANDY STORE TAB ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xDF2B1516)) // Custom classy semi-transparent deep burgundy
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
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

                            val categoryFilteredItems = viewModel.getUpgradeItems(state).filter {
                                if (shopSubcategory == "upgrades") !it.isBuilding else it.isBuilding
                            }
                            val availableCount = categoryFilteredItems.count { state.candies >= it.cost }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF547D))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$availableCount BUYABLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
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
                            if (shopSubcategory == "upgrades") !it.isBuilding else it.isBuilding
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1.0f)
                                .testTag("upgrades_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(upgrades, key = { it.id }) { item ->
                                BentoStoreCard(
                                    item = item,
                                    currentCandies = state.candies,
                                    onBuy = { viewModel.buyUpgrade(item.id) },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            } else if (activeTab == "settings") {
                // --- SETTINGS TAB ---
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xDF2B1516)) // Custom classy semi-transparent deep burgundy
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                targetValue = -120f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
            )
        }
    }

    Text(
        text = effect.text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFF547D), // neon raspberry pink pop
        modifier = Modifier
            .offset { IntOffset((effect.offsetX.toInt() / 2) - 15, (effect.offsetY.toInt() / 2) + yAnim.value.toInt() - 10) }
            .alpha(alphaAnim.value)
    )
}

// Upgrade item representing store element inside the deep burgundy shop layout
@Composable
fun BentoStoreCard(
    item: UpgradeItem,
    currentCandies: Double,
    onBuy: () -> Unit,
    viewModel: GameViewModel
) {
    val canAfford = currentCandies >= item.cost
    val icon = when {
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
        item.id == "spatula" -> "🥄"
        item.id == "synergies" -> "🤝"
        item.id == "munch" -> "⚡"
        else -> "🌌"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF)) // White 10%
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            .clickable(enabled = canAfford, onClick = onBuy)
            .padding(12.dp)
    ) {
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (item.level > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFDADB))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lv.${item.level}",
                                color = Color(0xFF2B1516),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xCCFFFFFF),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cost: ${viewModel.formatValue(item.cost)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color(0xFFFFDADB) else Color(0x80FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Buy button/indication
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canAfford) Color(0xFFFF547D) else Color(0x15FFFFFF))
                    .clickable(enabled = canAfford, onClick = onBuy)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BUY",
                    fontWeight = FontWeight.Black,
                    color = if (canAfford) Color.White else Color(0x40FFFFFF),
                    fontSize = 11.sp
                )
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
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
    
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
