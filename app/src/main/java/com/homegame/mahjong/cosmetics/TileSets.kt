package com.homegame.mahjong.cosmetics

import androidx.compose.ui.graphics.Color
import com.homegame.mahjong.ui.BackPattern
import com.homegame.mahjong.ui.TileFx
import com.homegame.mahjong.ui.TileFx.*
import com.homegame.mahjong.ui.TileSkin

enum class Rarity(val label: String, val price: Int, val color: Color) {
    STARTER("Starter", 0, Color(0xFFB8C4BE)),
    COMMON("Common", 100, Color(0xFFD9E3DE)),
    RARE("Rare", 250, Color(0xFF6FB6FF)),
    EPIC("Epic", 500, Color(0xFFC792FF)),
    LEGENDARY("Legendary", 1000, Color(0xFFF2C24F)),
}

data class TileSet(val id: String, val rarity: Rarity, val skin: TileSkin) {
    val name get() = skin.name
    val price get() = rarity.price
}

/** All unlockable tile sets, in the order shown on the Customize tab. */
object TileSets {
    private val C = TileSkin.Classic

    private fun set(id: String, rarity: Rarity, skin: TileSkin) = TileSet(id, rarity, skin)

    // Dark-faced sets share a light-ink palette shape; this keeps them readable.
    private fun dark(
        name: String, face: Pair<Long, Long>, back: Pair<Long, Long>, pattern: BackPattern,
        bam: Long, accent: Long, dot: Long, ink: Long, flower: Long, joker: Long, jokerAccent: Long,
        patternColor: Color = Color.White.copy(alpha = 0.2f), rim: Color? = null,
        fx: List<TileFx> = emptyList(), fxColor: Color = Color.White,
    ) = C.copy(
        name = name, faceTop = Color(face.first), faceBottom = Color(face.second), edge = Color(0xFF07070A),
        back = Color(back.first), backDeep = Color(back.second), pattern = pattern, patternColor = patternColor,
        bam = Color(bam), bamAccent = Color(accent), crak = Color(accent), crakInk = Color(ink),
        dot = Color(dot), dotAccent = Color(accent), honor = Color(ink), flower = Color(flower), leaf = Color(bam),
        joker = Color(joker), jokerAccent = Color(jokerAccent), shine = 0.2f, sheen = true, rim = rim,
        fx = fx, fxColor = fxColor,
    )

