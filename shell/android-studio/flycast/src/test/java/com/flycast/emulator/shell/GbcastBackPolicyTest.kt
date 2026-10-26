package com.flycast.emulator.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class GbcastBackPolicyTest {
    @Test
    fun nativeSubmenusReturnToNativeSettings() {
        listOf(9, 6, 7).forEach { state ->
            assertEquals(GbcastBackPolicy.Action.OPEN_NATIVE_SETTINGS, GbcastBackPolicy.decide(state, false))
        }
    }

    @Test
    fun commandAndNativePagesReturnToPauseMenu() {
        listOf(1, 3, 8, 11, 12).forEach { state ->
            assertEquals(GbcastBackPolicy.Action.RETURN_TO_PAUSE_MENU, GbcastBackPolicy.decide(state, true))
        }
    }

    @Test
    fun pauseStateDelegatesResumeOrShowDecisionToVisibilityAwareHandler() {
        assertEquals(GbcastBackPolicy.Action.RESUME_IF_VISIBLE_OR_SHOW, GbcastBackPolicy.decide(2, true))
        assertEquals(GbcastBackPolicy.Action.RESUME_IF_VISIBLE_OR_SHOW, GbcastBackPolicy.decide(2, false))
    }

    @Test
    fun mainFinishesAndUnknownStatesUseToggleFallback() {
        assertEquals(GbcastBackPolicy.Action.FINISH_ACTIVITY, GbcastBackPolicy.decide(4, false))
        assertEquals(GbcastBackPolicy.Action.TOGGLE_PAUSE_AND_SHOW, GbcastBackPolicy.decide(0, false))
        assertEquals(GbcastBackPolicy.Action.TOGGLE_PAUSE_AND_SHOW, GbcastBackPolicy.decide(99, true))
    }
}
