package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-сущность сохранённой академической работы.
 * Сохраняет метаданные, JSON-контент документа и путь к сгенерированному файлу .docx.
 */
@Entity(tableName = "works_history")
data class WorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val typeId: String,
    val topic: String,
    val subject: String,
    val language: String = "ru",
    val targetPages: Int = 15,
    val documentJson: String,
    val docxFilePath: String? = null,
    val profileName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
