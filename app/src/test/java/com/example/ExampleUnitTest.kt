package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import java.awt.Color

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun analyzeLollipop() {
    val file = File("src/main/res/drawable/img_lollipop.jpg")
    assertTrue("Lollipop file exists", file.exists())
    val img = ImageIO.read(file)
    println("Lollipop dims: ${img.width}x${img.height}")
    
    var minX = img.width
    var maxX = 0
    var minY = img.height
    var maxY = 0
    var remainingNonWhiteTotal = 0
    
    for (y in 0 until img.height) {
        for (x in 0 until img.width) {
            val rgb = img.getRGB(x, y)
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            
            var isTransparent = false
            
            // Clear everything completely below the stick tip
            if (y >= 961) {
                isTransparent = true
            } else if (y > 600) {
                // Below the candy swirl, only make the background transparent
                // Since the background here is off-white/light-grey, relax the thresh
                if (r > 180 && g > 180 && b > 180) {
                    isTransparent = true
                }
            } else {
                // For the candy swirl region, keep the standard safe threshold
                if (r > 240 && g > 240 && b > 240) {
                    isTransparent = true
                }
            }
            
            if (!isTransparent) {
                // If the pixel is not transparent, check if it's "colored"
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                remainingNonWhiteTotal++
            }
        }
    }
    
    val resultStr = "Remaining colored bounds after simulation: X:[$minX to $maxX], Y:[$minY to $maxY] | Non-transparent: $remainingNonWhiteTotal"
    File("output.txt").writeText(resultStr)
    println(resultStr)
    assertTrue("Remaining non-white elements exist", remainingNonWhiteTotal > 0)
  }
}

