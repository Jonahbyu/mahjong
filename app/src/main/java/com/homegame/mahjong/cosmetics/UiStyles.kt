package com.homegame.mahjong.cosmetics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.homegame.mahjong.ui.FeltMotion

/** A faint pattern drawn over the felt. */
enum class FeltTexture { NONE, WEAVE, DOTS, PINSTRIPE, STARS, LATTICE, WAVES }

/**
 * Colors for everything around the tiles: the felt, panels, the accent used for buttons and
 * headings, the rack and the display font. Backgrounds stay dark so white text reads on all of them.
 */
data class UiTheme(
    val name: String,
    val feltLight: Color,
    val felt: Color,
    val feltEdge: Color,
    val feltDark: Color,
    val accentHigh: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentRim: Color,
    /** Headings and other bright text. */
    val ivory: Color,
    /** Text drawn on top of the accent. */
    val ink: Color,
    val woodLight: Color,
    val woodDark: Color,
    val woodRim: Color,
    val texture: FeltTexture = FeltTexture.NONE,
    val textureColor: Color = Color.White.copy(alpha = 0.05f),
    val display: FontFamily = FontFamily.Serif,
    /** Ambient animation over the felt. */
    val motion: FeltMotion = FeltMotion.NONE,
    /** Colors the animation uses, brightest first. */
    val motionColors: List<Color> = listOf(Color.White),
)

data class UiStyle(val id: String, val rarity: Rarity, val theme: UiTheme) {
    val name get() = theme.name
    val price get() = rarity.price
}

/** All unlockable table styles, in the order shown on the Customize tab. */
object UiStyles {
    private fun c(v: Long) = Color(v)

    private val gold = listOf(c(0xFFFFDB7A), c(0xFFF2C24F), c(0xFFC8912A), c(0xFFFFE9A8))
    private val walnut = listOf(c(0xFF9A6434), c(0xFF55321A), c(0xFFC48A52))

    private fun theme(
        name: String,
        felt: List<Long>,
        accent: List<Color> = gold,
        ivory: Long = 0xFFFFF8E7,
        ink: Long = 0xFF3A2600,
        wood: List<Color> = walnut,
        texture: FeltTexture = FeltTexture.NONE,
        textureColor: Color = Color.White.copy(alpha = 0.05f),
        display: FontFamily = FontFamily.Serif,
        motion: FeltMotion = FeltMotion.NONE,
        motionColors: List<Long> = listOf(0xFFFFFFFF),
    ) = UiTheme(
        name = name,
        feltLight = c(felt[0]), felt = c(felt[1]), feltEdge = c(felt[2]), feltDark = c(felt[3]),
        accentHigh = accent[0], accent = accent[1], accentDeep = accent[2], accentRim = accent[3],
        ivory = c(ivory), ink = c(ink),
        woodLight = wood[0], woodDark = wood[1], woodRim = wood[2],
        texture = texture, textureColor = textureColor, display = display,
        motion = motion, motionColors = motionColors.map { c(it) },
    )

    private fun acc(high: Long, mid: Long, deep: Long, rim: Long) = listOf(c(high), c(mid), c(deep), c(rim))
    private fun wood(light: Long, dark: Long, rim: Long) = listOf(c(light), c(dark), c(rim))

