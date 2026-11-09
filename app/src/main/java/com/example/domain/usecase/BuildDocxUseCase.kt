package com.example.domain.usecase

import com.example.data.docx.DocxGenerator
import com.example.domain.model.WorkDocument
import java.io.File

/**
 * UseCase компиляции документа в файл .docx со строгим соблюдением требований ГОСТ и ФГОС.
 */
class BuildDocxUseCase(
    private val docxGenerator: DocxGenerator
) {
    suspend operator fun invoke(document: WorkDocument): Result<String> {
        return docxGenerator.generateDocx(document)
    }

    suspend operator fun invoke(document: WorkDocument, profileId: Long): Result<File> {
        return docxGenerator.generateDocx(document).map { path -> File(path) }
    }
}
