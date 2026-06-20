package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createViewModel(
    candiesVal: Double = 250000.0,
    prestigePointsVal: Long = 4,
    clickPowerLvl: Int = 5,
    candyDroneLvl: Int = 3,
    gingerbreadLvl: Int = 2,
    cottonCloudLvl: Int = 1,
    chocolateVolcanoLvl: Int = 0
  ): com.example.viewmodel.GameViewModel {
    val mockDao = object : com.example.data.GameStateDao {
      override fun getGameStateFlow(): kotlinx.coroutines.flow.Flow<com.example.data.GameState?> {
        return kotlinx.coroutines.flow.flowOf(
          com.example.data.GameState(
            candies = candiesVal,
            totalCandiesEarned = candiesVal * 2,
            prestigePoints = prestigePointsVal,
            clickPowerLevel = clickPowerLvl,
            candyDroneLevel = candyDroneLvl,
            gingerbreadLevel = gingerbreadLvl,
            cottonCloudLevel = cottonCloudLvl,
            chocolateVolcanoLevel = chocolateVolcanoLvl
          )
        )
      }
      override suspend fun getGameState(): com.example.data.GameState? {
        return com.example.data.GameState(
          candies = candiesVal,
          totalCandiesEarned = candiesVal * 2,
          prestigePoints = prestigePointsVal,
          clickPowerLevel = clickPowerLvl,
          candyDroneLevel = candyDroneLvl,
          gingerbreadLevel = gingerbreadLvl,
          cottonCloudLevel = cottonCloudLvl,
          chocolateVolcanoLevel = chocolateVolcanoLvl
        )
      }
      override suspend fun saveGameState(state: com.example.data.GameState) {}
    }
    val repository = com.example.data.GameRepository(mockDao)
    return com.example.viewmodel.GameViewModel(repository)
  }

  @Test
  fun testGameTab() {
    val viewModel = createViewModel()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        CandyClickerApp(viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/1_game_tab.png")
  }

  @Test
  fun testCandyStoreBuildingsTab() {
    val viewModel = createViewModel()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        CandyClickerApp(viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Candy Store").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/2_candy_store_buildings.png")
  }

  @Test
  fun testCandyStoreUpgradesTab() {
    val viewModel = createViewModel()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        CandyClickerApp(viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Candy Store").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Upgrades").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/3_candy_store_upgrades.png")
  }

  @Test
  fun testSecretSugarsTab() {
    val viewModel = createViewModel()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        CandyClickerApp(viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Secret Sugars").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/4_secret_sugars.png")
  }

  @Test
  fun testSettingsTab() {
    val viewModel = createViewModel()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        CandyClickerApp(viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/5_settings.png")
  }
}
