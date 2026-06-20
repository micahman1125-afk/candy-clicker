package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow

data class FloatingEffect(
    val id: Long,
    val text: String,
    val offsetX: Float,
    val offsetY: Float
)

data class UpgradeItem(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Double,
    val costMultiplier: Double,
    val cpsIncrease: Double,
    val clickPowerIncrease: Double,
    val level: Int,
    val cost: Double,
    val isBuilding: Boolean = true
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _floatingEffects = MutableStateFlow<List<FloatingEffect>>(emptyList())
    val floatingEffects: StateFlow<List<FloatingEffect>> = _floatingEffects.asStateFlow()

    // Offline earnings state
    private val _offlineEarnings = MutableStateFlow<Double?>(null)
    val offlineEarnings: StateFlow<Double?> = _offlineEarnings.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lastTickTime: Long = System.currentTimeMillis()

    init {
        // Collect saved game state from Room
        viewModelScope.launch {
            repository.gameStateFlow.firstOrNull()?.let { savedState ->
                _gameState.value = savedState
                calculateOfflineIncome(savedState)
            } ?: run {
                // First launch, save default state
                val defaultState = GameState()
                repository.saveGameState(defaultState)
                _gameState.value = defaultState
            }
            lastTickTime = System.currentTimeMillis()
            startGameLoop()
        }
    }

    private fun calculateOfflineIncome(savedState: GameState) {
        val now = System.currentTimeMillis()
        val timeDifferenceMs = now - savedState.lastActiveTime
        if (timeDifferenceMs > 5000) { // More than 5 seconds away
            val cps = getCps(savedState)
            if (cps > 0.0) {
                val secondsElapsed = timeDifferenceMs / 1000.0
                // Cap offline earnings to 12 hours max to protect economy balance
                val cappedSeconds = minOf(secondsElapsed, 43200.0)
                val earned = cappedSeconds * cps
                
                _offlineEarnings.value = earned
                
                // Save updated candies
                updateState {
                    copy(
                        candies = candies + earned,
                        totalCandiesEarned = totalCandiesEarned + earned,
                        lastActiveTime = now
                    )
                }
            }
        }
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(100) // 10 ticks per second for extremely responsive displays
                val now = System.currentTimeMillis()
                val deltaSeconds = (now - lastTickTime) / 1000.0
                lastTickTime = now

                val currentState = _gameState.value
                val cps = getCps(currentState)
                if (cps > 0) {
                    val generated = cps * deltaSeconds
                    updateState {
                        copy(
                            candies = candies + generated,
                            totalCandiesEarned = totalCandiesEarned + generated,
                            lastActiveTime = now
                        )
                    }
                } else {
                    // Update heartbeat active time
                    updateState {
                        copy(lastActiveTime = now)
                    }
                }
            }
        }
    }

    // Actions
    fun onCandyClicked(x: Float, y: Float) {
        val currentState = _gameState.value
        val clickPower = getClickPower(currentState)
        
        // Add floating effect
        val newEffect = FloatingEffect(
            id = System.nanoTime(),
            text = "+${formatValue(clickPower)}",
            offsetX = x,
            offsetY = y
        )
        _floatingEffects.value = _floatingEffects.value + newEffect

        // Add candies
        updateState {
            copy(
                candies = candies + clickPower,
                totalCandiesEarned = totalCandiesEarned + clickPower,
                clickCount = clickCount + 1
            )
        }

        // Auto remove floating effect after 600ms
        viewModelScope.launch {
            delay(600)
            _floatingEffects.value = _floatingEffects.value.filter { it.id != newEffect.id }
        }
    }

    fun buyUpgrade(upgradeId: String, count: Int = 1) {
        val currentState = _gameState.value
        val list = getUpgradeItems(currentState)
        val selected = list.find { it.id == upgradeId } ?: return

        val costToUse = if (selected.isBuilding && count > 1) {
            calculateMultiCost(selected.baseCost, selected.level, count, selected.costMultiplier)
        } else {
            selected.cost
        }

        if (currentState.candies >= costToUse) {
            val isIntervalUpgrade = upgradeId.contains("_")
            updateState {
                val newPurchased = if (isIntervalUpgrade) {
                    if (purchasedUpgrades.isEmpty()) upgradeId else "$purchasedUpgrades,$upgradeId"
                } else {
                    purchasedUpgrades
                }
                copy(
                    candies = candies - costToUse,
                    purchasedUpgrades = newPurchased,
                    clickPowerLevel = if (upgradeId == "click") clickPowerLevel + count else clickPowerLevel,
                    candyDroneLevel = if (upgradeId == "drone") candyDroneLevel + count else candyDroneLevel,
                    gingerbreadLevel = if (upgradeId == "gingerbread") gingerbreadLevel + count else gingerbreadLevel,
                    cottonCloudLevel = if (upgradeId == "cotton") cottonCloudLevel + count else cottonCloudLevel,
                    chocolateVolcanoLevel = if (upgradeId == "volcano") chocolateVolcanoLevel + count else chocolateVolcanoLevel,
                    sugarEarthLevel = if (upgradeId == "earth") sugarEarthLevel + count else sugarEarthLevel,
                    lollipopGalaxyLevel = if (upgradeId == "galaxy") lollipopGalaxyLevel + count else lollipopGalaxyLevel,
                    goldenSpatulaLevel = if (upgradeId == "spatula") goldenSpatulaLevel + 1 else goldenSpatulaLevel,
                    sweetSynergiesLevel = if (upgradeId == "synergies") sweetSynergiesLevel + 1 else sweetSynergiesLevel,
                    criticalMunchLevel = if (upgradeId == "munch") criticalMunchLevel + 1 else criticalMunchLevel
                )
            }
        }
    }

    fun sellUpgrade(upgradeId: String, count: Int = 1) {
        val currentState = _gameState.value
        val list = getUpgradeItems(currentState)
        val selected = list.find { it.id == upgradeId } ?: return
        if (!selected.isBuilding) return

        val actualCount = minOf(count, selected.level)
        if (actualCount <= 0) return

        val refundAmount = calculateMultiCost(selected.baseCost, selected.level - actualCount, actualCount, selected.costMultiplier) * 0.5

        updateState {
            copy(
                candies = candies + refundAmount,
                clickPowerLevel = if (upgradeId == "click") maxOf(0, clickPowerLevel - actualCount) else clickPowerLevel,
                candyDroneLevel = if (upgradeId == "drone") maxOf(0, candyDroneLevel - actualCount) else candyDroneLevel,
                gingerbreadLevel = if (upgradeId == "gingerbread") maxOf(0, gingerbreadLevel - actualCount) else gingerbreadLevel,
                cottonCloudLevel = if (upgradeId == "cotton") maxOf(0, cottonCloudLevel - actualCount) else cottonCloudLevel,
                chocolateVolcanoLevel = if (upgradeId == "volcano") maxOf(0, chocolateVolcanoLevel - actualCount) else chocolateVolcanoLevel,
                sugarEarthLevel = if (upgradeId == "earth") maxOf(0, sugarEarthLevel - actualCount) else sugarEarthLevel,
                lollipopGalaxyLevel = if (upgradeId == "galaxy") maxOf(0, lollipopGalaxyLevel - actualCount) else lollipopGalaxyLevel
            )
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            val freshState = GameState()
            repository.saveGameState(freshState)
            _gameState.value = freshState
            lastTickTime = System.currentTimeMillis()
        }
    }

    fun toggleSound() {
        updateState {
            copy(soundOn = !soundOn)
        }
    }

    fun toggleVibration() {
        updateState {
            copy(vibrationOn = !vibrationOn)
        }
    }

    fun toggleLollipopMovement() {
        updateState {
            copy(lollipopMovementOn = !lollipopMovementOn)
        }
    }

    fun prestige() {
        val currentState = _gameState.value
        val prestigeGoal = 1_000_000_000_000.0
        if (currentState.candies >= prestigeGoal) {
            val pointsToClaim = (currentState.candies / prestigeGoal).toLong()
            updateState {
                copy(
                    candies = 0.0,
                    clickPowerLevel = 0,
                    candyDroneLevel = 0,
                    gingerbreadLevel = 0,
                    cottonCloudLevel = 0,
                    chocolateVolcanoLevel = 0,
                    sugarEarthLevel = 0,
                    lollipopGalaxyLevel = 0,
                    goldenSpatulaLevel = 0,
                    sweetSynergiesLevel = 0,
                    criticalMunchLevel = 0,
                    prestigePoints = prestigePoints + pointsToClaim,
                    purchasedUpgrades = ""
                )
            }
        }
    }

    // Helper functions
    private fun updateState(block: GameState.() -> GameState) {
        val newState = _gameState.value.block()
        _gameState.value = newState
        
        // Save to Database asynchronously on background thread
        viewModelScope.launch {
            repository.saveGameState(newState)
        }
    }

    fun getClickPower(state: GameState): Double {
        val multiplier = 1.0 + state.prestigePoints * 0.01 // +1% click power boost per prestige point!
        val basePower = 1.0 + (state.goldenSpatulaLevel * 2.5) + (state.criticalMunchLevel * 12.0)
        return basePower * multiplier
    }

    fun getBuildingMultiplier(state: GameState, buildingId: String): Double {
        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()
        val intervals = listOf(25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500)
        var boughtCount = 0
        for (interval in intervals) {
            val upgradeId = "${buildingId}_$interval"
            if (purchasedSet.contains(upgradeId)) {
                boughtCount++
            }
        }
        return 2.0.pow(boughtCount.toDouble())
    }

    fun getCps(state: GameState): Double {
        val multiplier = (1.0 + state.prestigePoints * 0.01) * (1.0 + state.sweetSynergiesLevel * 0.15) // +15% CPS boost per synergy level!
        
        val clickMult = getBuildingMultiplier(state, "click")
        val droneMult = getBuildingMultiplier(state, "drone")
        val gingerbreadMult = getBuildingMultiplier(state, "gingerbread")
        val cottonMult = getBuildingMultiplier(state, "cotton")
        val volcanoMult = getBuildingMultiplier(state, "volcano")
        val earthMult = getBuildingMultiplier(state, "earth")
        val galaxyMult = getBuildingMultiplier(state, "galaxy")

        val baseCps = (state.clickPowerLevel * 0.1 * clickMult) +
               (state.candyDroneLevel * 0.5 * droneMult) +
               (state.gingerbreadLevel * 4.0 * gingerbreadMult) +
               (state.cottonCloudLevel * 32.0 * cottonMult) +
               (state.chocolateVolcanoLevel * 260.0 * volcanoMult) +
               (state.sugarEarthLevel * 600.0 * earthMult) +
               (state.lollipopGalaxyLevel * 1400.0 * galaxyMult)
        return baseCps * multiplier
    }

    private fun getIntervalUpgradeDetails(buildingId: String, interval: Int, baseCostOfBuilding: Double, currentLevel: Int, state: GameState): UpgradeItem? {
        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()
        val upgradeId = "${buildingId}_$interval"
        
        // Hide if already purchased
        if (purchasedSet.contains(upgradeId)) {
            return null
        }
        // Only show once the building quantity (level) is at least the interval target
        if (currentLevel < interval) {
            return null
        }
        
        val buildingName = when (buildingId) {
            "click" -> "Sugar Tower"
            "drone" -> "Candy Drone"
            "gingerbread" -> "Gingerbread Mill"
            "cotton" -> "Cotton Cloud"
            "volcano" -> "Chocolate Volcano"
            "earth" -> "Sugar Earth"
            "galaxy" -> "Lollipop Galaxy"
            else -> "Producer"
        }
        
        val name = when (buildingId) {
            "click" -> when (interval) {
                25 -> "Glazed Foundations"
                50 -> "Crystalline Spoke"
                100 -> "Atmospheric Spun Spire"
                150 -> "Sub-Orbital Confection"
                200 -> "Stratospheric Sweets"
                250 -> "Ionosphere Crunch"
                300 -> "Mesosphere Malt"
                350 -> "Thermosphere Toffee"
                400 -> "Exosphere Eclipse"
                450 -> "Cosmic Caramel Core"
                500 -> "Universal Sugar Singularity"
                else -> "Sugar Tower Level $interval"
            }
            "drone" -> when (interval) {
                25 -> "Precision Buzzers"
                50 -> "Honeycomb Hull"
                100 -> "Advanced Radar Honey"
                150 -> "Sweet Propeller Blades"
                200 -> "Quantum Swarms"
                250 -> "Stardust Wings"
                300 -> "Nano-Confection Thrusters"
                350 -> "Interstellar Syrup Guidance"
                400 -> "Antimatter Sugar Propellers"
                450 -> "Dimension-Flipping Wings"
                500 -> "Infinite Hivemind Drone"
                else -> "Candy Drone Level $interval"
            }
            "gingerbread" -> when (interval) {
                25 -> "Reinforced Sails"
                50 -> "Flour-Power Gears"
                100 -> "Sweet Butter Axles"
                150 -> "Superheated Sugar Ovens"
                200 -> "Nuclear Yeasting Chambers"
                250 -> "Quantum Flour Grinders"
                300 -> "Hyperdimensional Crust"
                350 -> "Cosmic Molasses Turbines"
                400 -> "Singularity Spicers"
                450 -> "Dark Energy Bakeries"
                500 -> "God-Tier Gingerbread Matrix"
                else -> "Gingerbread Mill Level $interval"
            }
            "cotton" -> when (interval) {
                25 -> "Humid Spun Condensers"
                50 -> "Cumulus Candy Shifters"
                100 -> "Altocumulus Evaporators"
                150 -> "High-Pressure Sugar Vapor"
                200 -> "Stormy Fudge Fronts"
                250 -> "Meteorological Sweetness"
                300 -> "Stratocumulus Crystallizers"
                350 -> "Supersonic Candy Storms"
                400 -> "Anticylonic Sugar Jets"
                450 -> "Tropospheric Syrup Rain"
                500 -> "Nebula Sweet Mist Synthesis"
                else -> "Cotton Cloud Level $interval"
            }
            "volcano" -> when (interval) {
                25 -> "Magma Fudge Stirrers"
                50 -> "Geothermal Caramel Cracks"
                100 -> "Volcanic Syrup Vents"
                150 -> "Eruptive Chocolate Plumes"
                200 -> "Tectonic Fudge Plates"
                250 -> "Superheated Volcano Core"
                300 -> "Pyroclastic Candy Flows"
                350 -> "Subterranean Cocoa Chambers"
                400 -> "Mantle Chocolate Conduits"
                450 -> "Core Sweetness Extractors"
                500 -> "Supernova Fudge Eruption"
                else -> "Chocolate Volcano Level $interval"
            }
            "earth" -> when (interval) {
                25 -> "Thick Caramel Crust"
                50 -> "Tectonic Sugar Shifts"
                100 -> "Continental Sweets"
                150 -> "Biosphere Fudge"
                200 -> "Atmospheric Icing Core"
                250 -> "Magnetic Syrup Fields"
                300 -> "Super-Deep Chocolate Mantle"
                350 -> "Sweet Ocean Tides"
                400 -> "Heliospheric Sugar Shield"
                450 -> "Gravitational Caramel Pull"
                500 -> "Gaia Sweet World Alignment"
                else -> "Sugar Earth Level $interval"
            }
            "galaxy" -> when (interval) {
                25 -> "Solar Syrup Flares"
                50 -> "Nebular Candy Nurseries"
                100 -> "Interstellar Sweet Clusters"
                150 -> "Lollipop Constellations"
                200 -> "Wormhole Candy Chutes"
                250 -> "Supermassive Sugar Hole"
                300 -> "Quasar Confection Jets"
                350 -> "Dark Matter Caramel"
                400 -> "Cosmic String Liquorice"
                450 -> "Event Horizon Candies"
                500 -> "Multiverse Sweet Singularity"
                else -> "Lollipop Galaxy Level $interval"
            }
            else -> "Bonus Level $interval"
        }
        
        val upgradeCost = baseCostOfBuilding * 1.15.pow(interval.toDouble()) * 2.5
        
        return UpgradeItem(
            id = upgradeId,
            name = name,
            description = "Doubles the efficiency of all ${buildingName}s (+100% CPS)!",
            baseCost = upgradeCost,
            costMultiplier = 1.0,
            cpsIncrease = 0.0,
            clickPowerIncrease = 0.0,
            level = 1,
            cost = upgradeCost,
            isBuilding = false
        )
    }

    fun getUpgradeItems(state: GameState): List<UpgradeItem> {
        val standardList = listOf(
            UpgradeItem(
                id = "click",
                name = "Sugar Tower",
                description = "A compact spun-sugar tower. Generates +0.1 candies/sec.",
                baseCost = 15.0,
                costMultiplier = 1.15,
                cpsIncrease = 0.1,
                clickPowerIncrease = 0.0,
                level = state.clickPowerLevel,
                cost = calculateCost(15.0, state.clickPowerLevel)
            ),
            UpgradeItem(
                id = "drone",
                name = "Candy Drone",
                description = "Automated mechanical bees. Generates +0.5 candies/sec.",
                baseCost = 100.0,
                costMultiplier = 1.15,
                cpsIncrease = 0.5,
                clickPowerIncrease = 0.0,
                level = state.candyDroneLevel,
                cost = calculateCost(100.0, state.candyDroneLevel)
            ),
            UpgradeItem(
                id = "gingerbread",
                name = "Gingerbread Mill",
                description = "Grinds delicious cookie-crust ingredients. Generates +4.0/sec.",
                baseCost = 1100.0,
                costMultiplier = 1.15,
                cpsIncrease = 4.0,
                clickPowerIncrease = 0.0,
                level = state.gingerbreadLevel,
                cost = calculateCost(1100.0, state.gingerbreadLevel)
            ),
            UpgradeItem(
                id = "cotton",
                name = "Cotton Cloud",
                description = "Spun sugar vaporizers drifting in the sky. Generates +32.0/sec.",
                baseCost = 12000.0,
                costMultiplier = 1.15,
                cpsIncrease = 32.0,
                clickPowerIncrease = 0.0,
                level = state.cottonCloudLevel,
                cost = calculateCost(12000.0, state.cottonCloudLevel)
            ),
            UpgradeItem(
                id = "volcano",
                name = "Chocolate Volcano",
                description = "Erupts molten lava fudge. Generates +260.0 candies/sec.",
                baseCost = 130000.0,
                costMultiplier = 1.15,
                cpsIncrease = 260.0,
                clickPowerIncrease = 0.0,
                level = state.chocolateVolcanoLevel,
                cost = calculateCost(130000.0, state.chocolateVolcanoLevel)
            ),
            UpgradeItem(
                id = "earth",
                name = "Sugar Earth",
                description = "A massive chocolate-crust globe with sweet icing soils. Generates +600.0/sec.",
                baseCost = 450000.0,
                costMultiplier = 1.15,
                cpsIncrease = 600.0,
                clickPowerIncrease = 0.0,
                level = state.sugarEarthLevel,
                cost = calculateCost(450000.0, state.sugarEarthLevel)
            ),
            UpgradeItem(
                id = "galaxy",
                name = "Lollipop Galaxy",
                description = "A swirling cluster of sugar solar systems. Generates +1,400.0/sec.",
                baseCost = 1400000.0,
                costMultiplier = 1.15,
                cpsIncrease = 1400.0,
                clickPowerIncrease = 0.0,
                level = state.lollipopGalaxyLevel,
                cost = calculateCost(1400000.0, state.lollipopGalaxyLevel)
            ),
            UpgradeItem(
                id = "spatula",
                name = "Golden Spatula",
                description = "Crafted from solidified sugar glass. Adds +2.5 to direct tap power.",
                baseCost = 150.0,
                costMultiplier = 1.25,
                cpsIncrease = 0.0,
                clickPowerIncrease = 2.5,
                level = state.goldenSpatulaLevel,
                cost = calculateCost(150.0, state.goldenSpatulaLevel, 1.25),
                isBuilding = false
            ),
            UpgradeItem(
                id = "synergies",
                name = "Sweet Synergies",
                description = "Boosts the efficiency of all automated producers by +15% per upgrade level.",
                baseCost = 8000.0,
                costMultiplier = 1.35,
                cpsIncrease = 0.0,
                clickPowerIncrease = 0.0,
                level = state.sweetSynergiesLevel,
                cost = calculateCost(8000.0, state.sweetSynergiesLevel, 1.35),
                isBuilding = false
            ),
            UpgradeItem(
                id = "munch",
                name = "Critical Munch",
                description = "Unlocks elite tapping power. Adds +12.0 to direct tap power.",
                baseCost = 120000.0,
                costMultiplier = 1.45,
                cpsIncrease = 0.0,
                clickPowerIncrease = 12.0,
                level = state.criticalMunchLevel,
                cost = calculateCost(120000.0, state.criticalMunchLevel, 1.45),
                isBuilding = false
            )
        )

        val intervalList = mutableListOf<UpgradeItem>()
        val intervals = listOf(25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500)
        val stats = listOf(
            Triple("click", 15.0, state.clickPowerLevel),
            Triple("drone", 100.0, state.candyDroneLevel),
            Triple("gingerbread", 1100.0, state.gingerbreadLevel),
            Triple("cotton", 12000.0, state.cottonCloudLevel),
            Triple("volcano", 130000.0, state.chocolateVolcanoLevel),
            Triple("earth", 450000.0, state.sugarEarthLevel),
            Triple("galaxy", 1400000.0, state.lollipopGalaxyLevel)
        )

        for ((bId, bBaseCost, bLevel) in stats) {
            for (interval in intervals) {
                getIntervalUpgradeDetails(bId, interval, bBaseCost, bLevel, state)?.let {
                    intervalList.add(it)
                }
            }
        }

        return standardList + intervalList
    }

    private fun calculateCost(base: Double, level: Int, multiplier: Double = 1.15): Double {
        return base * multiplier.pow(level.toDouble())
    }

    fun calculateMultiCost(base: Double, currentLevel: Int, count: Int, multiplier: Double = 1.15): Double {
        var total = 0.0
        for (i in 0 until count) {
            total += base * multiplier.pow((currentLevel + i).toDouble())
        }
        return total
    }

    fun formatValue(value: Double): String {
        return when {
            value >= 1_000_000_000_000.0 -> String.format("%.2f T", value / 1_000_000_000_000.0)
            value >= 1_000_000_000.0 -> String.format("%.2f B", value / 1_000_000_000.0)
            value >= 1_000_000.0 -> String.format("%.2f M", value / 1_000_000.0)
            value >= 1_000.0 -> String.format("%.1f K", value / 1_000.0)
            else -> String.format("%.1f", value)
        }
    }

    override fun onCleared() {
        gameLoopJob?.cancel()
        super.onCleared()
    }
}
