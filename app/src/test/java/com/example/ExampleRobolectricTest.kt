package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OTC Signal Pro", appName)
  }

  @Test
  fun `verify price action engine generates signals`() {
    val candles = listOf(
      com.example.data.entity.CandleEntity(
        pairName = "EUR/USD OTC",
        timestamp = System.currentTimeMillis(),
        open = 1.0850,
        high = 1.0870,
        low = 1.0848,
        close = 1.0868,
        isBullish = true,
        bodySize = 0.0018,
        upperWick = 0.0002,
        lowerWick = 0.0002
      )
    )
    val signal = com.example.engine.PriceActionEngine.analyze(candles)
    org.junit.Assert.assertTrue(signal.action == "CALL" || signal.action == "PUT")
    org.junit.Assert.assertTrue(signal.confidence in 70..100)
  }
}
