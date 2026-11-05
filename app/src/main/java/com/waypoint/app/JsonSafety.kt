package com.waypoint.app

/** Validate strict JSON and bound nesting before the platform's lenient recursive parser. */
internal object JsonSafety {
    fun checkDepth(text: String, maxDepth: Int = 64, maxValues: Int = 250_000) {
        require(maxDepth in 1..64)
        require(maxValues > 0)
        Reader(text, maxDepth, maxValues).read()
    }

    private class Reader(val text: String, val maxDepth: Int, val maxValues: Int) {
        var index = 0
        var values = 0
        fun read() { value(0); whitespace(); require(index == text.length) { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") } }
        fun whitespace() { while (index < text.length && text[index] in " \t\r\n") index++ }
        fun take(c: Char): Boolean { whitespace(); return if (index < text.length && text[index] == c) { index++; true } else false }
        fun expect(c: Char) { require(take(c)) { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") } }
        fun value(depth: Int) {
            require(++values <= maxValues) { tr("The service response is too complex.","התשובה מהשירות מורכבת מדי.") }
            whitespace()
            require(index < text.length) { tr("Incomplete service response.","התשובה מהשירות לא הושלמה.") }
            when (text[index]) {
                '{', '[' -> {
                    require(depth < maxDepth) { tr("The service response is too complex.","התשובה מהשירות מורכבת מדי.") }
                    val objectValue = text[index++] == '{'
                    val closing = if (objectValue) '}' else ']'
                    if (take(closing)) return
                    do {
                        if (objectValue) { whitespace(); string(); expect(':') }
                        value(depth + 1)
                    } while (take(','))
                    expect(closing)
                }
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> {
                    val start = index
                    while (index < text.length && text[index] in "-+0123456789.eE") index++
                    require(number.matches(text.substring(start,index))) { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") }
                }
            }
        }
        fun literal(value: String) { require(text.startsWith(value,index)) { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") }; index += value.length }
        fun string() {
            require(index < text.length && text[index++] == '"') { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") }
            while (index < text.length) {
                val c = text[index++]
                if (c == '"') return
                require(c >= ' ') { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") }
                if (c == '\\') {
                    require(index < text.length) { tr("Incomplete service response.","התשובה מהשירות לא הושלמה.") }
                    val escaped = text[index++]
                    if (escaped == 'u') {
                        repeat(4) { require(index < text.length && text[index++].digitToIntOrNull(16) != null) { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") } }
                    } else require(escaped in "\"\\/bfnrt") { tr("Invalid service response.","התקבלה תשובה לא תקינה מהשירות.") }
                }
            }
            error(tr("Incomplete service response.","התשובה מהשירות לא הושלמה."))
        }
    }
    private val number = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
}
