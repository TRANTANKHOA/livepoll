package com.example

import com.example.util.GoogleAuthHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthHelperUnitTest {

    @Test
    fun testRealClientIdsPass() {
        assertTrue(GoogleAuthHelper.isConfiguredClientId("1234567890-abcdef.apps.googleusercontent.com"))
        assertTrue(
            GoogleAuthHelper.isConfiguredClientId(
                "407249282310-fibci5psrbp5dgf5tn9m2r7v4v7g1f47.apps.googleusercontent.com"
            )
        )
        // Surrounding whitespace is tolerated (trimmed before matching)
        assertTrue(
            GoogleAuthHelper.isConfiguredClientId(" 1234567890-abcdef.apps.googleusercontent.com ")
        )
    }

    @Test
    fun testPlaceholderSentinelsFail() {
        // Current and historical placeholders must both fail, so renames can't
        // silently disable the misconfiguration warning.
        assertFalse(GoogleAuthHelper.isConfiguredClientId("livepulse-firebase-auth.apps.googleusercontent.com"))
        assertFalse(GoogleAuthHelper.isConfiguredClientId("pulsepoll-firebase-auth.apps.googleusercontent.com"))
    }

    @Test
    fun testMalformedOrMissingValuesFail() {
        assertFalse(GoogleAuthHelper.isConfiguredClientId(""))
        assertFalse(GoogleAuthHelper.isConfiguredClientId("   "))
        assertFalse(GoogleAuthHelper.isConfiguredClientId("not-a-client-id"))
        // Missing the numeric-prefix hash segment
        assertFalse(GoogleAuthHelper.isConfiguredClientId("1234567890.apps.googleusercontent.com"))
        // Domain must terminate the ID
        assertFalse(GoogleAuthHelper.isConfiguredClientId("1234567890-abc.apps.googleusercontent.com.evil.com"))
    }
}
