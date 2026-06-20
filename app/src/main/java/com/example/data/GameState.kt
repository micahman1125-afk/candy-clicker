package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val candies: Double = 0.0,
    val totalCandiesEarned: Double = 0.0,
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
    val sweetSynergiesLevel: Int = 0,
    val criticalMunchLevel: Int = 0,

    // Settings & Prestige
    val soundOn: Boolean = true,
    val vibrationOn: Boolean = true,
    val prestigePoints: Long = 0L,
    val purchasedUpgrades: String = ""
)
