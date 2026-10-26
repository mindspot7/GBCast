package com.flycast.emulator.shell

/**
 * Pure decision policy for Android Back while the Flycast game UI is active.
 * State IDs mirror GbcastGameUi's native GUI state constants.
 *
 * This only chooses the UI action; native pause/resume remains owned by GbcastGameUi.
 */
internal object GbcastBackPolicy {
    enum class Action {
        OPEN_NATIVE_SETTINGS,
        RETURN_TO_PAUSE_MENU,
        RESUME_IF_VISIBLE_OR_SHOW,
        FINISH_ACTIVITY,
        TOGGLE_PAUSE_AND_SHOW,
    }

    fun decide(state: Int): Action = when (state) {
        9, 6, 7 -> Action.OPEN_NATIVE_SETTINGS // LOADING, VJOY, VJOY_CMD
        1, 3, 8, 11, 12 -> Action.RETURN_TO_PAUSE_MENU // COMMANDS, SETTINGS, DISK, CHEATS, ACHIEVEMENTS
        2 -> Action.RESUME_IF_VISIBLE_OR_SHOW // PAUSE
        4 -> Action.FINISH_ACTIVITY // MAIN
        else -> Action.TOGGLE_PAUSE_AND_SHOW
    }
}
