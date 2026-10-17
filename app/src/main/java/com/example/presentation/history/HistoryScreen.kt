package com.example.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.core.utils.FileSharingHelper
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkType
import com.example.ui.components.AnimatedScaleCard
import com.example.ui.components.EmptyState
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onPreviewWork: (WorkDocument) -> Unit = {},
    showBackButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = showBackButton, onBack = onBack)

    val allWorks by appContainer.workRepository.getAllWorks().collectAsState(initial = emptyList())
    var selectedType by remember { mutableStateOf<WorkType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filteredWorks = remember(allWorks, selectedType, searchQuery) {
        allWorks.filter { work ->
            val matchesType = selectedType == null || work.meta.type == selectedType
            val matchesSearch = searchQuery.isBlank() ||
                work.meta.topic.contains(searchQuery, ignoreCase = true) ||
                work.meta.university.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Архив работ (${allWorks.size})",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "История генерации · Предпросмотр и экспорт .docx",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                }
            )
        },
        modifier = modifier.testTag("history_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по теме или ВУЗу...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("Все (${allWorks.size})") }
                )
                WorkType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type },
                        label = { Text(type.titleRu) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredWorks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "Работы не найдены",
                        description = "Все сгенерированные работы и файлы Word (.docx) сохраняются в этом архиве."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredWorks, key = { it.id }) { work ->
                        WorkHistoryCard(
                            work = work,
                            onPreview = { onPreviewWork(work) },
                            onOpenDocx = {
                                scope.launch {
                                    val path = work.docxPath ?: appContainer.buildDocxUseCase(work).getOrNull()?.also {
                                        appContainer.workRepository.updateDocxPath(work.id, it)
                                    }
                                    FileSharingHelper.openDocx(context, path)
                                }
                            },
                            onShareDocx = {
                                scope.launch {
                                    val path = work.docxPath ?: appContainer.buildDocxUseCase(work).getOrNull()?.also {
                                        appContainer.workRepository.updateDocxPath(work.id, it)
                                    }
                                    FileSharingHelper.shareDocx(context, path, work.meta.topic)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    appContainer.workRepository.deleteWork(work.id)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WorkHistoryCard(
    work: WorkDocument,
    onPreview: () -> Unit,
    onOpenDocx: () -> Unit,
    onShareDocx: () -> Unit,
    onDelete: () -> Unit
) {
    AnimatedScaleCard(
        onClick = onPreview,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = work.meta.type.titleRu.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${work.meta.targetPages} стр. · ${work.sections.size} разд.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = work.meta.topic,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${work.meta.university} · ${work.meta.authorName}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Просмотр", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenDocx,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(".docx", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                IconButton(onClick = onShareDocx) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться .docx",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
