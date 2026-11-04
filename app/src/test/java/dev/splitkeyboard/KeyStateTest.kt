package dev.splitkeyboard

import org.junit.Assert.*
import org.junit.Test

class KeyStateTest {
    @Test fun commandCombinationReleasesIndependently() {
        val state = KeyState()
        state.down(10, 227)
        state.down(11, 6)
        assertEquals(8, state.report()[0].toInt())
        assertEquals(6, state.report()[2].toInt())
        state.up(11)
        assertEquals(8, state.report()[0].toInt())
        assertEquals(0, state.report()[2].toInt())
        state.up(10)
        assertArrayEquals(ByteArray(8), state.report())
    }
    @Test fun twoPointersOnSameKeyNeedBothReleased() {
        val state = KeyState()
        state.down(1, 44); state.down(2, 44)
        state.up(1)
        assertEquals(44, state.report()[2].toInt())
        state.up(2)
        assertArrayEquals(ByteArray(8), state.report())
    }
    @Test fun overflowReportsRolloverAndRecovers() {
        val state = KeyState()
        repeat(7) { state.down(it, it + 4) }
        assertTrue(state.report().drop(2).all { it.toInt() == 1 })
        state.up(6)
        assertEquals(listOf(4, 5, 6, 7, 8, 9), state.report().drop(2).map { it.toInt() })
        state.clear()
        assertArrayEquals(ByteArray(8), state.report())
    }
    @Test fun bothLayoutsIncludeRequiredKeysAndTogetherCoverAlphabet() {
        val left = Layouts.rows(true).flatten().map { it.usage }
        val right = Layouts.rows(false).flatten().map { it.usage }
        for (required in listOf(5, 44, 224, 226, 227)) {
            assertTrue(required in left); assertTrue(required in right)
        }
        assertTrue((4..29).all { it in left + right })
    }
}
