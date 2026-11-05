package com.waypoint.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uniffi.ferrostar.SpokenInstruction
import java.io.File
import java.util.Locale
import java.util.UUID

class HebrewLocalizationTest {
    private val previous = AppLanguage.locale

    @Before fun hebrew() { AppLanguage.locale = { Locale.forLanguageTag("iw-IL") } }
    @After fun restore() { AppLanguage.locale = previous }

    private fun step(type: String, modifier: String = "", road: String = "", exit: Int = 0) =
        Maneuver(Coordinate(32.08, 34.78), type, modifier, road, 100.0, 10.0, exit)

    @Test fun androidAndModernHebrewCodesAreBothHebrew() {
        AppLanguage.locale = { Locale("he") }
        assertTrue(AppLanguage.isHebrew)
        AppLanguage.locale = { Locale.ENGLISH }
        assertFalse(AppLanguage.isHebrew)
        assertEquals("Map", tr("Map", "מפה"))
    }

    @Test fun turnInstructionsAreGenderNeutralHebrew() {
        assertEquals("פנו שמאלה אל הרצל", step("turn", "left", "הרצל").instruction)
        assertEquals("פנו קלות ימינה", step("turn", "slight right").instruction)
        assertEquals("בכיכר, צאו ביציאה השנייה אל דרך נמיר", step("roundabout", exit = 2, road = "דרך נמיר").instruction)
        assertEquals("הישארו משמאל", step("fork", "slight left").instruction)
        assertEquals("צאו ביציאה מימין", step("off ramp", "right").instruction)
        assertEquals("בצעו פניית פרסה", step("turn", "uturn").instruction)
        assertEquals("הגעה ליעד", step("arrive", road = "רוטשילד").instruction)
        assertEquals("בעוד 180 מטר, פנו ימינה", inMetres(180, step("turn", "right").instruction))
    }

    @Test fun englishInstructionsAreUnchanged() {
        AppLanguage.locale = { Locale.US }
        assertEquals("Turn left onto King Street", step("turn", "left", "King Street").instruction)
        assertEquals("In 180 meters, Turn right", inMetres(180, step("turn", "right").instruction))
        assertEquals("1.5 km", formatDistance(1500.0))
    }

    @Test fun distancesUseHebrewUnits() {
        assertEquals("500 מ׳", formatDistance(500.0))
        assertEquals("1.5 ק״מ", formatDistance(1500.0))
        assertEquals("1.0 מייל", formatDistance(1609.344, true))
    }

    @Test fun sentencesOpeningWithLatinStayRightToLeft() {
        assertEquals("‏Waypoint מוכן", rightToLeft("Waypoint מוכן"))
        assertEquals("מפה", rightToLeft("מפה"))
        assertEquals("12 דק׳", rightToLeft("12 דק׳"))
        assertTrue(tr("GPS connected", "GPS מחובר").startsWith("‏"))
    }

    @Test fun everyWorldHasThreeDistinctHebrewLinesPerMoment() {
        for (world in World.entries) {
            val lines = world.voiceLines()
            for (group in listOf(lines.openings, lines.cues, lines.arrivals)) {
                assertEquals(world.name, 3, group.distinct().size)
                assertTrue(world.name, group.all { it.isNotBlank() && it.length <= 80 && it.any { c -> c in 'א'..'ת' } })
            }
        }
    }

    @Test fun hebrewGuidanceKeepsTheTurnAndAddsAHebrewLine() {
        val text = inMetres(200, step("turn", "left", "אלנבי").instruction)
        val cue = SpokenInstruction(text, null, 200.0, UUID.randomUUID())
        val spoken = WorldVoiceNarrator { 0 }.decorate(cue, World.OLD_WEST)
        assertTrue(spoken.text.startsWith(text))
        assertTrue(spoken.text.endsWith(World.OLD_WEST.hebrewVoiceLines().openings.first()))
        val urgent = SpokenInstruction("מחשבים מסלול מחדש", null, 300.0, UUID.randomUUID())
        assertSame(urgent, WorldVoiceNarrator { 0 }.decorate(urgent, World.OLD_WEST))
    }

    @Test fun travelModesAndVoiceProblemsAreTranslated() {
        assertEquals("אופניים", TravelMode.BICYCLE.label)
        assertEquals("מסלול הליכה", TravelMode.WALK.hebrewRoute)
        val problem = GuidanceVoiceStatus.LANGUAGE_UNAVAILABLE.problem
        assertNotNull(problem)
        assertTrue(problem!!.contains("עברי"))
        assertNull(GuidanceVoiceStatus.READY.problem)
    }

