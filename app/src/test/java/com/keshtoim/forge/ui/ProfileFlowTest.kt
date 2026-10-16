package com.keshtoim.forge.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun heightAndWeightGiveTheBmi() {
        rule.onNodeWithText("Female", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Let's go", useUnmergedTree = true).performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("New workout"), timeoutMillis = 5_000)
        rule.onNodeWithText("More").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Profile", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Profile", useUnmergedTree = true).performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithText("Height", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Height", useUnmergedTree = true).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("165")
        rule.onNodeWithText("Save").performClick()
        rule.onAllNodesWithText("Weight", useUnmergedTree = true)[0].performClick()
        rule.onNode(hasSetTextAction()).performTextInput("62,4")
        rule.onNodeWithText("Save").performClick()

        rule.waitUntilAtLeastOneExists(hasText("22.9"), timeoutMillis = 5_000)
        rule.onNodeWithText("Normal").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("56.9 kg"))
    }
}
