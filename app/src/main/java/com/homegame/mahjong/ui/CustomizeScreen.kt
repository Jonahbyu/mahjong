package com.homegame.mahjong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.cosmetics.Rarity
import com.homegame.mahjong.cosmetics.TileSet
import com.homegame.mahjong.cosmetics.UiStyle
import com.homegame.mahjong.cosmetics.UiStyles
import com.homegame.mahjong.cosmetics.UiTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.homegame.mahjong.cosmetics.TileSets
import com.homegame.mahjong.cosmetics.Wallet
import com.homegame.mahjong.engine.Dragon
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Suit

private enum class MenuPage { START, COLLECTION, CUSTOM_TABLE, CIRCUITS, HUB }

/** The menu: the start screen, with the Shop card opening the collection and Custom opening the table setup. */
@Composable
fun HomeScreen(vm: TableViewModel, onHowToPlay: () -> Unit, onSettings: () -> Unit) {
    // Coming back from a circuit table lands on that circuit's hub.
    val backFromCircuit = vm.openHub
    var page by rememberSaveable { mutableStateOf(if (backFromCircuit) MenuPage.HUB else MenuPage.START) }
    var hubIndex by rememberSaveable { mutableStateOf(vm.career.run?.circuitIndex ?: 0) }
    LaunchedEffect(Unit) { vm.openHub = false }
    Box(Modifier.fillMaxSize().felt().safeDrawingPadding()) {
        if (page != MenuPage.START) BackHandler { page = if (page == MenuPage.HUB) MenuPage.CIRCUITS else MenuPage.START }
        when (page) {
            MenuPage.START -> StartScreen(
                vm, onHowToPlay, onSettings,
                onShop = { page = MenuPage.COLLECTION },
                onCustom = { page = MenuPage.CUSTOM_TABLE },
                onCircuits = { page = MenuPage.CIRCUITS },
            )
            MenuPage.CIRCUITS -> CircuitLadderScreen(
                vm, onClose = { page = MenuPage.START },
                onOpen = { hubIndex = it; page = MenuPage.HUB },
            )
            MenuPage.HUB -> CircuitHubScreen(vm, hubIndex, onBack = { page = MenuPage.CIRCUITS })
            MenuPage.COLLECTION -> CustomizeScreen(vm.wallet, onClose = { page = MenuPage.START })
            MenuPage.CUSTOM_TABLE -> CustomTableScreen(
                vm,
                onClose = { page = MenuPage.START },
                onDeal = { page = MenuPage.START; vm.newGame(vm.customLevels, vm.customRules) },
            )
        }
    }
}

private enum class Filter(val label: String) { ALL("All"), OWNED("Unlocked"), LOCKED("Locked") }

private enum class Shelf(val title: String) { TILES("Tile sets"), TABLE("Table styles") }

@Composable
fun CustomizeScreen(wallet: Wallet, onClose: () -> Unit) {
    var shelf by rememberSaveable { mutableStateOf(Shelf.TILES) }
    var inspectedId by rememberSaveable { mutableStateOf(wallet.equippedId) }
    var inspectedUiId by rememberSaveable { mutableStateOf(wallet.equippedUiId) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    fun keep(owned: Boolean) = when (filter) {
        Filter.ALL -> true
        Filter.OWNED -> owned
        Filter.LOCKED -> !owned
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) { Pill("‹ Menu", onClick = onClose) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(shelf.title, color = Ivory, fontSize = 28.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                val (have, total) = when (shelf) {
                    Shelf.TILES -> wallet.owned.size to TileSets.all.size
                    Shelf.TABLE -> wallet.ownedUi.size to UiStyles.all.size
                }
                Text("$have of $total unlocked", color = Soft, fontSize = 13.sp)
            }
            Box(
                Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { CoinAmount("${wallet.coins}") }
        }

        Segmented(Shelf.entries.map { it.title }, shelf.ordinal) { shelf = Shelf.entries[it] }

        when (shelf) {
            Shelf.TILES -> Preview(TileSets.byId(inspectedId), wallet)
            Shelf.TABLE -> UiPreview(UiStyles.byId(inspectedUiId), wallet)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Filter.entries.forEach { f -> Pill(f.label, active = filter == f, onClick = { filter = f }) }
        }

        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (shelf) {
                Shelf.TILES -> items(TileSets.all.filter { keep(wallet.owns(it)) }, key = { it.id }) { set ->
                    SetCard(set, wallet, selected = set.id == inspectedId) { inspectedId = set.id }
                }
                Shelf.TABLE -> items(UiStyles.all.filter { keep(wallet.owns(it)) }, key = { "ui_" + it.id }) { style ->
                    UiCard(style, wallet, selected = style.id == inspectedUiId) { inspectedUiId = style.id }
                }
            }
        }
    }
}

