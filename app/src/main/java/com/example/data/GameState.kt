package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val candies: Double = 0.0,
    val totalCandiesEarned: Double = 0.0,
    val totalClickCandiesEarned: Double = 0.0,
    val clickCount: Int = 0,
    val lastActiveTime: Long = System.currentTimeMillis(),
    
    // Upgrade levels
    val clickPowerLevel: Int = 0,
    val candyDroneLevel: Int = 0,
    val gingerbreadLevel: Int = 0,
    val cottonCloudLevel: Int = 0,
    val chocolateVolcanoLevel: Int = 0,
    val sugarEarthLevel: Int = 0,
    val lollipopGalaxyLevel: Int = 0,
    val goldenSpatulaLevel: Int = 0,
    val mittLevel: Int = 0,

    // Tower total candy produced trackers (resets on prestige)
    val mittsTotalEarned: Double = 0.0,
    val clickTotalEarned: Double = 0.0,
    val droneTotalEarned: Double = 0.0,
    val gingerbreadTotalEarned: Double = 0.0,
    val cottonTotalEarned: Double = 0.0,
    val volcanoTotalEarned: Double = 0.0,
    val earthTotalEarned: Double = 0.0,
    val galaxyTotalEarned: Double = 0.0,

    // Settings & Prestige
    val soundOn: Boolean = true,
    val vibrationOn: Boolean = true,
    val lollipopMovementOn: Boolean = true,
    val prestigePoints: Long = 0L,
    val purchasedUpgrades: String = ""
)
