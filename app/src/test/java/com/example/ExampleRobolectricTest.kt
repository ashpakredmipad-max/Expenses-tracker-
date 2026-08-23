package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.CurrencyUtils
import org.junit.Assert.assertEquals
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
        assertEquals("Expense Tracker", appName)
    }

    @Test
    fun `test indian currency formatting`() {
        assertEquals("₹25,500.00", CurrencyUtils.formatPaise(2550000L))
        assertEquals("₹1,25,000.00", CurrencyUtils.formatPaise(12500000L))
        assertEquals("₹10,00,000.00", CurrencyUtils.formatPaise(100000000L))
    }
}
