package com.example.presentation.support.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.SupportTicketDto
import com.example.domain.model.SupportCategories
import com.example.ui.components.AppDivider
import com.example.ui.components.KickerBadge
import com.example.ui.theme.StudToolTheme

@Composable
fun TicketCard(
    ticket: SupportTicketDto,
    modifier: Modifier = Modifier
) {
    val colors = StudToolTheme.colors
    val typography = StudToolTheme.typography

    var isExpanded by remember { mutableStateOf(false) }

    val category = SupportCategories.findByValue(ticket.category)
    val categoryLabel = category?.label ?: ticket.category

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(colors.paperWarm)
            .border(1.dp, colors.line, RoundedCornerShape(4.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(14.dp)
            .testTag("ticket_card_${ticket.id}")
    ) {
        Column {
            // Верхняя строка: Категория, Статус и ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KickerBadge(text = categoryLabel.uppercase())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${ticket.id}",
                        style = typography.codeMono.copy(
                            color = colors.inkFaint,
                            fontSize = 11.sp
                        )
                    )
                }

                StatusBadge(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Тема обращения
            Text(
                text = ticket.subject,
                style = typography.titleSerif.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Сообщение (сжатое или развернутое)
            Text(
                text = if (isExpanded) ticket.message else ticket.message.take(80) + if (ticket.message.length > 80) "..." else "",
                style = typography.bodySmall.copy(color = colors.inkSoft, lineHeight = 18.sp)
            )

            // Дата создания и привязка к работе
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.createdAt,
                    style = typography.metaMono.copy(color = colors.inkFaint, fontSize = 11.sp)
                )

                if (ticket.workId != null && ticket.workId > 0) {
                    Text(
                        text = "Работа ID: ${ticket.workId}",
                        style = typography.codeMono.copy(color = colors.inkFaint, fontSize = 11.sp)
                    )
                }
            }

            // Блок ответа администратора
            if (!ticket.adminReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AppDivider(color = colors.line)
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.paper, RoundedCornerShape(3.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(3.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ОТВЕТ ПОДДЕРЖКИ",
                                style = typography.labelMono.copy(
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (!ticket.processedAt.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "· ${ticket.processedAt}",
                                    style = typography.metaMono.copy(color = colors.inkFaint, fontSize = 10.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = ticket.adminReply,
                            style = typography.bodySmall.copy(color = colors.ink, fontWeight = FontWeight.Medium)
                        )

                        if (ticket.refundEskoins > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF16A34A).copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                    .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Обращение решено · подписка активна",
                                    style = typography.labelMono.copy(
                                        color = Color(0xFF15803D),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
