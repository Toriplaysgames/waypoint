package com.waypoint.app

/**
 * In Hebrew, map labels prefer OpenStreetMap's `name:he`, then fall back to each style's
 * own Latin and local-script expression. Road numbers (`ref`) and non-name labels are untouched.
 *
 * Works on the style text directly so numbers, ordering and formatting stay exactly as authored.
 */
internal object MapLabels {
    private const val KEY = "\"text-field\""

    fun localize(style: String, hebrew: Boolean = AppLanguage.isHebrew): String =
        if (hebrew) preferName(style, "name:he") else style

    internal fun preferName(style: String, field: String): String {
        val out = StringBuilder(style.length + 1024)
        var from = 0
        while (true) {
            val key = style.indexOf(KEY, from)
            if (key < 0) break
            var i = key + KEY.length
            while (i < style.length && style[i].isWhitespace()) i++
            if (i >= style.length || style[i] != ':') { out.append(style, from, i); from = i; continue }
            i++
            while (i < style.length && style[i].isWhitespace()) i++
            val end = valueEnd(style, i)
            out.append(style, from, i)
            val value = style.substring(i, end)
            // Only expressions that read a name; plain strings and ref-only labels stay as authored.
            if (value.startsWith("[") && value.contains("\"name") && !value.contains("\"$field\""))
                out.append("[\"coalesce\",[\"get\",\"").append(field).append("\"],").append(value).append(']')
            else out.append(value)
            from = end
        }
        out.append(style, from, style.length)
        return out.toString()
    }

    /** End index of the JSON value starting at [start]: a string, array, object or literal. */
    private fun valueEnd(s: String, start: Int): Int {
        if (start >= s.length) return start
        return when (s[start]) {
            '"' -> stringEnd(s, start)
            '[', '{' -> {
                var depth = 0
                var i = start
                while (i < s.length) {
                    when (s[i]) {
                        '"' -> { i = stringEnd(s, i); continue }
                        '[', '{' -> depth++
                        ']', '}' -> { depth--; if (depth == 0) return i + 1 }
                    }
                    i++
                }
                s.length
            }
            else -> {
                var i = start
                while (i < s.length && s[i] != ',' && s[i] != '}' && s[i] != ']') i++
                i
            }
        }
    }

    private fun stringEnd(s: String, start: Int): Int {
        var i = start + 1
        while (i < s.length) {
            when (s[i]) {
                '\\' -> i += 2
                '"' -> return i + 1
                else -> i++
            }
        }
        return s.length
    }
}
