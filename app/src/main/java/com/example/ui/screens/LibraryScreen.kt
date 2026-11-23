package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.data.model.LibrarySection
import com.example.data.model.SortMode
import com.example.ui.components.GameCoverArt
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors
import com.example.ui.viewmodel.LibraryFilters

@Composable
fun LibraryScreen(
    games: List<GameEntity>,
    categories: List<String>,
    filters: LibraryFilters,
    isArabic: Boolean,
    strings: AppStrings,
    onSystemSelected: (AtariSystem) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSectionSelected: (LibrarySection) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onOpenGameDetail: (GameEntity) -> Unit,
    onToggleFavorite: (GameEntity) -> Unit,
    onOpenImportSheet: () -> Unit,
    onOpenTelegram: () -> Unit = {},
    onNavigateSettings: () -> Unit
) {
    val colors = LocalAtariColors.current
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Top Header Bar (matches style board: Title + Search icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (filters.section) {
                    LibrarySection.ALL -> strings.appTitle
                    LibrarySection.FAVORITES -> strings.navFavorites
                    LibrarySection.RECENT -> strings.navRecent
                    LibrarySection.MOST_PLAYED -> strings.topPlayed
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.cream
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenTelegram,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("library_telegram_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = strings.telegramChannelsTitle,
                        tint = colors.orange
                    )
                }

                IconButton(
                    onClick = onOpenImportSheet,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("library_add_games_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.addGames,
                        tint = colors.amber
                    )
                }

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("library_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = strings.sortLabel,
                            tint = colors.amber
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = colors.card
                    ) {
                        SortMode.entries.forEach { mode ->
                            val selected = filters.sortMode == mode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isArabic) mode.labelAr else mode.labelEn,
                                        color = if (selected) colors.orange else colors.cream,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortChanged(mode)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) onSearchChanged("")
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("library_search_toggle")
                ) {
                    Icon(
                        imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = strings.searchPlaceholder,
                        tint = colors.amber
                    )
                }
            }
        }

        // Expandable Instant Search Field
        AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text(strings.searchPlaceholder, color = colors.muted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.amber)
                },
                trailingIcon = {
                    if (filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.orange,
                    unfocusedBorderColor = colors.divider,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card,
                    focusedTextColor = colors.cream,
                    unfocusedTextColor = colors.cream
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("library_search_input")
            )
        }

        // System Pill Filter Chips (الكل / 2600 / 5200 / 7800 / Lynx / Jaguar)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AtariSystem.entries, key = { it.code }) { sys ->
                val selected = filters.system == sys
                FilterPillChip(
                    label = if (isArabic) sys.labelAr else sys.labelEn,
                    selected = selected,
                    onClick = { onSystemSelected(sys) },
                    modifier = Modifier.testTag("system_chip_${sys.code}")
                )
            }
        }

        // Category & Quick-Section Chips + Live Game Counter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "count_badge") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${games.size} ${strings.gamesCountSuffix}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = colors.amber
                    )
                }
            }

            item(key = "top_played_toggle") {
                val isTop = filters.section == LibrarySection.MOST_PLAYED
                FilterPillChip(
                    label = strings.topPlayed,
                    selected = isTop,
                    onClick = {
                        onSectionSelected(if (isTop) LibrarySection.ALL else LibrarySection.MOST_PLAYED)
                    },
                    compact = true,
                    modifier = Modifier.testTag("section_most_played_chip")
                )
            }

            item(key = "cat_all") {
                FilterPillChip(
                    label = strings.allCategories,
                    selected = filters.category == "ALL",
                    onClick = { onCategorySelected("ALL") },
                    compact = true,
                    modifier = Modifier.testTag("category_chip_all")
                )
            }

            items(categories, key = { it }) { cat ->
                FilterPillChip(
                    label = cat,
                    selected = filters.category == cat,
                    onClick = { onCategorySelected(cat) },
                    compact = true,
                    modifier = Modifier.testTag("category_chip_$cat")
                )
            }
        }

        // Virtualized 2-Column / Adaptive Grid
        Box(modifier = Modifier.weight(1f)) {
            if (games.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = colors.amber,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.emptyLibraryTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.cream
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.emptyLibraryHint,
                        fontSize = 13.sp,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("games_virtual_grid")
                ) {
                    items(
                        items = games,
                        key = { it.id }
                    ) { game ->
                        GameGridCard(
                            game = game,
                            isArabic = isArabic,
                            onClick = { onOpenGameDetail(game) },
                            onToggleFavorite = { onToggleFavorite(game) }
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar matching the style board
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(vertical = 8.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavIcon(
                selected = filters.section == LibrarySection.ALL || filters.section == LibrarySection.MOST_PLAYED,
                filledIcon = Icons.Rounded.Home,
                outlinedIcon = Icons.Outlined.Home,
                contentDescription = strings.navHome,
                onClick = { onSectionSelected(LibrarySection.ALL) },
                modifier = Modifier.testTag("nav_home")
            )
            BottomNavIcon(
                selected = filters.section == LibrarySection.FAVORITES,
                filledIcon = Icons.Rounded.Favorite,
                outlinedIcon = Icons.Outlined.FavoriteBorder,
                contentDescription = strings.navFavorites,
                onClick = { onSectionSelected(LibrarySection.FAVORITES) },
                modifier = Modifier.testTag("nav_favorites")
            )
            BottomNavIcon(
                selected = filters.section == LibrarySection.RECENT,
                filledIcon = Icons.Rounded.History,
                outlinedIcon = Icons.Outlined.History,
                contentDescription = strings.navRecent,
                onClick = { onSectionSelected(LibrarySection.RECENT) },
                modifier = Modifier.testTag("nav_recent")
            )
            BottomNavIcon(
                selected = false,
                filledIcon = Icons.Rounded.Settings,
                outlinedIcon = Icons.Outlined.Settings,
                contentDescription = strings.navSettings,
                onClick = onNavigateSettings,
                modifier = Modifier.testTag("nav_settings")
            )
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = LocalAtariColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.orange else colors.chip)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 6.dp else 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else colors.cream
        )
    }
}

@Composable
private fun GameGridCard(
    game: GameEntity,
    isArabic: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = LocalAtariColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("game_card_${game.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GameCoverArt(
                coverStyle = game.coverStyle,
                coverChar = game.coverChar,
                paletteIndex = game.paletteIndex,
                cornerRadius = 12.dp,
                letterSize = 36.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
            )

            // System badge (subtle top-start)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xAA120F0C))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = game.system,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = colors.amber
                )
            }

            if (game.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA120F0C))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = colors.orange,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isArabic) game.nameAr else game.nameEn,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BottomNavIcon(
    selected: Boolean,
    filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = if (selected) filledIcon else outlinedIcon,
            contentDescription = contentDescription,
            tint = if (selected) colors.orange else colors.muted,
            modifier = Modifier.size(26.dp)
        )
    }
}
