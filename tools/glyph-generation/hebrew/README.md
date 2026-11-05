# Hebrew map lettering

None of the world typefaces cover Hebrew, so `scripts/generate_glyphs.cjs` adds a
Hebrew face as the last fallback of each fontstack. Only the `1280-1535` glyph range
changes; every other range is byte-identical to the previous build.

The files here are small static subsets (Hebrew block U+0590–U+05FF, presentation
forms U+FB1D–U+FB4F and, where present, ₪ U+20AA). They are build inputs only and are
not packaged in the app.

| File | Source | Instance |
|---|---|---|
| `noto_sans_hebrew_regular.ttf` | Noto Sans Hebrew, [google/fonts `ofl/notosanshebrew`](https://github.com/google/fonts/tree/main/ofl/notosanshebrew) | wght 400, wdth 100 |
| `noto_sans_hebrew_condensed_medium.ttf` | Noto Sans Hebrew | wght 500, wdth 75 |
| `noto_sans_hebrew_condensed_semibold.ttf` | Noto Sans Hebrew | wght 600, wdth 75 |
| `heebo_medium.ttf` | Heebo, [google/fonts `ofl/heebo`](https://github.com/google/fonts/tree/main/ofl/heebo) | wght 500 |
| `frank_ruhl_libre_regular.ttf` | Frank Ruhl Libre, [google/fonts `ofl/frankruhllibre`](https://github.com/google/fonts/tree/main/ofl/frankruhllibre) | wght 400 |
| `frank_ruhl_libre_semibold.ttf` | Frank Ruhl Libre | wght 600 |
| `monocraft_hebrew.ttf` | Hebrew subset of the bundled `app/src/main/res/font/monocraft.ttf` (Monocraft by Idrees Hassan) | static |

Google Fonts sources were taken from commit `bbda7253fc363238b05d97515ca71f8ae49c4fa9`
and instanced and subset with fontTools 4.67 (`varLib.instancer`, `subset`). All faces are
licensed under the SIL Open Font License 1.1; the license texts are alongside.

Fontstack pairing, chosen to sit beside each world's Latin lettering:

| Fontstack | Hebrew face |
|---|---|
| Waypoint Classic, Terminal, Retro | Noto Sans Hebrew Regular |
| Waypoint Condensed, Futura | Noto Sans Hebrew Condensed Medium |
| Waypoint Stratum | Noto Sans Hebrew Condensed SemiBold |
| Waypoint Technical, Borda | Heebo Medium |
| Waypoint Atlas | Frank Ruhl Libre Regular |
| Waypoint Western | Frank Ruhl Libre SemiBold |
| Waypoint Pixel | Monocraft (Hebrew subset) |

Waypoint Retro already draws Hebrew from Monocraft; the Noto face only adds marks it lacks.
