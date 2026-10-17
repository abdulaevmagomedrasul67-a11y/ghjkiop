package com.example.presentation.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.domain.model.SupportCategory
import kotlinx.coroutines.launch

@Composable
fun NewTicketScreen(
    appContainer: AppContainer,
    onTicketCreated: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategory: SupportCategory? = null
) {
    var selectedCategory by remember { mutableStateOf(initialCategory ?: SupportCategory.QUESTION) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf(appContainer.sessionManager.getUserEmail()) }
    var selectedWorkId by remember { mutableStateOf<Long?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val userWorks by appContainer.workRepository.getAllWorks().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ВЫБЕРИТЕ КАТЕГОРИЮ ОБРАЩЕНИЯ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        SupportCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = if (isSelected) 2.dp else 0.8.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedCategory = category }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.titleRu,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = category.descriptionRu,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Селектор работы при возврате коинов
        AnimatedVisibility(visible = selectedCategory == SupportCategory.REFUND) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "ПРИВЯЗКА К СГЕНЕРИРОВАННОЙ РАБОТЕ (ОПЦИОНАЛЬНО)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                var workDropdownExpanded by remember { mutableStateOf(false) }
                val selectedWork = userWorks.firstOrNull { it.id == selectedWorkId }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedWork?.meta?.topic ?: "Выберите работу из списка...",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { workDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = workDropdownExpanded,
                        onDismissRequest = { workDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Без привязки к конкретной работе") },
                            onClick = {
                                selectedWorkId = null
                                workDropdownExpanded = false
                            }
                        )
                        userWorks.forEach { work ->
                            DropdownMenuItem(
                                text = { Text("${work.meta.type.titleRu}: ${work.meta.topic}") },
                                onClick = {
                                    selectedWorkId = work.id
                                    workDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "ТЕМА ОБРАЩЕНИЯ (ДО 100 СИМВОЛОВ)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = subject,
            onValueChange = { if (it.length <= 100) subject = it },
            placeholder = { Text("Краткая суть проблемы") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_ticket_subject"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ПОДРОБНОЕ ОПИСАНИЕ (ДО 2000 СИМВОЛОВ)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { if (it.length <= 2000) message = it },
            placeholder = { Text("Опишите подробности возникшей ситуации...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("input_ticket_message"),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "КОНТАКТНЫЙ EMAIL ДЛЯ ОТВЕТА",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = contactEmail,
            onValueChange = { contactEmail = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (subject.isBlank() || message.isBlank()) {
                    errorMessage = "Заполните тему и текст обращения"
                    return@Button
                }
                scope.launch {
                    isSubmitting = true
                    errorMessage = null
                    val result = appContainer.createSupportTicketUseCase(
                        category = selectedCategory,
                        subject = subject.trim(),
                        message = message.trim(),
                        workId = selectedWorkId,
                        contactEmail = contactEmail.trim()
                    )
                    isSubmitting = false
                    if (result.isSuccess) {
                        successMessage = "Обращение успешно отправлено! Номер тикета: #${result.getOrNull()}"
                        subject = ""
                        message = ""
                        onTicketCreated()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Ошибка при отправке обращения"
                    }
                }
            },
            enabled = !isSubmitting && subject.isNotBlank() && message.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_submit_ticket"),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Отправить обращение", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
