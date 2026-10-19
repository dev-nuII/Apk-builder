package com.porter.tvremote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroStoreTest {
    @Test fun acceptsNavigationAndWaitSteps() {
        assertTrue(MacroStore.isValidStep("up"))
        assertTrue(MacroStore.isValidStep("home"))
        assertTrue(MacroStore.isValidStep("wait:5"))
    }

    @Test fun rejectsUnknownOrOutOfRangeSteps() {
        assertFalse(MacroStore.isValidStep("reboot"))
        assertFalse(MacroStore.isValidStep("wait:0"))
        assertFalse(MacroStore.isValidStep("wait:61"))
        assertFalse(MacroStore.isValidStep("wait:abc"))
    }

    @Test fun rejectsEmptyMacros() {
        assertFalse(MacroStore.isValid(Macro("x", emptyList())))
        assertFalse(MacroStore.isValid(Macro(" ", listOf("up"))))
        assertTrue(MacroStore.isValid(Macro("ok", listOf("home", "wait:2", "down"))))
    }
}
