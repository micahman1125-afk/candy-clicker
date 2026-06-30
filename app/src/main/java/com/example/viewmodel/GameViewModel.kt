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
                
                // Save updated candies and individual tower totals
                updateState {
                    val mittCps = getTowerCps(this, "mitt")
                    val clickCps = getTowerCps(this, "click")
                    val droneCps = getTowerCps(this, "drone")
                    val gingerbreadCps = getTowerCps(this, "gingerbread")
                    val cottonCps = getTowerCps(this, "cotton")
                    val volcanoCps = getTowerCps(this, "volcano")
                    val earthCps = getTowerCps(this, "earth")
                    val galaxyCps = getTowerCps(this, "galaxy")

                    copy(
                        candies = candies + earned,
                        totalCandiesEarned = totalCandiesEarned + earned,
                        mittsTotalEarned = mittsTotalEarned + (mittCps * cappedSeconds),
                        clickTotalEarned = clickTotalEarned + (clickCps * cappedSeconds),
                        droneTotalEarned = droneTotalEarned + (droneCps * cappedSeconds),
                        gingerbreadTotalEarned = gingerbreadTotalEarned + (gingerbreadCps * cappedSeconds),
                        cottonTotalEarned = cottonTotalEarned + (cottonCps * cappedSeconds),
                        volcanoTotalEarned = volcanoTotalEarned + (volcanoCps * cappedSeconds),
                        earthTotalEarned = earthTotalEarned + (earthCps * cappedSeconds),
                        galaxyTotalEarned = galaxyTotalEarned + (galaxyCps * cappedSeconds),
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
                        val mittCps = getTowerCps(this, "mitt")
                        val clickCps = getTowerCps(this, "click")
                        val droneCps = getTowerCps(this, "drone")
                        val gingerbreadCps = getTowerCps(this, "gingerbread")
                        val cottonCps = getTowerCps(this, "cotton")
                        val volcanoCps = getTowerCps(this, "volcano")
                        val earthCps = getTowerCps(this, "earth")
                        val galaxyCps = getTowerCps(this, "galaxy")

                        copy(
                            candies = candies + generated,
                            totalCandiesEarned = totalCandiesEarned + generated,
                            mittsTotalEarned = mittsTotalEarned + (mittCps * deltaSeconds),
                            clickTotalEarned = clickTotalEarned + (clickCps * deltaSeconds),
                            droneTotalEarned = droneTotalEarned + (droneCps * deltaSeconds),
                            gingerbreadTotalEarned = gingerbreadTotalEarned + (gingerbreadCps * deltaSeconds),
                            cottonTotalEarned = cottonTotalEarned + (cottonCps * deltaSeconds),
                            volcanoTotalEarned = volcanoTotalEarned + (volcanoCps * deltaSeconds),
                            earthTotalEarned = earthTotalEarned + (earthCps * deltaSeconds),
                            galaxyTotalEarned = galaxyTotalEarned + (galaxyCps * deltaSeconds),
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
        val baseClickPower = getClickPower(currentState)
        val purchasedSet = currentState.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()
        var bonusSpatulaPower = 0.0
        val spatulaList = listOf(
            "spatula_wooden",
            "spatula_plastic",
            "spatula_silicon",
            "spatula_steel",
            "spatula_aluminum",
            "spatula_titanium",
            "spatula_iridium",
            "spatula_gold"
        )
        for (spatula in spatulaList) {
            if (purchasedSet.contains(spatula)) {
                bonusSpatulaPower += getCps(currentState) * 0.01
            }
        }
        val clickPower = baseClickPower + bonusSpatulaPower
        
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
                totalClickCandiesEarned = totalClickCandiesEarned + clickPower,
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
            val isUpgrade = !selected.isBuilding
            updateState {
                val newPurchased = if (isUpgrade) {
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
                    mittLevel = if (upgradeId == "mitts") mittLevel + count else mittLevel
                )
            }
        }
    }

    fun buyMaxAffordableUpgrades() {
        val currentState = _gameState.value
        var candies = currentState.candies
        val purchasedSet = currentState.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toMutableSet()
        
        var changed = true
        while (changed) {
            changed = false
            val tempState = currentState.copy(
                candies = candies,
                purchasedUpgrades = purchasedSet.joinToString(",")
            )
            val availableUpgrades = getUpgradeItems(tempState).filter { !it.isBuilding && !purchasedSet.contains(it.id) }
            val affordable = availableUpgrades.filter { candies >= it.cost }.sortedBy { it.cost }
            if (affordable.isNotEmpty()) {
                val toBuy = affordable.first()
                candies -= toBuy.cost
                purchasedSet.add(toBuy.id)
                changed = true
            }
        }
        
        val finalPurchasedString = purchasedSet.joinToString(",")
        if (candies != currentState.candies || finalPurchasedString != currentState.purchasedUpgrades) {
            updateState {
                copy(
                    candies = candies,
                    purchasedUpgrades = finalPurchasedString
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
                lollipopGalaxyLevel = if (upgradeId == "galaxy") maxOf(0, lollipopGalaxyLevel - actualCount) else lollipopGalaxyLevel,
                mittLevel = if (upgradeId == "mitts") maxOf(0, mittLevel - actualCount) else mittLevel
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
        val totalPrestigeCalculated = (currentState.totalCandiesEarned / prestigeGoal).toLong()
        val pointsToClaim = totalPrestigeCalculated - currentState.prestigePoints
        if (pointsToClaim > 0) {
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
                    mittLevel = 0,
                    mittsTotalEarned = 0.0,
                    clickTotalEarned = 0.0,
                    droneTotalEarned = 0.0,
                    gingerbreadTotalEarned = 0.0,
                    cottonTotalEarned = 0.0,
                    volcanoTotalEarned = 0.0,
                    earthTotalEarned = 0.0,
                    galaxyTotalEarned = 0.0,
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
        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()

        var mittClickMult = 1.0
        if (purchasedSet.contains("mitt_up_1")) mittClickMult *= 2.0
        if (purchasedSet.contains("mitt_up_2")) mittClickMult *= 2.0
        if (purchasedSet.contains("mitt_up_3")) mittClickMult *= 2.0

        val numNonMitts = state.clickPowerLevel + state.candyDroneLevel + state.gingerbreadLevel + state.cottonCloudLevel + state.chocolateVolcanoLevel + state.sugarEarthLevel + state.lollipopGalaxyLevel
        var additionalClickPower = 0.0
        if (purchasedSet.contains("mitt_up_4")) {
            val qMultiplier = 1.0 * 
                (if (purchasedSet.contains("mitt_up_5")) 5.0 else 1.0) * 
                (if (purchasedSet.contains("mitt_up_6")) 10.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_7")) 15.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_8")) 20.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_9")) 20.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_10")) 25.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_11")) 25.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_12")) 25.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_13")) 25.0 else 1.0) *
                (if (purchasedSet.contains("mitt_up_14")) 100.0 else 1.0)
            additionalClickPower = 0.1 * numNonMitts * qMultiplier
        }

        val basePower = (1.0 + additionalClickPower) * mittClickMult
        return basePower * multiplier
    }

    fun getBuildingMultiplier(state: GameState, buildingId: String): Double {
        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()
        var boughtCount = 0
        if (buildingId == "click") {
            val clickUpgrades = listOf(
                "click_up_1_concrete", "click_up_1_bolts", "click_up_10_steel", "click_up_25_cables",
                "click_up_50_paint", "click_up_100_elevator", "click_up_150_glass", "click_up_200_rivets",
                "click_up_250_roof", "click_up_300_walls", "click_up_350_tiles", "click_up_400_carpet",
                "click_up_450_elevator", "click_up_500_stairs"
            )
            for (id in clickUpgrades) {
                if (purchasedSet.contains(id)) {
                    boughtCount++
                }
            }
        } else {
            val intervals = listOf(25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500)
            for (interval in intervals) {
                val upgradeId = "${buildingId}_$interval"
                if (purchasedSet.contains(upgradeId)) {
                    boughtCount++
                }
            }
        }
        return 2.0.pow(boughtCount.toDouble())
    }

    fun getTowerCps(state: GameState, towerId: String): Double {
        val multiplier = (1.0 + state.prestigePoints * 0.01)
        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()

        return when (towerId) {
            "mitt" -> {
                var mittCpsMult = 1.0
                if (purchasedSet.contains("mitt_up_1")) mittCpsMult *= 2.0
                if (purchasedSet.contains("mitt_up_2")) mittCpsMult *= 2.0
                if (purchasedSet.contains("mitt_up_3")) mittCpsMult *= 2.0

                val numNonMitts = state.clickPowerLevel + state.candyDroneLevel + state.gingerbreadLevel + state.cottonCloudLevel + state.chocolateVolcanoLevel + state.sugarEarthLevel + state.lollipopGalaxyLevel
                var additionalCpsPower = 0.0
                if (purchasedSet.contains("mitt_up_4")) {
                    val qMultiplier = 1.0 * 
                        (if (purchasedSet.contains("mitt_up_5")) 5.0 else 1.0) * 
                        (if (purchasedSet.contains("mitt_up_6")) 10.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_7")) 15.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_8")) 20.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_9")) 20.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_10")) 25.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_11")) 25.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_12")) 25.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_13")) 25.0 else 1.0) *
                        (if (purchasedSet.contains("mitt_up_14")) 100.0 else 1.0)
                    additionalCpsPower = 0.1 * numNonMitts * qMultiplier
                }

                val singleMittCps = (0.1 + additionalCpsPower) * mittCpsMult
                (state.mittLevel * singleMittCps) * multiplier
            }
            "click" -> {
                val clickMult = getBuildingMultiplier(state, "click")
                (state.clickPowerLevel * 1.0 * clickMult) * multiplier
            }
            "drone" -> {
                val droneMult = getBuildingMultiplier(state, "drone")
                (state.candyDroneLevel * 12.0 * droneMult) * multiplier
            }
            "gingerbread" -> {
                val gingerbreadMult = getBuildingMultiplier(state, "gingerbread")
                (state.gingerbreadLevel * 56.0 * gingerbreadMult) * multiplier
            }
            "cotton" -> {
                val cottonMult = getBuildingMultiplier(state, "cotton")
                (state.cottonCloudLevel * 300.0 * cottonMult) * multiplier
            }
            "volcano" -> {
                val volcanoMult = getBuildingMultiplier(state, "volcano")
                (state.chocolateVolcanoLevel * 1600.0 * volcanoMult) * multiplier
            }
            "earth" -> {
                val earthMult = getBuildingMultiplier(state, "earth")
                (state.sugarEarthLevel * 8200.0 * earthMult) * multiplier
            }
            "galaxy" -> {
                val galaxyMult = getBuildingMultiplier(state, "galaxy")
                (state.lollipopGalaxyLevel * 50000.0 * galaxyMult) * multiplier
            }
            else -> 0.0
        }
    }

    fun getCps(state: GameState): Double {
        return getTowerCps(state, "mitt") +
               getTowerCps(state, "click") +
               getTowerCps(state, "drone") +
               getTowerCps(state, "gingerbread") +
               getTowerCps(state, "cotton") +
               getTowerCps(state, "volcano") +
               getTowerCps(state, "earth") +
               getTowerCps(state, "galaxy")
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
        val standardList = mutableListOf(
            UpgradeItem(
                id = "mitts",
                name = "Oven Mitts",
                description = "Thick heat-resistant mitts. Generates +0.1 candies/sec and rotates around the lollipop!",
                baseCost = 10.0,
                costMultiplier = 1.15,
                cpsIncrease = 0.1,
                clickPowerIncrease = 0.0,
                level = state.mittLevel,
                cost = calculateCost(10.0, state.mittLevel)
            ),
            UpgradeItem(
                id = "click",
                name = "Sugar Tower",
                description = "A compact spun-sugar tower. Generates +1.0 candies/sec.",
                baseCost = 15.0,
                costMultiplier = 1.15,
                cpsIncrease = 1.0,
                clickPowerIncrease = 0.0,
                level = state.clickPowerLevel,
                cost = calculateCost(15.0, state.clickPowerLevel)
            ),
            UpgradeItem(
                id = "drone",
                name = "Candy Drone",
                description = "Automated mechanical bees. Generates +12.0 candies/sec.",
                baseCost = 100.0,
                costMultiplier = 1.15,
                cpsIncrease = 12.0,
                clickPowerIncrease = 0.0,
                level = state.candyDroneLevel,
                cost = calculateCost(100.0, state.candyDroneLevel)
            ),
            UpgradeItem(
                id = "gingerbread",
                name = "Gingerbread Mill",
                description = "Grinds delicious cookie-crust ingredients. Generates +56.0/sec.",
                baseCost = 1100.0,
                costMultiplier = 1.15,
                cpsIncrease = 56.0,
                clickPowerIncrease = 0.0,
                level = state.gingerbreadLevel,
                cost = calculateCost(1100.0, state.gingerbreadLevel)
            ),
            UpgradeItem(
                id = "cotton",
                name = "Cotton Cloud",
                description = "Spun sugar vaporizers drifting in the sky. Generates +300.0/sec.",
                baseCost = 12000.0,
                costMultiplier = 1.15,
                cpsIncrease = 300.0,
                clickPowerIncrease = 0.0,
                level = state.cottonCloudLevel,
                cost = calculateCost(12000.0, state.cottonCloudLevel)
            ),
            UpgradeItem(
                id = "volcano",
                name = "Chocolate Volcano",
                description = "Erupts molten lava fudge. Generates +1,600.0 candies/sec.",
                baseCost = 130000.0,
                costMultiplier = 1.15,
                cpsIncrease = 1600.0,
                clickPowerIncrease = 0.0,
                level = state.chocolateVolcanoLevel,
                cost = calculateCost(130000.0, state.chocolateVolcanoLevel)
            ),
            UpgradeItem(
                id = "earth",
                name = "Sugar Earth",
                description = "A massive chocolate-crust globe with sweet icing soils. Generates +8,200.0/sec.",
                baseCost = 450000.0,
                costMultiplier = 1.15,
                cpsIncrease = 8200.0,
                clickPowerIncrease = 0.0,
                level = state.sugarEarthLevel,
                cost = calculateCost(450000.0, state.sugarEarthLevel)
            ),
            UpgradeItem(
                id = "galaxy",
                name = "Lollipop Galaxy",
                description = "A swirling cluster of sugar solar systems. Generates +50,000.0/sec.",
                baseCost = 1400000.0,
                costMultiplier = 1.15,
                cpsIncrease = 50000.0,
                clickPowerIncrease = 0.0,
                level = state.lollipopGalaxyLevel,
                cost = calculateCost(1400000.0, state.lollipopGalaxyLevel)
            )
        )






        val purchasedSet = state.purchasedUpgrades.split(",").filter { it.isNotEmpty() }.toSet()

        if (state.mittLevel >= 1 && !purchasedSet.contains("mitt_up_1")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_1",
                    name = "First Mitt",
                    description = "Twice as efficient for clicking power and mitts auto clicking power.",
                    baseCost = 100.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 100.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 1 && !purchasedSet.contains("mitt_up_2")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_2",
                    name = "First Mitt Twin",
                    description = "Twice as efficient for clicking power and mitts auto clicking power.",
                    baseCost = 500.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 500.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 10 && !purchasedSet.contains("mitt_up_3")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_3",
                    name = "Tenamitts",
                    description = "Twice as efficient for clicking power and mitts auto clicking power.",
                    baseCost = 10000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 10000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 25 && !purchasedSet.contains("mitt_up_4")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_4",
                    name = "Quarter Mitts",
                    description = "Tapping and mitts gain +0.1 candies/sec for each non-mitt tower owned.",
                    baseCost = 100000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 100000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 50 && !purchasedSet.contains("mitt_up_5")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_5",
                    name = "Half Mitts",
                    description = "Multiplies the gain from Quarter Mitts by 5.",
                    baseCost = 1000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 100 && !purchasedSet.contains("mitt_up_6")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_6",
                    name = "Century Mitts",
                    description = "Multiplies the gain from Half Mitts by 10.",
                    baseCost = 10000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 10000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 150 && !purchasedSet.contains("mitt_up_7")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_7",
                    name = "Fire Proof Mitts",
                    description = "Multiplies the gain from Century Mitts by 15.",
                    baseCost = 100000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 100000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 200 && !purchasedSet.contains("mitt_up_8")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_8",
                    name = "Titanium Mitts",
                    description = "Multiplies the gain from Fire Proof Mitts by 20.",
                    baseCost = 1000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 250 && !purchasedSet.contains("mitt_up_9")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_9",
                    name = "Space Mitts",
                    description = "Multiplies the gain from Titanium Mitts by 20.",
                    baseCost = 50000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 50000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 300 && !purchasedSet.contains("mitt_up_10")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_10",
                    name = "Black Hole Mitts",
                    description = "Multiplies the gain from Space Mitts by 25.",
                    baseCost = 1000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 350 && !purchasedSet.contains("mitt_up_11")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_11",
                    name = "Quasar Mitts",
                    description = "Multiplies the gain from Black Hole Mitts by 25.",
                    baseCost = 50000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 50000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 400 && !purchasedSet.contains("mitt_up_12")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_12",
                    name = "Core Mitts",
                    description = "Multiplies the gain from Quasar Mitts by 25.",
                    baseCost = 1000000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 450 && !purchasedSet.contains("mitt_up_13")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_13",
                    name = "Nebula Mitts",
                    description = "Multiplies the gain from Core Mitts by 25.",
                    baseCost = 50000000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 50000000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.mittLevel >= 500 && !purchasedSet.contains("mitt_up_14")) {
            standardList.add(
                UpgradeItem(
                    id = "mitt_up_14",
                    name = "Finale Mitts",
                    description = "Multiplies the gain from Nebula Mitts by 100.",
                    baseCost = 1000000000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 1000.0 && !purchasedSet.contains("spatula_wooden")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_wooden",
                    name = "Wooden Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 50000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 50000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 100000.0 && !purchasedSet.contains("spatula_plastic")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_plastic",
                    name = "Plastic Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 5000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 5000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 10000000.0 && !purchasedSet.contains("spatula_silicon")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_silicon",
                    name = "Silicon Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 500000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 500000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 1000000000.0 && !purchasedSet.contains("spatula_steel")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_steel",
                    name = "Steel Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 5000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 5000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 100000000000.0 && !purchasedSet.contains("spatula_aluminum")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_aluminum",
                    name = "Aluminum Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 500000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 500000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 1000000000000.0 && !purchasedSet.contains("spatula_titanium")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_titanium",
                    name = "Titanium Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 10000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 10000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 100000000000000.0 && !purchasedSet.contains("spatula_iridium")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_iridium",
                    name = "Iridium Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 1000000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 1000000000000000.0,
                    isBuilding = false
                )
            )
        }

        if (state.totalClickCandiesEarned >= 10000000000000000.0 && !purchasedSet.contains("spatula_gold")) {
            standardList.add(
                UpgradeItem(
                    id = "spatula_gold",
                    name = "Gold Spatula",
                    description = "Tapping gains +1% of your candies per second when upgrade is bought.",
                    baseCost = 100000000000000000.0,
                    costMultiplier = 1.0,
                    cpsIncrease = 0.0,
                    clickPowerIncrease = 0.0,
                    level = 0,
                    cost = 100000000000000000.0,
                    isBuilding = false
                )
            )
        }

        val intervalList = mutableListOf<UpgradeItem>()
        val intervals = listOf(25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500)
        val stats = listOf(
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

        val customClickUpgrades = mutableListOf<UpgradeItem>()
        val clickLevel = state.clickPowerLevel
        val clickUpDefs = listOf(
            Triple("click_up_1_concrete", "Sweet Concrete", Pair(1, 1000.0)),
            Triple("click_up_1_bolts", "Sweet Bolts", Pair(1, 5000.0)),
            Triple("click_up_10_steel", "Sweet Steel", Pair(10, 20000.0)),
            Triple("click_up_25_cables", "Sweet Cables", Pair(25, 150000.0)),
            Triple("click_up_50_paint", "Sweet Paint", Pair(50, 500000.0)),
            Triple("click_up_100_elevator", "Sweet Elevator", Pair(100, 1000000.0)),
            Triple("click_up_150_glass", "Sweet Glass", Pair(150, 10000000.0)),
            Triple("click_up_200_rivets", "Sweet Rivets", Pair(200, 100000000.0)),
            Triple("click_up_250_roof", "Sweet Roof", Pair(250, 500000000.0)),
            Triple("click_up_300_walls", "Sweet Walls", Pair(300, 2000000000.0)),
            Triple("click_up_350_tiles", "Sweet Tiles", Pair(350, 50000000000.0)),
            Triple("click_up_400_carpet", "Sweet Carpet", Pair(400, 250000000000.0)),
            Triple("click_up_450_elevator", "Sweet Elevator", Pair(450, 750000000000.0)),
            Triple("click_up_500_stairs", "Sweet Stairs", Pair(500, 50000000000000.0))
        )
        for (def in clickUpDefs) {
            val upId = def.first
            val upName = def.second
            val upTargetLvl = def.third.first
            val upCost = def.third.second
            if (!purchasedSet.contains(upId) && clickLevel >= upTargetLvl) {
                customClickUpgrades.add(
                    UpgradeItem(
                        id = upId,
                        name = upName,
                        description = "Sugar Tower upgrade twice as efficient (+100% CPS)!",
                        baseCost = upCost,
                        costMultiplier = 1.0,
                        cpsIncrease = 0.0,
                        clickPowerIncrease = 0.0,
                        level = 1,
                        cost = upCost,
                        isBuilding = false
                    )
                )
            }
        }

        return standardList + intervalList + customClickUpgrades
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

    fun getMaxAffordableCount(base: Double, currentLevel: Int, candies: Double, multiplier: Double = 1.15): Int {
        var count = 0
        var totalCost = 0.0
        while (true) {
            val nextCost = base * multiplier.pow((currentLevel + count).toDouble())
            if (totalCost + nextCost <= candies) {
                totalCost += nextCost
                count++
            } else {
                break
            }
        }
        return count
    }

    fun formatValue(value: Double): String {
        return when {
            value >= 1_000_000_000_000_000_000.0 -> String.format(java.util.Locale.US, "%.2f Qi", value / 1_000_000_000_000_000_000.0)
            value >= 1_000_000_000_000_000.0 -> String.format(java.util.Locale.US, "%.2f Qa", value / 1_000_000_000_000_000.0)
            value >= 1_000_000_000_000.0 -> String.format(java.util.Locale.US, "%.2f T", value / 1_000_000_000_000.0)
            value >= 1_000_000_000.0 -> String.format(java.util.Locale.US, "%.2f B", value / 1_000_000_000.0)
            value >= 1_000_000.0 -> String.format(java.util.Locale.US, "%.2f M", value / 1_000_000.0)
            value >= 1_000.0 -> String.format(java.util.Locale.US, "%.1f K", value / 1_000.0)
            else -> String.format(java.util.Locale.US, "%.1f", value)
        }
    }

    override fun onCleared() {
        gameLoopJob?.cancel()
        super.onCleared()
    }
}
