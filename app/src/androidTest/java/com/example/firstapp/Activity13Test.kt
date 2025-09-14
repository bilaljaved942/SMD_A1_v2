package com.example.firstapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivity13Test {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity13::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testNavigationToMainActivity15_whenEditProfileClicked() {
        // Perform click on edit profile (text2_5)
        onView(withId(R.id.text2_5)).perform(click())

        // Get all captured intents
        val capturedIntents = Intents.getIntents()

        // Verify that at least one of them launches MainActivity15
        assertThat(
            "MainActivity15 should have been launched",
            capturedIntents,
            hasItem(hasComponent(MainActivity15::class.java.name))
        )
    }
}
