package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Floppa Apps", appName)
  }

  @Test
  fun `verify store catalog is clean of fake apps and floppa security sha256 works`() {
    val sampleApps = com.example.data.StoreCatalog.sampleApps
    // All fake apps removed; store starts clean
    assertTrue(sampleApps.isEmpty())

    val hash = com.example.security.FloppaSecurityScanner.calculateSha256("floppa_test_payload".toByteArray())
    assertEquals(64, hash.length)
  }
}
