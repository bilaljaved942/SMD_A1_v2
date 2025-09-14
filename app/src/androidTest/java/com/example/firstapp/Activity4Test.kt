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
class MainActivity4Test {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity4::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testNavigationToMainActivity5_whenLoginBtnClicked() {
        // Perform click
        onView(withId(R.id.loginBtn)).perform(click())

        // Get all captured intents
        val capturedIntents = Intents.getIntents()

        // Assert that at least one of them launches MainActivity5
        assertThat(
            "MainActivity5 should have been launched",
            capturedIntents,
            hasItem(hasComponent(MainActivity5::class.java.name))
        )
    }
}
