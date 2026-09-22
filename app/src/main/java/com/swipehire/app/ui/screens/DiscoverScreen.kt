@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipehire.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.AccountType
import androidx.compose.foundation.layout.height
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.RemoteType
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.ui.components.MatchCelebrationOverlay
import com.swipehire.app.ui.components.SwipeCardStack
import com.swipehire.app.ui.components.SwipeDirection
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Mint20
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Sky
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.VioletDeep
import com.swipehire.app.ui.theme.decorativeRings
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.ui.tr
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipehire.app.viewmodel.DiscoverViewModel
import com.swipehire.app.viewmodel.SavedItemsViewModel

private enum class BrowseMode { SWIPE, LIST }

@Composable
fun DiscoverScreen(
    accountType: AccountType,
    onOpenNearbyJobs: () -> Unit = {},
    onViewJobLocation: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    viewModel: DiscoverViewModel = viewModel(),
    savedItemsViewModel: SavedItemsViewModel = viewModel()
) {
    val currentUserId = currentFirebaseUserId()
    if (currentUserId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(tr("Sign in to browse SwipeHire."))
        }
        return
    }
    LaunchedEffect(accountType, currentUserId) {
        viewModel.loadCards(accountType, currentUserId)
        savedItemsViewModel.load(currentUserId)
    }

    if (accountType == AccountType.STUDENT) {
        StudentDiscoverContent(
            onOpenNearbyJobs = onOpenNearbyJobs,
            onViewJobLocation = onViewJobLocation,
            viewModel = viewModel,
            savedItemsViewModel = savedItemsViewModel,
            currentUserId = currentUserId,
            onOpenChat = onOpenChat
        )
    } else {
        CompanyDiscoverContent(
            viewModel = viewModel,
            savedItemsViewModel = savedItemsViewModel,
            currentUserId = currentUserId,
            onOpenChat = onOpenChat
        )
    }
}

// ---------------------------------------------------------------------------
// Student side — swiping on jobs
// ---------------------------------------------------------------------------

