package com.snap.tiles.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snap.tiles.R
import com.snap.tiles.data.Action
import com.snap.tiles.data.remote.ActionRegistry
import com.snap.tiles.data.remote.RemoteAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionScreen(alreadySelected: Set<Action>, onBack: () -> Unit, onActionsSelected: (Set<Action>) -> Unit) {
    var selected by remember { mutableStateOf(alreadySelected) }
    // Use top-level tree actions grouped by category
    val topLevelByCategory = remember { ActionRegistry.getTopLevelByCategory() }
    val remoteToEnum = remember { Action.entries.associateBy { it.name } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_action_title), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) } },
                actions = {
                    Button(
                        onClick = { onActionsSelected(selected) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.padding(end = 8.dp)
                    ) { Text(stringResource(R.string.btn_save), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 80.dp)
        ) {
            topLevelByCategory.entries.forEachIndexed { catIndex, (category, rootActions) ->
                if (catIndex > 0) {
                    Spacer(Modifier.height(32.dp))
                    Text(category.label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(stringResource(R.string.available_toggles), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(category.label, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                    Spacer(Modifier.height(24.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rootActions.forEach { remote ->
                        ActionTreeNode(
                            remote = remote,
                            remoteToEnum = remoteToEnum,
                            selected = selected,
                            onSelectionChanged = { selected = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTreeNode(
    remote: RemoteAction,
    remoteToEnum: Map<String, Action>,
    selected: Set<Action>,
    onSelectionChanged: (Set<Action>) -> Unit
) {
    val enumAction = remoteToEnum[remote.id] ?: return
    val isSelected = enumAction in selected
    val hasChildren = remote.safeChildren.isNotEmpty()
    var expanded by remember { mutableStateOf(isSelected && hasChildren) }

    // Collect all child enum actions for this parent
    val childEnums = remember(remote) {
        remote.safeChildren.mapNotNull { remoteToEnum[it.id] }
    }
    val selectedChildCount = childEnums.count { it in selected }

    Column {
        // Parent card
        ActionLibraryCard(
            remote = remote,
            action = enumAction,
            isSelected = isSelected,
            childInfo = if (hasChildren) "$selectedChildCount/${childEnums.size}" else null,
            isExpanded = expanded,
            onToggle = {
                if (isSelected) {
                    // Deselect parent + all children
                    onSelectionChanged(selected - enumAction - childEnums.toSet())
                } else {
                    // Select parent only (children are opt-in)
                    onSelectionChanged(selected + enumAction)
                    if (hasChildren) expanded = true
                }
            },
            onExpandToggle = if (hasChildren) {{ expanded = !expanded }} else null
        )

        // Children (indented)
        if (hasChildren) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                remote.safeChildren.forEach { child ->
                        val childEnum = remoteToEnum[child.id] ?: return@forEach
                        val childSelected = childEnum in selected
                        ChildActionCard(
                            remote = child,
                            action = childEnum,
                            isSelected = childSelected,
                            onToggle = {
                                if (childSelected) {
                                    onSelectionChanged(selected - childEnum)
                                } else {
                                    // Auto-select parent if not selected
                                    val newSet = selected + childEnum +
                                        (if (!isSelected) setOf(enumAction) else emptySet())
                                    onSelectionChanged(newSet)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionLibraryCard(
    remote: RemoteAction,
    action: Action,
    isSelected: Boolean,
    childInfo: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpandToggle: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (isSelected) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.primary))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(remote.safeEmoji.ifEmpty { getActionEmoji(action) }, fontSize = 20.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(remote.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(remote.safeDescription, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (childInfo != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$childInfo sub-actions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (onExpandToggle != null) {
                    IconButton(onClick = onExpandToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(40.dp), shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = onToggle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSelected) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = stringResource(if (isSelected) R.string.cd_remove else R.string.cd_add),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ChildActionCard(
    remote: RemoteAction,
    action: Action,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (isSelected) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colorScheme.tertiary))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(remote.safeEmoji.ifEmpty { getActionEmoji(action) }, fontSize = 16.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(remote.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(remote.safeDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    modifier = Modifier.size(32.dp), shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = onToggle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSelected) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = stringResource(if (isSelected) R.string.cd_remove else R.string.cd_add),
                            tint = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getActionEmoji(action: Action): String = when (action) {
    Action.STAY_AWAKE -> "☀️"; Action.RUNNING_SERVICES -> "🧠"; Action.FORCE_RTL -> "↔️"
    Action.PROFILE_GPU -> "📊"; Action.USB_DEBUGGING -> "🔌"; Action.DEMO_MODE -> "📱"
    Action.ANIMATOR_SCALE -> "⚡"; Action.DEVELOPER_MODE -> "🛠️"; Action.ACCESSIBILITY -> "♿"
}