    @Test fun hebrewCategorySearchesReachProvidersInEnglish() {
        assertEquals("fuel", PlaceMatcher.providerQuery("דלק"))
        assertEquals("coffee", PlaceMatcher.providerQuery(tr("Coffee", "קפה")))
        assertEquals("parking", PlaceMatcher.providerQuery("חניון"))
        assertEquals("דיזנגוף סנטר", PlaceMatcher.providerQuery("דיזנגוף סנטר"))
        val station = Place("osm:1", "פז", "", Coordinate(32.0, 34.8), "fuel")
        assertEquals(1.0, PlaceMatcher.score("תחנת דלק", station), 0.0)
    }

    @Test fun hebrewGameNamesFindTheirWorld() {
        assertTrue(World.PIXEL.matchesGameQuery("מיינקראפט"))
        assertTrue(World.SKYRIM.matchesGameQuery("סקיירים"))
        assertFalse(World.SKYRIM.matchesGameQuery("מיינקראפט"))
    }

    @Test fun mapLabelsPreferHebrewNamesAndLeaveRoadNumbersAlone() {
        val style = """{"layers":[{"id":"a","layout":{"text-field":["coalesce",["get","name_en"],["get","name"]]}},""" +
            """{"id":"b","layout":{"text-field":["to-string",["get","ref"]]}},{"id":"c","layout":{"text-field":"{name}"}}]}"""
        val layers = JSONObject(MapLabels.localize(style, hebrew = true)).getJSONArray("layers")
        assertEquals("""["coalesce",["get","name:he"],["coalesce",["get","name_en"],["get","name"]]]""",
            layers.getJSONObject(0).getJSONObject("layout").getJSONArray("text-field").toString())
        assertEquals("""["to-string",["get","ref"]]""", layers.getJSONObject(1).getJSONObject("layout").getJSONArray("text-field").toString())
        assertEquals("{name}", layers.getJSONObject(2).getJSONObject("layout").getString("text-field"))
        assertEquals(style, MapLabels.localize(style, hebrew = false))
    }

    @Test fun everyBundledStyleStaysValidWithHebrewLabels() {
        for (world in World.entries) {
            val source = File("src/main/assets/styles/${world.file}.json").readText()
            val localized = MapLabels.localize(source, hebrew = true)
            assertEquals(world.name, localized, MapLabels.localize(localized, hebrew = true))
            val layers = JSONObject(localized).getJSONArray("layers")
            var named = 0
            for (i in 0 until layers.length()) {
                val field = layers.getJSONObject(i).optJSONObject("layout")?.opt("text-field") as? JSONArray ?: continue
                if (field.toString().contains("\"name")) {
                    assertEquals(world.name, "coalesce", field.getString(0))
                    assertEquals(world.name, "name:he", field.getJSONArray(1).getString(1))
                    named++
                }
            }
            assertTrue(world.name, named > 0)
        }
    }

    @Test fun everyMapFontCarriesTheHebrewAlphabet() {
        for (dir in File("src/main/assets/glyphs").listFiles().orEmpty().filter { it.isDirectory }) {
            val ids = glyphIds(File(dir, "1280-1535.pbf").readBytes())
            for (letter in 0x05D0..0x05EA) assertTrue("${dir.name} lacks U+%04X".format(letter), letter in ids)
        }
    }

    /** Reads glyph ids from a MapLibre glyph range: glyphs.stacks[].glyphs[].id. */
    private fun glyphIds(bytes: ByteArray): Set<Int> {
        fun varint(b: ByteArray, start: Int): Pair<Long, Int> {
            var result = 0L; var shift = 0; var i = start
            while (true) { val c = b[i++].toInt() and 0xFF; result = result or ((c and 0x7F).toLong() shl shift); shift += 7; if (c < 0x80) return result to i }
        }
        fun fields(b: ByteArray): List<Pair<Int, Any>> {
            val out = mutableListOf<Pair<Int, Any>>(); var i = 0
            while (i < b.size) {
                val (key, next) = varint(b, i); i = next
                when ((key and 7).toInt()) {
                    0 -> { val (v, n) = varint(b, i); i = n; out += (key shr 3).toInt() to v }
                    2 -> { val (len, n) = varint(b, i); i = n; out += (key shr 3).toInt() to b.copyOfRange(i, i + len.toInt()); i += len.toInt() }
                    5 -> i += 4
                    1 -> i += 8
                }
            }
            return out
        }
        val ids = mutableSetOf<Int>()
        for ((tag, stack) in fields(bytes)) if (tag == 1 && stack is ByteArray)
            for ((field, glyph) in fields(stack)) if (field == 3 && glyph is ByteArray)
                fields(glyph).firstOrNull { it.first == 1 }?.let { ids += (it.second as Long).toInt() }
        return ids
    }
}