    val all: List<TileSet> = listOf(
        // Starters: owned from the first launch.
        set("classic", Rarity.STARTER, C),
        set("harbor", Rarity.STARTER, C.copy(name = "Harbor Blue", back = Color(0xFF2867B2), backDeep = Color(0xFF0E2F5E), pattern = BackPattern.WAVES)),
        set("ruby", Rarity.STARTER, C.copy(name = "Ruby", back = Color(0xFFB3262E), backDeep = Color(0xFF5E1015), pattern = BackPattern.LATTICE)),

        // Common
        set("amber", Rarity.COMMON, C.copy(name = "Amber", back = Color(0xFFD98A1E), backDeep = Color(0xFF8A4E08), pattern = BackPattern.DOTS)),
        set("plum", Rarity.COMMON, C.copy(name = "Plum", back = Color(0xFF7B3F8C), backDeep = Color(0xFF3E1A4A), pattern = BackPattern.DIAMONDS)),
        set("slate", Rarity.COMMON, C.copy(
            name = "Slate", faceTop = Color(0xFFF8FAFC), faceBottom = Color(0xFFE2E7ED), edge = Color(0xFFB4BEC9),
            back = Color(0xFF4A5A6A), backDeep = Color(0xFF232C36), pattern = BackPattern.STRIPES,
        )),
        set("rose", Rarity.COMMON, C.copy(
            name = "Rose Quartz", faceTop = Color(0xFFFFF6F8), faceBottom = Color(0xFFF6DEE4), edge = Color(0xFFDDB3BE),
            back = Color(0xFFE08AA2), backDeep = Color(0xFFA24F68), pattern = BackPattern.DOTS, flower = Color(0xFFE0457A),
        )),
        set("mint", Rarity.COMMON, C.copy(
            name = "Mint", faceTop = Color(0xFFF8FFFB), faceBottom = Color(0xFFE0F2E8), edge = Color(0xFFAFD2BF),
            back = Color(0xFF4FBF9F), backDeep = Color(0xFF1F7A60), pattern = BackPattern.LATTICE,
        )),
        set("cocoa", Rarity.COMMON, C.copy(
            name = "Cocoa", faceTop = Color(0xFFFFF7EA), faceBottom = Color(0xFFEBD8B8), edge = Color(0xFFC6AA80),
            back = Color(0xFF6B4226), backDeep = Color(0xFF3A2112), pattern = BackPattern.PLAID,
        )),
        set("sky", Rarity.COMMON, C.copy(name = "Sky", back = Color(0xFF5BB0E6), backDeep = Color(0xFF2C6E9E), pattern = BackPattern.SCALES, patternColor = Color.White.copy(alpha = 0.3f))),
        set("forest", Rarity.COMMON, C.copy(
            name = "Forest", back = Color(0xFF2E5E2E), backDeep = Color(0xFF132B13), pattern = BackPattern.STRIPES,
            bam = Color(0xFF175E2E), leaf = Color(0xFF175E2E),
        )),
        set("sunflower", Rarity.COMMON, C.copy(
            name = "Sunflower", back = Color(0xFFF2B630), backDeep = Color(0xFFA5700A), pattern = BackPattern.SUNBURST,
            flower = Color(0xFFF0901A), patternColor = Color.White.copy(alpha = 0.26f),
        )),
        set("coral", Rarity.COMMON, C.copy(name = "Coral Reef", back = Color(0xFFF07560), backDeep = Color(0xFFB33A2A), pattern = BackPattern.SCALES)),

        // Rare
        set("sapphire", Rarity.RARE, C.copy(
            name = "Sapphire", back = Color(0xFF2346B0), backDeep = Color(0xFF0A1A55), pattern = BackPattern.LATTICE,
            dot = Color(0xFF1E3FA0), crakInk = Color(0xFF0E1E52), honor = Color(0xFF0E1E52),
        )),
        set("lavender", Rarity.RARE, C.copy(
            name = "Lavender Fields", faceTop = Color(0xFFFCF9FF), faceBottom = Color(0xFFEAE1F7), edge = Color(0xFFC6B6E2),
            back = Color(0xFF9B7BD0), backDeep = Color(0xFF5A3E8F), pattern = BackPattern.DIAMONDS,
            flower = Color(0xFF8E5BD6), joker = Color(0xFF5A3E8F),
        )),
        set("teal", Rarity.RARE, C.copy(
            name = "Teal Tide", back = Color(0xFF138A8A), backDeep = Color(0xFF07494A), pattern = BackPattern.WAVES,
            dot = Color(0xFF0F6F7A), patternColor = Color.White.copy(alpha = 0.28f),
        )),
        set("blossom", Rarity.RARE, C.copy(
            name = "Cherry Blossom", faceTop = Color(0xFFFFF9FA), faceBottom = Color(0xFFF9E3E8), edge = Color(0xFFE3B9C3),
            back = Color(0xFFE9A7B8), backDeep = Color(0xFFB0587A), pattern = BackPattern.STARS,
            flower = Color(0xFFE0607E), crak = Color(0xFFD03A64), bamAccent = Color(0xFFD03A64), dotAccent = Color(0xFFD03A64),
        )),
        set("marble", Rarity.RARE, C.copy(
            name = "Marble", faceTop = Color(0xFFFCFCFC), faceBottom = Color(0xFFDADADA), edge = Color(0xFFAEAEAE),
            back = Color(0xFF3B3B3B), backDeep = Color(0xFF111111), pattern = BackPattern.STRIPES,
            patternColor = Color.White.copy(alpha = 0.12f), honor = Color(0xFF111111), crakInk = Color(0xFF111111),
        )),
        set("honeycomb", Rarity.RARE, C.copy(
            name = "Honeycomb", faceTop = Color(0xFFFFFBEF), faceBottom = Color(0xFFF5E6BF), edge = Color(0xFFD7BD7C),
            back = Color(0xFFE8A317), backDeep = Color(0xFF9A6200), pattern = BackPattern.HEX, patternColor = Color(0x66FFF3C4),
        )),
        set("denim", Rarity.RARE, C.copy(name = "Denim", back = Color(0xFF3D5A80), backDeep = Color(0xFF1E2F47), pattern = BackPattern.PLAID, patternColor = Color.White.copy(alpha = 0.16f))),
        set("autumn", Rarity.RARE, C.copy(
            name = "Autumn", faceTop = Color(0xFFFFF6E5), faceBottom = Color(0xFFF0DBB4), edge = Color(0xFFCDAE7A),
            back = Color(0xFFC0571E), backDeep = Color(0xFF6E2A0A), pattern = BackPattern.LATTICE,
            bam = Color(0xFF5E7A1C), leaf = Color(0xFF5E7A1C), crak = Color(0xFFB23A12), bamAccent = Color(0xFFB23A12),
            dot = Color(0xFF7A3E12), dotAccent = Color(0xFFB23A12), flower = Color(0xFFD9641E),
        )),
        set("arctic", Rarity.RARE, C.copy(
            name = "Arctic", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFE4F2FA), edge = Color(0xFFB0D2E6),
            back = Color(0xFF8CC8E4), backDeep = Color(0xFF3F7FA3), pattern = BackPattern.STARS, patternColor = Color.White.copy(alpha = 0.55f),
            dot = Color(0xFF2A6F9E), honor = Color(0xFF1D3B52), crakInk = Color(0xFF1D3B52),
        )),

