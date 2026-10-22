package hr.bertlab.fluteassistant.core.log

import java.util.Locale

/**
 * One structured log line: a dotted event name followed by `key=value` fields in the order
 * given, e.g. `tuner.frame f=440.12 p=0.80 level=0.40`. Floating-point values get two
 * decimals, null is `none`, enums their name, and text containing whitespace is quoted, so a
 * line can always be split on spaces by a script. Pure Kotlin, unit-tested on the JVM.
 */
object LogLine {
    fun format(event: String, vararg fields: Pair<String, Any?>): String {
        if (fields.isEmpty()) return event
        val line = StringBuilder(event)
        for ((key, value) in fields) line.append(' ').append(key).append('=').append(render(value))
        return line.toString()
    }

    private fun render(value: Any?): String = when (value) {
        null -> "none"
        is Double -> String.format(Locale.ROOT, "%.2f", value)
        is Float -> String.format(Locale.ROOT, "%.2f", value)
        is Enum<*> -> value.name
        is String -> if (value.any { it.isWhitespace() }) "\"$value\"" else value
        else -> value.toString()
    }
}