@Composable
private fun StudentDiscoverContent(
    onOpenNearbyJobs: () -> Unit,
    onViewJobLocation: (String) -> Unit,
    viewModel: DiscoverViewModel,
    savedItemsViewModel: SavedItemsViewModel,
    currentUserId: String,
    onOpenChat: (String) -> Unit
) {
    val savedIds by savedItemsViewModel.savedJobIds.collectAsState()
    val jobStack by viewModel.jobStack.collectAsState()
    val swipedIds by viewModel.swipedTargetIds.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var remoteFilter by remember { mutableStateOf<RemoteType?>(null) }
    var pendingSwipe by remember { mutableStateOf<SwipeDirection?>(null) }
    var lastAction by remember { mutableStateOf<Pair<JobPosting, SwipeDirection>?>(null) }
    var matchedJob by remember { mutableStateOf<Pair<JobPosting, String>?>(null) }
    var detailJob by remember { mutableStateOf<JobPosting?>(null) }
    var savedOnly by remember { mutableStateOf(false) }

    val rawDeck = jobStack

    val visibleDeck = remember(rawDeck, swipedIds, searchQuery, remoteFilter, savedIds, savedOnly) {
        rawDeck.filter { job ->
            job.id !in swipedIds &&
                    (!savedOnly || job.id in savedIds) &&
                    (remoteFilter == null || job.remoteType == remoteFilter) &&
                    (searchQuery.isBlank() ||
                            job.role.contains(searchQuery, ignoreCase = true) ||
                            job.company.contains(searchQuery, ignoreCase = true) ||
                            job.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }.sortedByDescending { it.matchedSkills.size }
    }

    fun decide(job: JobPosting, direction: SwipeDirection) {
        lastAction = job to direction

        viewModel.onSwipe(
            userId = currentUserId,
            targetId = job.id,
            targetUserId = job.companyId,
            isLike = direction == SwipeDirection.RIGHT,
            onMatchFound = { matchId ->
                if (matchId != null) matchedJob = job to matchId
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(tr("Find your\nnext role"), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Swipe right to apply, left to pass."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Violet40.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onOpenNearbyJobs)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = Violet40, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Near you"), style = MaterialTheme.typography.labelMedium, color = Violet40)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SearchAndFilterBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedRemote = remoteFilter,
                onRemoteSelected = { remoteFilter = it },
                savedOnly = savedOnly,
                savedCount = savedIds.size,
                onSavedOnlyChange = { savedOnly = it }
            )

            Spacer(Modifier.height(10.dp))
            ProgressRow(reviewed = rawDeck.count { it.id in swipedIds }, total = rawDeck.size)

            if (visibleDeck.isEmpty() && (searchQuery.isNotBlank() || remoteFilter != null)) {
                NoResultsMessage(Modifier.weight(1f))
            } else {
                SwipeCardStack(
                    items = visibleDeck,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    pendingSwipe = pendingSwipe,
                    onPendingSwipeHandled = { pendingSwipe = null },
                    onSwiped = { job, direction -> decide(job, direction) },
                    emptyContent = { EmptyDeckMessage() }
                ) { job ->
                    JobCard(
                        job = job,
                        isSaved = job.id in savedIds,
                        onSaveToggle = { savedItemsViewModel.toggleJob(job.id) },
                        onViewLocation = { onViewJobLocation(job.id) },
                        onInfo = { detailJob = job }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 26.dp, top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SwipeActionButton(icon = Icons.Filled.Close, color = Coral, onClick = { pendingSwipe = SwipeDirection.LEFT })
                Spacer(Modifier.width(30.dp))
                SwipeActionButton(icon = Icons.Filled.Favorite, color = Mint40, onClick = { pendingSwipe = SwipeDirection.RIGHT })
            }
        }

        if (lastAction != null && matchedJob == null) {
            UndoPill(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 108.dp),
                onUndo = {
                    val (job, _) = lastAction ?: return@UndoPill
                    viewModel.undoSwipe(currentUserId, job.id, job.companyId)
                    lastAction = null
                }
            )
        }

        MatchCelebrationOverlay(
            visible = matchedJob != null,
            name = matchedJob?.first?.company.orEmpty(),
            subtitle = matchedJob?.first?.role.orEmpty(),
            initials = matchedJob?.first?.logoInitials.orEmpty(),
            isCompanySide = false,
            onKeepSwiping = { matchedJob = null },
            onSendMessage = {
                matchedJob?.second?.let { matchId ->
                    matchedJob = null
                    onOpenChat(matchId)
                }
            }
        )
    }

    detailJob?.let { job ->
        ModalBottomSheet(onDismissRequest = { detailJob = null }, sheetState = rememberModalBottomSheetState()) {
            JobDetailContent(
                job = job,
                isSaved = job.id in savedIds,
                onSaveToggle = { savedItemsViewModel.toggleJob(job.id) },
                onViewLocation = { onViewJobLocation(job.id); detailJob = null }
            )
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedRemote: RemoteType?,
    onRemoteSelected: (RemoteType?) -> Unit,
    savedOnly: Boolean,
    savedCount: Int,
    onSavedOnlyChange: (Boolean) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(tr("Search role, company, or skill")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet40)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedRemote == null,
                    onClick = { onRemoteSelected(null) },
                    label = { Text(tr("All")) }
                )
            }
            items(RemoteType.values().toList()) { type ->
                FilterChip(
                    selected = selectedRemote == type,
                    onClick = { onRemoteSelected(if (selectedRemote == type) null else type) },
                    label = { Text(type.label) }
                )
            }
            item {
                FilterChip(
                    selected = savedOnly,
                    onClick = { onSavedOnlyChange(!savedOnly) },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("${tr("Saved")} ($savedCount)") }
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(reviewed: Int, total: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$reviewed / $total ${tr("reviewed")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = if (total == 0) 0f else reviewed / total.toFloat(),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
            color = Violet40,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun NoResultsMessage(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(tr("No matches for that search"), style = MaterialTheme.typography.titleMedium)
        Text(tr("Try a different keyword or clear filters."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyDeckMessage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Style, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(tr("You're all caught up"), style = MaterialTheme.typography.titleMedium)
        Text(tr("Check back soon for new matches."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UndoPill(modifier: Modifier = Modifier, onUndo: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onUndo),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Undo, contentDescription = null, tint = Violet40, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Undo"), style = MaterialTheme.typography.labelLarge, color = Violet40)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared swipe-card shell + job/student card content
// ---------------------------------------------------------------------------

@Composable
private fun SwipeCardShell(
    gradient: Brush,
    initials: String,
    headerTitle: String,
    headerSubtitle: String,
    accent: Color,
    onInfoClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxSize(),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(gradient)
                    .decorativeRings(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Info, contentDescription = "Full details", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().offset(y = (-20).dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(headerTitle, style = MaterialTheme.typography.titleLarge)
                    Text(headerSubtitle, style = MaterialTheme.typography.titleSmall, color = accent)
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun MatchedSkillsRow(skills: List<String>, accent: Color) {
    if (skills.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Matches you on ${skills.joinToString(", ")}",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JobCard(job: JobPosting, isSaved: Boolean, onSaveToggle: () -> Unit, onViewLocation: () -> Unit, onInfo: () -> Unit) {
    SwipeCardShell(
        gradient = Brush.linearGradient(listOf(Violet40, VioletDeep, Sky.copy(alpha = 0.55f))),
        initials = job.logoInitials,
        headerTitle = job.role,
        headerSubtitle = job.company,
        accent = Violet40,
        onInfoClick = onInfo
    ) {
        MatchedSkillsRow(job.matchedSkills, Violet40)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(3.dp))
                Text(job.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSaveToggle, modifier = Modifier.size(36.dp)) {
                    Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = if (isSaved) "Remove saved job" else "Save job", tint = Violet40)
                }
                Surface(shape = RoundedCornerShape(50), color = Violet40.copy(alpha = 0.12f), modifier = Modifier.clickable(onClick = onViewLocation)) {
                    Text(tr("View location"), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = Violet40)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(3.dp))
            Text(job.salaryRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(job.blurb, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Spacer(Modifier.height(14.dp))
        TagRow(job.tags, Violet40)
    }
}

@Composable
private fun StudentCard(student: StudentProfile, isSaved: Boolean, onSaveToggle: () -> Unit, onInfo: () -> Unit) {
    SwipeCardShell(
        gradient = Brush.linearGradient(listOf(Mint40, Mint20, Sky.copy(alpha = 0.4f))),
        initials = student.avatarInitials,
        headerTitle = student.name,
        headerSubtitle = "${student.course} · ${student.year}",
        accent = Mint40,
        onInfoClick = onInfo
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onSaveToggle) {
                Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = if (isSaved) "Remove saved candidate" else "Save candidate", tint = Mint20)
            }
        }
        MatchedSkillsRow(student.matchedSkills, Mint20)
        Text(student.blurb, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Spacer(Modifier.height(14.dp))
        TagRow(student.skills, Mint20)
    }
}

@Composable
private fun TagRow(tags: List<String>, accent: Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags) { tag ->
            Surface(
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
            ) {
                Text(
                    tag,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun SwipeActionButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(60.dp).glow(color, radiusMultiplier = 2.4f, alpha = 0.4f),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = color
        )
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Detail bottom sheets
// ---------------------------------------------------------------------------

@Composable
private fun JobDetailContent(job: JobPosting, isSaved: Boolean, onSaveToggle: () -> Unit, onViewLocation: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(job.role, style = MaterialTheme.typography.headlineSmall)
        Text(job.company, style = MaterialTheme.typography.titleMedium, color = Violet40)
        Spacer(Modifier.height(14.dp))
        DetailRow(Icons.Filled.LocationOn, job.location)
        DetailRow(Icons.Filled.Payments, job.salaryRange)
        Spacer(Modifier.height(14.dp))
        Text(job.blurb, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text(tr("Requirements"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        TagRow(job.tags, Violet40)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSaveToggle, modifier = Modifier.fillMaxWidth()) {
            Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isSaved) "Remove from saved" else "Save for later")
        }
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Violet40,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onViewLocation)
        ) {
            Text(
                "View on map",
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StudentDetailContent(student: StudentProfile, isSaved: Boolean, onSaveToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(student.name, style = MaterialTheme.typography.headlineSmall)
        Text("${student.course} · ${student.year}", style = MaterialTheme.typography.titleMedium, color = Mint40)
        Spacer(Modifier.height(14.dp))
        Text(student.blurb, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text(tr("Skills"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        TagRow(student.skills, Mint20)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSaveToggle, modifier = Modifier.fillMaxWidth()) {
            Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isSaved) "Remove from saved" else "Save for later")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------
// Company side — swiping (or listing) students
// ---------------------------------------------------------------------------

@Composable
private fun CompanyDiscoverContent(
    viewModel: DiscoverViewModel,
    savedItemsViewModel: SavedItemsViewModel,
    currentUserId: String,
    onOpenChat: (String) -> Unit
) {
    val savedIds by savedItemsViewModel.savedStudentIds.collectAsState()
    val studentStack by viewModel.studentStack.collectAsState()
    val swipedIds by viewModel.swipedTargetIds.collectAsState()
    var pendingSwipe by remember { mutableStateOf<SwipeDirection?>(null) }
    var lastAction by remember { mutableStateOf<Pair<StudentProfile, SwipeDirection>?>(null) }
    var matchedStudent by remember { mutableStateOf<Pair<StudentProfile, String>?>(null) }
    var detailStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var mode by remember { mutableStateOf(BrowseMode.SWIPE) }
    var savedOnly by remember { mutableStateOf(false) }

    val rawDeck = studentStack

    val visibleDeck = remember(rawDeck, swipedIds, savedIds, savedOnly) {
        rawDeck.filter { it.id !in swipedIds && (!savedOnly || it.id in savedIds) }
            .sortedByDescending { it.matchedSkills.size }
    }

    fun decide(student: StudentProfile, direction: SwipeDirection) {
        lastAction = student to direction

        viewModel.onSwipe(
            userId = currentUserId,
            targetId = student.id,
            targetUserId = student.id,
            isLike = direction == SwipeDirection.RIGHT,
            onMatchFound = { matchId ->
                if (matchId != null) matchedStudent = student to matchId
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(tr("Find your\nnext hire"), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Swipe right to shortlist, left to pass."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { mode = if (mode == BrowseMode.SWIPE) BrowseMode.LIST else BrowseMode.SWIPE }) {
                    Icon(
                        if (mode == BrowseMode.SWIPE) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                        contentDescription = "Toggle view",
                        tint = Mint40
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            ProgressRow(reviewed = rawDeck.count { it.id in swipedIds }, total = rawDeck.size)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                FilterChip(
                    selected = savedOnly,
                    onClick = { savedOnly = !savedOnly },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("${tr("Saved")} (${savedIds.size})") }
                )
            }

            when (mode) {
                BrowseMode.SWIPE -> SwipeCardStack(
                    items = visibleDeck,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    pendingSwipe = pendingSwipe,
                    onPendingSwipeHandled = { pendingSwipe = null },
                    onSwiped = { student, direction -> decide(student, direction) },
                    emptyContent = { EmptyDeckMessage() }
                ) { student ->
                    StudentCard(
                        student = student,
                        isSaved = student.id in savedIds,
                        onSaveToggle = { savedItemsViewModel.toggleStudent(student.id) },
                        onInfo = { detailStudent = student }
                    )
                }

                BrowseMode.LIST -> if (visibleDeck.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyDeckMessage() }
                } else {
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        items(visibleDeck, key = { it.id }) { student ->
                            CandidateListRow(
                                student = student,
                                onPass = { decide(student, SwipeDirection.LEFT) },
                                onLike = { decide(student, SwipeDirection.RIGHT) },
                                isSaved = student.id in savedIds,
                                onSaveToggle = { savedItemsViewModel.toggleStudent(student.id) },
                                onClick = { detailStudent = student }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            if (mode == BrowseMode.SWIPE) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 26.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SwipeActionButton(icon = Icons.Filled.Close, color = Coral, onClick = { pendingSwipe = SwipeDirection.LEFT })
                    Spacer(Modifier.width(30.dp))
                    SwipeActionButton(icon = Icons.Filled.Favorite, color = Mint40, onClick = { pendingSwipe = SwipeDirection.RIGHT })
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        if (lastAction != null && matchedStudent == null) {
            UndoPill(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (mode == BrowseMode.SWIPE) 108.dp else 24.dp),
                onUndo = {
                    val (student, _) = lastAction ?: return@UndoPill
                    viewModel.undoSwipe(currentUserId, student.id, student.id)
                    lastAction = null
                }
            )
        }

        MatchCelebrationOverlay(
            visible = matchedStudent != null,
            name = matchedStudent?.first?.name.orEmpty(),
            subtitle = matchedStudent?.first?.course.orEmpty(),
            initials = matchedStudent?.first?.avatarInitials.orEmpty(),
            isCompanySide = true,
            onKeepSwiping = { matchedStudent = null },
            onSendMessage = {
                matchedStudent?.second?.let { matchId ->
                    matchedStudent = null
                    onOpenChat(matchId)
                }
            }
        )
    }

    detailStudent?.let { student ->
        ModalBottomSheet(onDismissRequest = { detailStudent = null }, sheetState = rememberModalBottomSheetState()) {
            StudentDetailContent(
                student = student,
                isSaved = student.id in savedIds,
                onSaveToggle = { savedItemsViewModel.toggleStudent(student.id) }
            )
        }
    }
}

@Composable
private fun CandidateListRow(
    student: StudentProfile,
    onPass: () -> Unit,
    onLike: () -> Unit,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Mint40.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(student.avatarInitials, color = Mint40, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium)
                    Text("${student.course} · ${student.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSaveToggle) {
                    Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = if (isSaved) "Remove saved candidate" else "Save candidate", tint = Mint40)
                }
            }
            if (student.matchedSkills.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                MatchedSkillsRow(student.matchedSkills, Mint20)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onPass,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Coral.copy(alpha = 0.12f))
                ) { Icon(Icons.Filled.Close, contentDescription = "Pass", tint = Coral, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Mint40.copy(alpha = 0.12f))
                ) { Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = Mint40, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