    val all: List<UiStyle> = listOf(
        // Starter: the original table.
        UiStyle("felt", Rarity.STARTER, theme("Classic Felt", listOf(0xFF2B7A5A, 0xFF1D5641, 0xFF0C2F22, 0xFF123A2B))),

        // Common: new felt colors, same brass and walnut.
        UiStyle("navy", Rarity.COMMON, theme(
            "Navy Club", listOf(0xFF2E5A8F, 0xFF1C3D66, 0xFF0A1A33, 0xFF13284A),
            texture = FeltTexture.WEAVE,
        )),
        UiStyle("burgundy", Rarity.COMMON, theme(
            "Burgundy Room", listOf(0xFF8A2E3E, 0xFF5E1A28, 0xFF2A0910, 0xFF3F0F1A),
            texture = FeltTexture.PINSTRIPE,
        )),
        UiStyle("charcoal", Rarity.COMMON, theme(
            "Charcoal & Silver", listOf(0xFF4A4F57, 0xFF33373D, 0xFF141619, 0xFF22252A),
            accent = acc(0xFFF4F6F8, 0xFFC9CED6, 0xFF8A919C, 0xFFFFFFFF), ink = 0xFF1A1D22,
            wood = wood(0xFF5A5F68, 0xFF2A2D33, 0xFF8A909A),
        )),
        UiStyle("lagoon", Rarity.COMMON, theme(
            "Teal Lagoon", listOf(0xFF1F8A8A, 0xFF136262, 0xFF06302F, 0xFF0B4443),
            texture = FeltTexture.WAVES,
        )),

        // Rare: new accents and rack woods.
        UiStyle("royal", Rarity.RARE, theme(
            "Royal Purple", listOf(0xFF6A3E9E, 0xFF482872, 0xFF1C0D33, 0xFF2E1A4D),
            texture = FeltTexture.DOTS, textureColor = Color(0x14F2C24F),
            wood = wood(0xFF4A2E5E, 0xFF24142F, 0xFF8A6AA8),
        )),
        UiStyle("terracotta", Rarity.RARE, theme(
            "Terracotta", listOf(0xFFA0522D, 0xFF73391E, 0xFF33160A, 0xFF4A2210),
            accent = acc(0xFFFFF1DA, 0xFFF3D7A8, 0xFFC9A26A, 0xFFFFF6E6), ink = 0xFF3A1E0A,
            wood = wood(0xFFC08A5A, 0xFF7A4E2A, 0xFFE0B080), texture = FeltTexture.WEAVE,
        )),
        UiStyle("copper", Rarity.RARE, theme(
            "Pine & Copper", listOf(0xFF2F5E46, 0xFF1E4231, 0xFF0A1F16, 0xFF132E22),
            accent = acc(0xFFFFC09A, 0xFFE08A5A, 0xFFA8552C, 0xFFFFD4B8), ink = 0xFF2E1408,
            wood = wood(0xFF7A4A2A, 0xFF3A2010, 0xFFC07A4A), texture = FeltTexture.WEAVE,
        )),
        UiStyle("library", Rarity.RARE, theme(
            "Mahogany Library", listOf(0xFF6E3A22, 0xFF4E2614, 0xFF1F0D05, 0xFF2E1408),
            accent = acc(0xFFF5E0A0, 0xFFD9B864, 0xFFA07E30, 0xFFFAEBC0),
            wood = wood(0xFF3A2414, 0xFF1A0E06, 0xFF8A6438), texture = FeltTexture.PINSTRIPE,
            textureColor = Color(0x10D9B864),
        )),

        // Epic: stronger moods and new display fonts.
        UiStyle("lounge", Rarity.EPIC, theme(
            "Midnight Lounge", listOf(0xFF26335A, 0xFF172040, 0xFF060915, 0xFF0E1428),
            accent = acc(0xFFDDEBFF, 0xFF9EC3FF, 0xFF5A82C9, 0xFFEAF2FF), ink = 0xFF0E1A33, ivory = 0xFFEAF0FF,
            wood = wood(0xFF2A3350, 0xFF10152A, 0xFF5A6A9A), texture = FeltTexture.STARS,
            textureColor = Color(0x55FFFFFF), motion = FeltMotion.TWINKLE, motionColors = listOf(0xFFEAF2FF),
        )),
        UiStyle("arcade", Rarity.EPIC, theme(
            "Neon Arcade", listOf(0xFF3A1A5E, 0xFF22103A, 0xFF08040F, 0xFF140A22),
            accent = acc(0xFFFF8FC8, 0xFFFF2E93, 0xFFB0105E, 0xFFFFB8DC), ink = 0xFF1A0010, ivory = 0xFF7FF7FF,
            wood = wood(0xFF2A1A40, 0xFF0E0618, 0xFF00E5FF), texture = FeltTexture.LATTICE,
            textureColor = Color(0x1A00E5FF), display = FontFamily.Monospace,
            motion = FeltMotion.SYNTHWAVE, motionColors = listOf(0xFF00E5FF, 0xFFFF2E93, 0xFFFFB000),
        )),
        UiStyle("sakura", Rarity.EPIC, theme(
            "Sakura Night", listOf(0xFF6E2E5A, 0xFF4A1C3E, 0xFF1E0818, 0xFF2E0E24),
            accent = acc(0xFFFFD6E4, 0xFFF59AB8, 0xFFC45A80, 0xFFFFE6EE), ink = 0xFF3A0A20, ivory = 0xFFFFF0F4,
            wood = wood(0xFF5A2A40, 0xFF2A0E1C, 0xFFE0A0B8), texture = FeltTexture.DOTS,
            textureColor = Color(0x18FFC0D4), display = FontFamily.Cursive,
            motion = FeltMotion.PETALS, motionColors = listOf(0xFFFFC0D4, 0xFFFFE6EE),
        )),

        // Legendary: lacquer, light and black glass.
        UiStyle("lacquer", Rarity.LEGENDARY, theme(
            "Imperial Lacquer", listOf(0xFFB0202A, 0xFF7A0E16, 0xFF2E0306, 0xFF4A060C),
            wood = wood(0xFF2A1A12, 0xFF0E0806, 0xFFE2B33C), texture = FeltTexture.LATTICE,
            textureColor = Color(0x22F2C24F), motion = FeltMotion.LANTERNS,
            motionColors = listOf(0xFFFF7A2E, 0xFFFFD27A, 0xFF5A1A08),
        )),
        UiStyle("aurora", Rarity.LEGENDARY, theme(
            "Aurora", listOf(0xFF1FA88E, 0xFF2A4E8E, 0xFF140A3A, 0xFF1A2050),
            accent = acc(0xFFD8FFF4, 0xFF7FF0D0, 0xFF2EB896, 0xFFE8FFF8), ink = 0xFF06302A, ivory = 0xFFF0FFFC,
            wood = wood(0xFF2A3A6A, 0xFF10163A, 0xFF7FF0D0), texture = FeltTexture.STARS,
            textureColor = Color(0x66FFFFFF), motion = FeltMotion.AURORA,
            motionColors = listOf(0xFF5CFFC8, 0xFF7FB8FF, 0xFFC08AFF),
        )),
        UiStyle("obsidian", Rarity.LEGENDARY, theme(
            "Obsidian & Gold", listOf(0xFF2E2A24, 0xFF1C1A16, 0xFF070605, 0xFF12100D),
            wood = wood(0xFF1E1B17, 0xFF080706, 0xFFE2B33C), texture = FeltTexture.LATTICE,
            textureColor = Color(0x1AE2B33C), motion = FeltMotion.SWEEP, motionColors = listOf(0xFFFFE08A),
        )),

        // ---- Second collection ----

        // Common: a soft lamp that breathes, or bubbles.
        UiStyle("espresso", Rarity.COMMON, theme(
            "Espresso Bar", listOf(0xFF6A4A36, 0xFF4A3222, 0xFF1E120A, 0xFF2E1E12),
            texture = FeltTexture.DOTS, motion = FeltMotion.BREATHE, motionColors = listOf(0xFFFFD9A0),
        )),
        UiStyle("spray", Rarity.COMMON, theme(
            "Ocean Spray", listOf(0xFF2A7FA0, 0xFF1A5A78, 0xFF082636, 0xFF0E3A50),
            texture = FeltTexture.WAVES, motion = FeltMotion.BUBBLES, motionColors = listOf(0xFFD8F4FF),
        )),
        UiStyle("sage", Rarity.COMMON, theme(
            "Sage Garden", listOf(0xFF6E8A6A, 0xFF4E6A4C, 0xFF1E2E1C, 0xFF2E422C),
            accent = acc(0xFFFFF4DA, 0xFFEAD7A8, 0xFFB89E62, 0xFFFFF8EA), ink = 0xFF2A2410,
            texture = FeltTexture.PINSTRIPE, motion = FeltMotion.BREATHE, motionColors = listOf(0xFFF4FFE0),
        )),
        UiStyle("velvet", Rarity.COMMON, theme(
            "Plum Velvet", listOf(0xFF7A3A6A, 0xFF56244A, 0xFF220A1C, 0xFF36122C),
            texture = FeltTexture.WEAVE, motion = FeltMotion.BREATHE, motionColors = listOf(0xFFFFC8E8),
        )),

        // Rare: weather.
        UiStyle("rainy", Rarity.RARE, theme(
            "Rainy Window", listOf(0xFF3E5468, 0xFF2A3A4A, 0xFF0C141C, 0xFF16222E),
            accent = acc(0xFFF0F6FA, 0xFFBFD2E0, 0xFF7E96AA, 0xFFFFFFFF), ink = 0xFF14202A,
            wood = wood(0xFF4A5662, 0xFF1E262E, 0xFF8A9AA8), motion = FeltMotion.RAIN, motionColors = listOf(0xFFD8E8F4),
        )),
        UiStyle("lodge", Rarity.RARE, theme(
            "Winter Lodge", listOf(0xFF2E5E48, 0xFF1C4232, 0xFF081A12, 0xFF10281C),
            accent = acc(0xFFFFB0A8, 0xFFE0544A, 0xFFA02A22, 0xFFFFD0CA), ink = 0xFF2A0604, ivory = 0xFFFFFAF0,
            wood = wood(0xFF7A4A2A, 0xFF3A2010, 0xFFF0E0C8), texture = FeltTexture.WEAVE,
            motion = FeltMotion.SNOW, motionColors = listOf(0xFFFFFFFF),
        )),
        UiStyle("dusk", Rarity.RARE, theme(
            "Desert Dusk", listOf(0xFFB0603A, 0xFF7A3A3E, 0xFF2A1030, 0xFF401A3A),
            accent = acc(0xFFFFC09A, 0xFFE08A5A, 0xFFA8552C, 0xFFFFD4B8), ink = 0xFF2E1408,
            wood = wood(0xFF8A5A3A, 0xFF4A2A1A, 0xFFE0A070), motion = FeltMotion.RAYS, motionColors = listOf(0xFFFFE0B0),
        )),
        UiStyle("deepsea", Rarity.RARE, theme(
            "Deep Sea", listOf(0xFF1A5A7A, 0xFF0E3A56, 0xFF020E1C, 0xFF06203A),
            accent = acc(0xFFC8FFFF, 0xFF5CE0E6, 0xFF1E9AA8, 0xFFE0FFFF), ink = 0xFF02262A, ivory = 0xFFEFFFFF,
            wood = wood(0xFF1E3A50, 0xFF0A1828, 0xFF5CE0E6), texture = FeltTexture.WAVES, textureColor = Color(0x0FFFFFFF),
            motion = FeltMotion.BUBBLES, motionColors = listOf(0xFFB0F4FF),
        )),

        // Epic: a whole scene.
        UiStyle("forge", Rarity.EPIC, theme(
            "Ember Forge", listOf(0xFF4A2A22, 0xFF2E1814, 0xFF0A0504, 0xFF1A0C08),
            accent = acc(0xFFFFD08A, 0xFFFF8A2E, 0xFFC0480E, 0xFFFFE0B0), ink = 0xFF2A0E00, ivory = 0xFFFFF2E0,
            wood = wood(0xFF3A2A24, 0xFF140C0A, 0xFFFF8A2E), texture = FeltTexture.LATTICE, textureColor = Color(0x10FF8A2E),
            motion = FeltMotion.EMBERS, motionColors = listOf(0xFFFFA040, 0xFFFFE08A),
        )),
        UiStyle("meadow", Rarity.EPIC, theme(
            "Firefly Meadow", listOf(0xFF24503E, 0xFF163428, 0xFF040E0C, 0xFF0A1E18),
            accent = acc(0xFFFFFAC0, 0xFFF0E070, 0xFFB0A030, 0xFFFFFDE0), ink = 0xFF2A2600, ivory = 0xFFF8FFE8,
            wood = wood(0xFF3A3A24, 0xFF16160C, 0xFFF0E070), texture = FeltTexture.DOTS, textureColor = Color(0x0CFFFFFF),
            display = FontFamily.Cursive, motion = FeltMotion.FIREFLIES, motionColors = listOf(0xFFF6FF80, 0xFFFFFFE0),
        )),
        UiStyle("synthwave", Rarity.EPIC, theme(
            "Synthwave", listOf(0xFF5A1E7A, 0xFF34104E, 0xFF0A0216, 0xFF1A0630),
            accent = acc(0xFFB8FFFF, 0xFF2EE6FF, 0xFF0A8AB0, 0xFFE0FFFF), ink = 0xFF001A22, ivory = 0xFFFFD8F4,
            wood = wood(0xFF2A0E40, 0xFF0E0418, 0xFFFF4FC8), display = FontFamily.Monospace,
            motion = FeltMotion.SYNTHWAVE, motionColors = listOf(0xFFFF4FC8, 0xFFFFB02E, 0xFFFF2E6E),
        )),
        UiStyle("cathedral", Rarity.EPIC, theme(
            "Cathedral Light", listOf(0xFF2A3A7A, 0xFF1A2556, 0xFF060A22, 0xFF0E1638),
            wood = wood(0xFF3A2A1A, 0xFF140E06, 0xFFE2B33C), texture = FeltTexture.LATTICE, textureColor = Color(0x14F2C24F),
            motion = FeltMotion.RAYS, motionColors = listOf(0xFFFFF0C0),
        )),

        // Legendary: the sky, the water, the light itself.
        UiStyle("celestial", Rarity.LEGENDARY, theme(
            "Celestial", listOf(0xFF2A2060, 0xFF16103C, 0xFF030212, 0xFF0A0826),
            accent = acc(0xFFFFF4D0, 0xFFF0D080, 0xFFB09040, 0xFFFFFAE8), ink = 0xFF221A00, ivory = 0xFFF4F0FF,
            wood = wood(0xFF241E4A, 0xFF0A0820, 0xFFF0D080), motion = FeltMotion.COMETS,
            motionColors = listOf(0xCCFFFFFF, 0xFFFFF0C0),
        )),
        UiStyle("koi", Rarity.LEGENDARY, theme(
            "Koi Pond", listOf(0xFF1E7A7A, 0xFF125656, 0xFF032222, 0xFF083838),
            accent = acc(0xFFFFD8B0, 0xFFFF8A4A, 0xFFC04E1A, 0xFFFFE8D0), ink = 0xFF2A0E00, ivory = 0xFFF0FFFA,
            wood = wood(0xFF2A4A3A, 0xFF0E2018, 0xFFFF8A4A), texture = FeltTexture.WAVES, textureColor = Color(0x0CFFFFFF),
            motion = FeltMotion.RIPPLES, motionColors = listOf(0xFFE0FFFA),
        )),
        UiStyle("prism", Rarity.LEGENDARY, theme(
            "Prism", listOf(0xFF3A3A4A, 0xFF24242E, 0xFF08080C, 0xFF14141A),
            accent = acc(0xFFFFFFFF, 0xFFE4E8F0, 0xFFA0A8B8, 0xFFFFFFFF), ink = 0xFF14161C,
            wood = wood(0xFF2E2E3A, 0xFF0E0E14, 0xFFFFFFFF), texture = FeltTexture.LATTICE, textureColor = Color(0x0CFFFFFF),
            motion = FeltMotion.PRISM,
        )),
    )

    val starterIds: Set<String> = all.filter { it.rarity == Rarity.STARTER }.map { it.id }.toSet()

    fun byId(id: String?): UiStyle = all.firstOrNull { it.id == id } ?: all.first()
}
