package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NoteVault", appName)
  }

  @Test
  fun `test pin hashing and verification`() {
    val salt = "test_salt_12345"
    val pin = "1234"
    val hash = SecurityManager.hashPin(pin, salt)
    assertNotNull(hash)
    val hash2 = SecurityManager.hashPin(pin, salt)
    assertEquals(hash, hash2)
  }
}

