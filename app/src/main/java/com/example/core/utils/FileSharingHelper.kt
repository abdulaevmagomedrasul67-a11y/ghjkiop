package com.example.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Утилита для экспорта, отправки и открытия сгенерированных .docx файлов через FileProvider.
 */
object FileSharingHelper {

    private const val DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Поделиться файлом .docx через системное меню (Telegram, WhatsApp, почта, Google Диск).
     */
    fun shareDocx(context: Context, filePath: String?, topic: String) {
        if (filePath.isNullOrBlank()) {
            Toast.makeText(context, "Файл документа не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Файл не существует на устройстве", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = DOCX_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, topic)
                putExtra(Intent.EXTRA_TEXT, "Сгенерированная работа по ГОСТ: $topic")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Поделиться работой (.docx)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка отправки файла: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Открыть файл .docx в установленном на устройстве приложении (Microsoft Word, Google Документы, WPS).
     */
    fun openDocx(context: Context, filePath: String?) {
        if (filePath.isNullOrBlank()) {
            Toast.makeText(context, "Файл документа не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Файл не существует на устройстве", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, DOCX_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Не найдено приложение для открытия Word документов. Установите Word или Google Документы.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
