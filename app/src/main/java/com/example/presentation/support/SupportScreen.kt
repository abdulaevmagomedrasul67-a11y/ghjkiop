package com.example.presentation.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.di.AppContainer
import com.example.data.remote.dto.SupportTicketDto
import com.example.domain.model.SupportCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategory: SupportCategory? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var tickets by remember { mutableStateOf<List<SupportTicketDto>>(emptyList()) }
    var isLoadingTickets by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val loadTickets: () -> Unit = {
        scope.launch {
            isLoadingTickets = true
            val result = appContainer.getMySupportTicketsUseCase()
            if (result.isSuccess) {
                tickets = result.getOrNull() ?: emptyList()
            }
            isLoadingTickets = false
        }
    }

    LaunchedEffect(Unit) {
        loadTickets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Поддержка",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("support_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Новое обращение") },
                    icon = { Icon(Icons.Default.AddComment, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        loadTickets()
                    },
                    text = {
                        BadgedBox(
                            badge = {
                                if (tickets.isNotEmpty()) {
                                    Badge { Text(tickets.size.toString()) }
                                }
                            }
                        ) {
                            Text("Мои обращения")
                        }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                NewTicketScreen(
                    appContainer = appContainer,
                    initialCategory = initialCategory,
                    onTicketCreated = {
                        loadTickets()
                        selectedTab = 1
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MyTicketsScreen(
                    tickets = tickets,
                    isLoading = isLoadingTickets,
                    onRefresh = loadTickets,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