        // Epic: glossy faces.
        set("midnight", Rarity.EPIC, dark(
            "Midnight", 0xFF2A2F45 to 0xFF181B2B, 0xFF3D4A8A to 0xFF141A45, BackPattern.STARS,
            bam = 0xFF6EE7A0, accent = 0xFFFF6B7A, dot = 0xFF7FB2FF, ink = 0xFFE8ECFF, flower = 0xFFFF8FC0,
            joker = 0xFFC9A6FF, jokerAccent = 0xFFFFD45C, patternColor = Color(0x88FFE9A8),
            fx = listOf(STARFIELD), fxColor = Color(0xFFFFE9A8),
        )),
        set("jade", Rarity.EPIC, C.copy(
            name = "Imperial Jade", faceTop = Color(0xFFE2F5E8), faceBottom = Color(0xFFB5DCC1), edge = Color(0xFF86BB97),
            back = Color(0xFF0F5C3A), backDeep = Color(0xFF06301E), pattern = BackPattern.LATTICE, patternColor = Color(0x55E2B33C),
            bam = Color(0xFF0E5A34), leaf = Color(0xFF0E5A34), crak = Color(0xFFB71C1C), crakInk = Color(0xFF0E3A26),
            dot = Color(0xFF0E5A34), honor = Color(0xFF0E3A26), shine = 0.6f, sheen = true, fx = listOf(SHIMMER),
        )),
        set("neon", Rarity.EPIC, dark(
            "Neon Arcade", 0xFF17132A to 0xFF0A0812, 0xFFFF2E93 to 0xFF5A0B3B, BackPattern.STRIPES,
            bam = 0xFF39FF88, accent = 0xFFFF2E93, dot = 0xFF00E5FF, ink = 0xFFF5F5F5, flower = 0xFFFF2E93,
            joker = 0xFFFFE600, jokerAccent = 0xFF00E5FF, patternColor = Color(0x4400E5FF),
            fx = listOf(PULSE), fxColor = Color(0xFFFF2E93),
        )),
        set("porcelain", Rarity.EPIC, C.copy(
            name = "Blue Porcelain", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFECF1FA), edge = Color(0xFFB4C1DA),
            back = Color(0xFF2148A0), backDeep = Color(0xFF0D2563), pattern = BackPattern.WAVES, patternColor = Color.White.copy(alpha = 0.35f),
            bam = Color(0xFF2148A0), bamAccent = Color(0xFF0D2563), leaf = Color(0xFF4A6FC0), crak = Color(0xFF2148A0),
            crakInk = Color(0xFF0D2563), dot = Color(0xFF2148A0), dotAccent = Color(0xFF0D2563), honor = Color(0xFF0D2563),
            flower = Color(0xFF2F5FC4), joker = Color(0xFF0D2563), jokerAccent = Color(0xFF4A90E2), sheen = true,
            fx = listOf(SHIMMER),
        )),
        set("sunset", Rarity.EPIC, C.copy(
            name = "Sunset Glass", faceTop = Color(0xFFFFF3E3), faceBottom = Color(0xFFFFD2C0), edge = Color(0xFFE0A48A),
            back = Color(0xFFFF7E5F), backDeep = Color(0xFF6B2D73), pattern = BackPattern.SUNBURST, patternColor = Color(0x33FFE3A0),
            crak = Color(0xFFD9432F), bamAccent = Color(0xFFD9432F), dotAccent = Color(0xFFD9432F), dot = Color(0xFF6B2D73),
            joker = Color(0xFF6B2D73), jokerAccent = Color(0xFFFF9A3C), flower = Color(0xFFFF6F61), sheen = true,
            fx = listOf(MOLTEN), fxColor = Color(0xFFFF7E5F),
        )),

        // Legendary: gloss plus a metal rim.
        set("gold", Rarity.LEGENDARY, C.copy(
            name = "24 Karat", faceTop = Color(0xFFFFFBEA), faceBottom = Color(0xFFF2DFA6), edge = Color(0xFFC9A54A),
            back = Color(0xFFE2B33C), backDeep = Color(0xFF8A6210), pattern = BackPattern.SUNBURST, patternColor = Color(0x44FFF6D0),
            joker = Color(0xFF8A6210), jokerAccent = Color(0xFFE2B33C), sheen = true, rim = Color(0xFFFFD86B),
            fx = listOf(SHIMMER, GLINT), fxColor = Color(0xFFFFF6D0),
        )),
        set("obsidian", Rarity.LEGENDARY, dark(
            "Obsidian & Gold", 0xFF1F1C18 to 0xFF0D0C0A, 0xFF2A2620 to 0xFF0A0907, BackPattern.LATTICE,
            bam = 0xFFE2B33C, accent = 0xFFE5484D, dot = 0xFFF3E2AE, ink = 0xFFF3E2AE, flower = 0xFFE5484D,
            joker = 0xFFE2B33C, jokerAccent = 0xFFE5484D, patternColor = Color(0x88E2B33C), rim = Color(0xFFE2B33C),
            fx = listOf(GLINT), fxColor = Color(0xFFFFE08A),
        )),
        set("aurora", Rarity.LEGENDARY, C.copy(
            name = "Aurora", faceTop = Color(0xFFF4FFFC), faceBottom = Color(0xFFDDEBFF), edge = Color(0xFFA6C4DE),
            back = Color(0xFF26C6A6), backDeep = Color(0xFF5B3FC4), pattern = BackPattern.STARS, patternColor = Color.White.copy(alpha = 0.5f),
            bam = Color(0xFF0F9D7A), leaf = Color(0xFF0F9D7A), crak = Color(0xFF7A3FC4), bamAccent = Color(0xFF7A3FC4),
            dot = Color(0xFF1F78C9), dotAccent = Color(0xFF7A3FC4), flower = Color(0xFFB04FD6),
            joker = Color(0xFF5B3FC4), jokerAccent = Color(0xFF26C6A6), sheen = true, rim = Color(0xFFB8F0FF),
            fx = listOf(HOLO),
        )),

        // ---- Second collection ----

        // Common
        set("lime", Rarity.COMMON, C.copy(name = "Key Lime", back = Color(0xFF8CC63F), backDeep = Color(0xFF4A7A12), pattern = BackPattern.CHECKER)),
        set("tangerine", Rarity.COMMON, C.copy(name = "Tangerine", back = Color(0xFFF57C1F), backDeep = Color(0xFFA8420A), pattern = BackPattern.CHEVRON)),
        set("cobalt", Rarity.COMMON, C.copy(name = "Cobalt", back = Color(0xFF1F5FD6), backDeep = Color(0xFF0A2A72), pattern = BackPattern.RINGS)),
        set("blush", Rarity.COMMON, C.copy(
            name = "Blush", faceTop = Color(0xFFFFF8F6), faceBottom = Color(0xFFF7E2DC), edge = Color(0xFFDDB8AE),
            back = Color(0xFFE8A598), backDeep = Color(0xFFA85C50), pattern = BackPattern.HEX, patternColor = Color(0x44FFFFFF),
        )),
        set("olive", Rarity.COMMON, C.copy(name = "Olive Grove", back = Color(0xFF7A7A2E), backDeep = Color(0xFF3E3E12), pattern = BackPattern.PLAID)),
        set("graphite", Rarity.COMMON, C.copy(
            name = "Graphite", faceTop = Color(0xFFF6F6F6), faceBottom = Color(0xFFE0E0E0), edge = Color(0xFFB0B0B0),
            back = Color(0xFF4A4A4E), backDeep = Color(0xFF1E1E22), pattern = BackPattern.CHEVRON, patternColor = Color(0x22FFFFFF),
        )),
        set("lilac", Rarity.COMMON, C.copy(name = "Lilac", back = Color(0xFFB59AD8), backDeep = Color(0xFF6E52A0), pattern = BackPattern.DOTS, flower = Color(0xFF9A5AD0))),
        set("seafoam", Rarity.COMMON, C.copy(name = "Seafoam", back = Color(0xFF6CCFB8), backDeep = Color(0xFF2A8A74), pattern = BackPattern.WAVES, patternColor = Color(0x44FFFFFF))),

        // Rare: a couple start to catch the light.
        set("bamboo", Rarity.RARE, C.copy(
            name = "Bamboo Grove", faceTop = Color(0xFFFBFFF2), faceBottom = Color(0xFFE6F0D2), edge = Color(0xFFB6C890),
            back = Color(0xFF5A9A3A), backDeep = Color(0xFF2A5A18), pattern = BackPattern.STRIPES,
            bam = Color(0xFF2E6A1E), leaf = Color(0xFF2E6A1E),
        )),
        set("pearl", Rarity.RARE, C.copy(
            name = "Pearl", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFEFEAF2), edge = Color(0xFFC8C0D0),
            back = Color(0xFFDCD6E4), backDeep = Color(0xFF9A90AA), pattern = BackPattern.RINGS, patternColor = Color(0x55FFFFFF),
            fx = listOf(SHIMMER),
        )),
        set("emerald", Rarity.RARE, C.copy(
            name = "Emerald Cut", back = Color(0xFF0E8A5A), backDeep = Color(0xFF044A2E), pattern = BackPattern.DIAMONDS,
            patternColor = Color(0x44C8FFE0), fx = listOf(SHIMMER),
        )),
        set("terracotta", Rarity.RARE, C.copy(
            name = "Terracotta", faceTop = Color(0xFFFFF4EA), faceBottom = Color(0xFFF0D8C4), edge = Color(0xFFCCA88A),
            back = Color(0xFFB8572E), backDeep = Color(0xFF6A2810), pattern = BackPattern.CHEVRON,
            crak = Color(0xFFB8402A), bamAccent = Color(0xFFB8402A), dotAccent = Color(0xFFB8402A),
        )),
        set("inkwash", Rarity.RARE, C.copy(
            name = "Ink Wash", faceTop = Color(0xFFFAF8F2), faceBottom = Color(0xFFE6E2D6), edge = Color(0xFFB8B2A2),
            back = Color(0xFF2A2A2A), backDeep = Color(0xFF0A0A0A), pattern = BackPattern.WAVES, patternColor = Color(0x22FFFFFF),
            bam = Color(0xFF1E1E1E), leaf = Color(0xFF3A3A3A), dot = Color(0xFF1E1E1E), honor = Color(0xFF111111), crakInk = Color(0xFF111111),
        )),
        set("penny", Rarity.RARE, C.copy(
            name = "Copper Penny", back = Color(0xFFC0703A), backDeep = Color(0xFF6A3412), pattern = BackPattern.RINGS,
            patternColor = Color(0x44FFE0C0), rim = Color(0xFFE0905A),
        )),
        set("saffron", Rarity.RARE, C.copy(
            name = "Saffron Silk", faceTop = Color(0xFFFFFBEE), faceBottom = Color(0xFFF6E8C2), edge = Color(0xFFD6BC80),
            back = Color(0xFFE09A12), backDeep = Color(0xFF9A3A0A), pattern = BackPattern.SUNBURST, patternColor = Color(0x33FFF0C0),
            crak = Color(0xFFC0300E), flower = Color(0xFFD0501A),
        )),
        set("glacier", Rarity.RARE, C.copy(
            name = "Glacier", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFE2F2F8), edge = Color(0xFFA8CCDC),
            back = Color(0xFF6AB8D8), backDeep = Color(0xFF1E6A8A), pattern = BackPattern.DIAMONDS, patternColor = Color(0x55FFFFFF),
            dot = Color(0xFF1E6A8A), fx = listOf(FROST), fxColor = Color(0xFFD8F4FF),
        )),
        set("desertrose", Rarity.RARE, C.copy(
            name = "Desert Rose", faceTop = Color(0xFFFFF6F0), faceBottom = Color(0xFFF2DCD0), edge = Color(0xFFD0AE9A),
            back = Color(0xFFC07A6A), backDeep = Color(0xFF6A3A34), pattern = BackPattern.SCALES, flower = Color(0xFFC04A5A),
        )),

        // Epic: everything moves.
        set("holo", Rarity.EPIC, C.copy(
            name = "Holo Foil", faceTop = Color(0xFFFAFAFF), faceBottom = Color(0xFFE6E8F4), edge = Color(0xFFB8BCD0),
            back = Color(0xFFB8C0D8), backDeep = Color(0xFF5A6080), pattern = BackPattern.DIAMONDS, patternColor = Color(0x66FFFFFF),
            sheen = true, fx = listOf(HOLO, GLINT),
        )),
        set("galaxy", Rarity.EPIC, dark(
            "Galaxy", 0xFF241A44 to 0xFF0E0A1E, 0xFF3A1E6A to 0xFF0A0420, BackPattern.STARS,
            bam = 0xFF7FFFD0, accent = 0xFFFF7AC0, dot = 0xFF9AB8FF, ink = 0xFFF0EAFF, flower = 0xFFFF9AD8,
            joker = 0xFFFFE08A, jokerAccent = 0xFF7FFFD0, patternColor = Color(0x99FFFFFF),
            fx = listOf(STARFIELD), fxColor = Color(0xFFFFFFFF),
        )),
        set("lava", Rarity.EPIC, dark(
            "Lava Lamp", 0xFF2E1410 to 0xFF140806, 0xFFE0401A to 0xFF5A0A04, BackPattern.SCALES,
            bam = 0xFFFFC040, accent = 0xFFFF5A2E, dot = 0xFFFF9A40, ink = 0xFFFFEAD8, flower = 0xFFFF6A4A,
            joker = 0xFFFFE04A, jokerAccent = 0xFFFF5A2E, patternColor = Color(0x44FFC040),
            fx = listOf(MOLTEN), fxColor = Color(0xFFFF6A1A),
        )),
        set("electric", Rarity.EPIC, dark(
            "Electric", 0xFF101C2E to 0xFF060A14, 0xFF0A6AD0 to 0xFF021A40, BackPattern.CHEVRON,
            bam = 0xFF5CFFB0, accent = 0xFF2EE6FF, dot = 0xFF7FD0FF, ink = 0xFFE8F8FF, flower = 0xFF9AE0FF,
            joker = 0xFFFFF04A, jokerAccent = 0xFF2EE6FF, patternColor = Color(0x552EE6FF),
            fx = listOf(PULSE), fxColor = Color(0xFF2EE6FF),
        )),
        set("rosegold", Rarity.EPIC, C.copy(
            name = "Rose Gold", faceTop = Color(0xFFFFF8F4), faceBottom = Color(0xFFF4DCD2), edge = Color(0xFFD8A898),
            back = Color(0xFFE0A090), backDeep = Color(0xFF9A5A4A), pattern = BackPattern.RINGS, patternColor = Color(0x44FFF0E8),
            joker = Color(0xFF9A5A4A), jokerAccent = Color(0xFFE0A090), sheen = true, rim = Color(0xFFF0B8A8),
            fx = listOf(SHIMMER), fxColor = Color(0xFFFFF0EA),
        )),
        set("frostbite", Rarity.EPIC, C.copy(
            name = "Frostbite", faceTop = Color(0xFFF4FCFF), faceBottom = Color(0xFFD4ECF8), edge = Color(0xFF8ABCD8),
            back = Color(0xFF3A8AC0), backDeep = Color(0xFF0E2E5A), pattern = BackPattern.STARS, patternColor = Color(0x88FFFFFF),
            dot = Color(0xFF1E5A9A), honor = Color(0xFF0E2E5A), crakInk = Color(0xFF0E2E5A), sheen = true,
            fx = listOf(FROST, GLINT), fxColor = Color(0xFFE0F6FF),
        )),
        set("toxic", Rarity.EPIC, dark(
            "Toxic", 0xFF14200E to 0xFF080E06, 0xFF4ACA1E to 0xFF0E3A04, BackPattern.HEX,
            bam = 0xFF8AFF4A, accent = 0xFFE0FF2E, dot = 0xFF4AFFB0, ink = 0xFFEAFFE0, flower = 0xFFE0FF2E,
            joker = 0xFFE0FF2E, jokerAccent = 0xFF8AFF4A, patternColor = Color(0x558AFF4A),
            fx = listOf(PULSE, MOLTEN), fxColor = Color(0xFF8AFF4A),
        )),

        // Legendary: stacked finishes and a metal rim.
        set("prismatic", Rarity.LEGENDARY, C.copy(
            name = "Prismatic", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFEEF0F8), edge = Color(0xFFB8BED0),
            back = Color(0xFF8A7AE0), backDeep = Color(0xFF2E7AC0), pattern = BackPattern.SUNBURST, patternColor = Color(0x55FFFFFF),
            sheen = true, rim = Color(0xFFFFFFFF), fx = listOf(HOLO, RAINBOW_RIM),
        )),
        set("dragonscale", Rarity.LEGENDARY, dark(
            "Dragon Scale", 0xFF3A0E10 to 0xFF160406, 0xFF8A1018 to 0xFF2A0206, BackPattern.SCALES,
            bam = 0xFFE2B33C, accent = 0xFFFF4A3A, dot = 0xFFFFC07A, ink = 0xFFFFE8C8, flower = 0xFFFF6A4A,
            joker = 0xFFE2B33C, jokerAccent = 0xFFFF4A3A, patternColor = Color(0x77E2B33C), rim = Color(0xFFE2B33C),
            fx = listOf(MOLTEN, GLINT), fxColor = Color(0xFFFF8A2E),
        )),
        set("nebula", Rarity.LEGENDARY, dark(
            "Nebula", 0xFF2E1A4A to 0xFF100820, 0xFF7A2EC0 to 0xFF1A0640, BackPattern.STARS,
            bam = 0xFF7FFFE0, accent = 0xFFFF5AC8, dot = 0xFFB89AFF, ink = 0xFFF4EAFF, flower = 0xFFFF7AD8,
            joker = 0xFFFFE08A, jokerAccent = 0xFFFF5AC8, patternColor = Color(0xAAFFFFFF), rim = Color(0xFFC89AFF),
            fx = listOf(STARFIELD, HOLO), fxColor = Color(0xFFFFFFFF),
        )),
        set("diamond", Rarity.LEGENDARY, C.copy(
            name = "Diamond", faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFEAF0F6), edge = Color(0xFFB0BCCC),
            back = Color(0xFFC8D4E4), backDeep = Color(0xFF6A7A90), pattern = BackPattern.DIAMONDS, patternColor = Color(0x88FFFFFF),
            honor = Color(0xFF14202E), crakInk = Color(0xFF14202E), sheen = true, rim = Color(0xFFE8F0FF),
            fx = listOf(GLINT, SHIMMER),
        )),
        set("liquidgold", Rarity.LEGENDARY, C.copy(
            name = "Liquid Gold", faceTop = Color(0xFFFFF4C8), faceBottom = Color(0xFFE8C860), edge = Color(0xFFB08A20),
            back = Color(0xFFF0C030), backDeep = Color(0xFF7A5200), pattern = BackPattern.WAVES, patternColor = Color(0x66FFF6D0),
            bam = Color(0xFF5A3A00), leaf = Color(0xFF5A3A00), dot = Color(0xFF5A3A00), honor = Color(0xFF3A2600), crakInk = Color(0xFF3A2600),
            joker = Color(0xFF5A3A00), jokerAccent = Color(0xFFB71C1C), sheen = true, rim = Color(0xFFFFE680),
            fx = listOf(MOLTEN, SHIMMER), fxColor = Color(0xFFFFF6D0),
        )),
        set("void", Rarity.LEGENDARY, dark(
            "Void", 0xFF0E0A16 to 0xFF040208, 0xFF1A0E2E to 0xFF020104, BackPattern.RINGS,
            bam = 0xFFB07AFF, accent = 0xFFFF4AE0, dot = 0xFF7A9AFF, ink = 0xFFEAE0FF, flower = 0xFFFF4AE0,
            joker = 0xFFFFFFFF, jokerAccent = 0xFFB07AFF, patternColor = Color(0x66B07AFF), rim = Color(0xFFB07AFF),
            fx = listOf(PULSE, STARFIELD), fxColor = Color(0xFFB07AFF),
        )),
    )

    val starterIds: Set<String> = all.filter { it.rarity == Rarity.STARTER }.map { it.id }.toSet()

    fun byId(id: String?): TileSet = all.firstOrNull { it.id == id } ?: all.first()
}
