package dev.splitkeyboard

data class Key(val label: String, val usage: Int, val weight: Float = 1f)

class KeyState {
    private val held = mutableMapOf<Int, Int>()
    fun down(pointer: Int, usage: Int) { held[pointer] = usage }
    fun up(pointer: Int) { held.remove(pointer) }
    fun clear() { held.clear() }
    fun active(usage: Int) = usage in held.values
    fun report(): ByteArray {
        val report = ByteArray(8)
        held.values.filter { it in 224..231 }.distinct().forEach {
            report[0] = (report[0].toInt() or (1 shl (it - 224))).toByte()
        }
        val keys = held.values.filter { it in 1..223 }.distinct()
        if (keys.size > 6) for (index in 2..7) report[index] = 1
        else keys.forEachIndexed { index, usage -> report[index + 2] = usage.toByte() }
        return report
    }
}

object Layouts {
    private fun letters(text: String) = text.map { Key(it.toString(), it.code - 'A'.code + 4) }
    fun rows(left: Boolean): List<List<Key>> {
        val modifiers = if (left)
            listOf(Key("Ctrl", 224), Key("⌥ Opt", 226), Key("⌘ Cmd", 227), Key("空格", 44, 1.5f))
        else listOf(Key("空格", 44, 1.5f), Key("⌘ Cmd", 227), Key("⌥ Opt", 226), Key("Ctrl", 224))
        return if (left) listOf(
            listOf(Key("Tab", 43)) + letters("QWERT"),
            listOf(Key("Caps", 57)) + letters("ASDFG"),
            listOf(Key("⇧", 225)) + letters("ZXCVB"),
            modifiers
        ) else listOf(
            letters("YUIOP") + listOf(Key("⌫", 42, 1.5f)),
            letters("HJKL") + listOf(Key(";", 51), Key("↵", 40)),
            letters("BNM") + listOf(Key(",", 54, .7f), Key(".", 55, .7f), Key("/", 56, .7f), Key("⇧", 229)),
            modifiers
        )
    }
}
