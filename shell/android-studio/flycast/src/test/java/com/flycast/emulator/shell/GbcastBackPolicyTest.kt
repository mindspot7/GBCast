package com.flycast.emulator.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class GbcastBackPolicyTest {
    @Test
    fun nativeSubmenusReturnToNativeSettings() {
        listOf(GbcastGameUi.LOADING, GbcastGameUi.VJOY, GbcastGameUi.VJOY_CMD).forEach { state ->
            assertEquals(GbcastBackPolicy.Action.OPEN_NATIVE_SETTINGS, GbcastBackPolicy.decide(state))
        }
    }

    @Test
    fun commandAndNativePagesReturnToPauseMenu() {
        listOf(
            GbcastGameUi.COMMANDS,
            GbcastGameUi.SETTINGS,
            GbcastGameUi.DISK,
            GbcastGameUi.CHEATS,
            GbcastGameUi.ACHIEVEMENTS,
        ).forEach { state ->
            assertEquals(GbcastBackPolicy.Action.RETURN_TO_PAUSE_MENU, GbcastBackPolicy.decide(state))
        }
    }

    @Test
    fun pauseStateDelegatesResumeOrShowDecisionToVisibilityAwareHandler() {
        assertEquals(
            GbcastBackPolicy.Action.RESUME_IF_VISIBLE_OR_SHOW,
            GbcastBackPolicy.decide(GbcastGameUi.PAUSE),
        )
    }

    @Test
    fun mainFinishesAndUnknownStatesUseToggleFallback() {
        assertEquals(GbcastBackPolicy.Action.FINISH_ACTIVITY, GbcastBackPolicy.decide(GbcastGameUi.MAIN))
        assertEquals(GbcastBackPolicy.Action.TOGGLE_PAUSE_AND_SHOW, GbcastBackPolicy.decide(GbcastGameUi.CLOSED))
        assertEquals(GbcastBackPolicy.Action.TOGGLE_PAUSE_AND_SHOW, GbcastBackPolicy.decide(99))
    }
}
