package com.gokul.personalvault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LoginScreenSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreenIsDisplayed() {
        composeRule.onNodeWithText("PERSONAL VAULT").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun accountCreationModeAndEmptyFieldValidationWork() {
        composeRule.onNodeWithText("New to Personal Vault? Create account")
            .performClick()

        composeRule.onNodeWithText("Create your account").assertIsDisplayed()
        composeRule.onNodeWithText("Create Account").performClick()

        composeRule.onNodeWithText("Please enter your email and password.")
            .assertIsDisplayed()
    }
}