/** A tiny table drawn in the given style: felt, a heading, an accent button and a rack. */
@Composable
private fun TableSwatch(theme: UiTheme, modifier: Modifier = Modifier, big: Boolean = false) {
    val shape = RoundedCornerShape(10.dp)
    val tile = if (big) 26.dp else 18.dp
    Column(
        modifier.clip(shape).felt(theme).border(1.dp, Faint, shape).padding(if (big) 10.dp else 7.dp),
        verticalArrangement = Arrangement.spacedBy(if (big) 8.dp else 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mahjong", color = theme.ivory, fontFamily = theme.display, fontWeight = FontWeight.Bold,
                fontSize = if (big) 20.sp else 13.sp, maxLines = 1, modifier = Modifier.weight(1f),
            )
            Text(
                "Play", color = theme.ink, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 13.sp else 10.sp,
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(theme.accentHigh, theme.accent, theme.accentDeep)), RoundedCornerShape(50))
                    .border(1.dp, theme.accentRim, RoundedCornerShape(50))
                    .padding(horizontal = if (big) 14.dp else 9.dp, vertical = if (big) 5.dp else 3.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(theme.woodLight, theme.woodDark)), RoundedCornerShape(6.dp))
                .border(1.dp, theme.woodRim.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = if (big) 5.dp else 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        ) {
            val hand = if (big) SampleHand else SampleHand.take(4)
            hand.forEach { TileView(it, tile) }
        }
    }
}

@Composable
private fun UiPreview(style: UiStyle, wallet: Wallet) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, style.rarity.color.copy(alpha = 0.6f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TableSwatch(style.theme, Modifier.fillMaxWidth(), big = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(style.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                RarityTag(style.rarity, style.motionLabel())
            }
            Spacer(Modifier.width(8.dp))
            when {
                wallet.equippedUiId == style.id -> Text("In use \u2713", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                wallet.owns(style) -> BigButton("Use style", primary = true) { wallet.equip(style) }
                wallet.coins >= style.price -> BigButton("Unlock \u00b7 ${style.price}", primary = true) {
                    if (wallet.unlock(style)) wallet.equip(style)
                }
                else -> BigButton("Need ${style.price - wallet.coins} more", enabled = false) {}
            }
        }
    }
}

@Composable
private fun UiCard(style: UiStyle, wallet: Wallet, selected: Boolean, onClick: () -> Unit) {
    val owned = wallet.owns(style)
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) Gold.copy(alpha = 0.12f) else Panel, shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Gold else Faint, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TableSwatch(style.theme, Modifier.fillMaxWidth().alpha(if (owned) 1f else 0.75f))
        Text(style.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RarityTag(style.rarity)
            Spacer(Modifier.weight(1f))
            when {
                wallet.equippedUiId == style.id -> Text("In use", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                owned -> Text("Owned", color = Soft, fontSize = 12.sp)
                else -> CoinAmount("${style.price}", fontSize = 12.sp)
            }
        }
    }
}

private val SampleHand = listOf(
    Kinds.suited(Suit.DOT, 5), Kinds.suited(Suit.BAM, 3), Kinds.suited(Suit.CRAK, 7),
    Kinds.dragon(Dragon.RED), Kinds.FLOWER, Kinds.JOKER,
)

/** Big preview of the set being looked at, with the unlock / use button. */
@Composable
private fun Preview(set: TileSet, wallet: Wallet) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, set.rarity.color.copy(alpha = 0.6f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CompositionLocalProvider(LocalTileSkin provides set.skin) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                TileBack(38.dp, Modifier.bob(amp = 2.5f, phase = 0f))
                SampleHand.forEachIndexed { i, k -> TileView(k, 38.dp, Modifier.bob(amp = 2.5f, phase = (i + 1) * 0.12f)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(set.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                RarityTag(set.rarity, set.motionLabel())
            }
            Spacer(Modifier.width(8.dp))
            when {
                wallet.equippedId == set.id -> Text("In use ✓", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                wallet.owns(set) -> BigButton("Use set", primary = true) { wallet.equip(set) }
                wallet.coins >= set.price -> BigButton("Unlock · ${set.price}", primary = true) {
                    if (wallet.unlock(set)) wallet.equip(set)
                }
                else -> BigButton("Need ${set.price - wallet.coins} more", enabled = false) {}
            }
        }
    }
}

@Composable
private fun RarityTag(rarity: Rarity, animated: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rarity.label.uppercase(), color = rarity.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        if (animated != null) {
            Text(
                "  \u2726 $animated", color = rarity.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.shine(Color.White, alpha = 0.9f, period = 3f),
            )
        }
    }
}

private fun TileSet.motionLabel(): String? = skin.fx.takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.label }
private fun UiStyle.motionLabel(): String? = theme.motion.takeIf { it != FeltMotion.NONE }?.label

@Composable
private fun SetCard(set: TileSet, wallet: Wallet, selected: Boolean, onClick: () -> Unit) {
    val owned = wallet.owns(set)
    val equipped = wallet.equippedId == set.id
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) Gold.copy(alpha = 0.12f) else Panel, shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Gold else Faint, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalTileSkin provides set.skin) {
            Row(
                Modifier.fillMaxWidth().alpha(if (owned) 1f else 0.75f),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                TileBack(40.dp)
                TileView(Kinds.suited(Suit.DOT, 5), 40.dp)
                TileView(Kinds.JOKER, 40.dp)
            }
        }
        Text(set.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RarityTag(set.rarity)
            Spacer(Modifier.weight(1f))
            when {
                equipped -> Text("In use", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                owned -> Text("Owned", color = Soft, fontSize = 12.sp)
                else -> CoinAmount("${set.price}", fontSize = 12.sp)
            }
        }
    }
}
