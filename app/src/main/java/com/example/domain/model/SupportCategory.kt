package com.example.domain.model

enum class SupportCategory(
    val code: String,
    val titleRu: String,
    val descriptionRu: String
) {
    REFUND(
        code = "refund",
        titleRu = "Возврат коинов",
        descriptionRu = "Работа не сгенерировалась, а S-coin списались с баланса"
    ),
    PASSWORD(
        code = "password",
        titleRu = "Забыл пароль",
        descriptionRu = "Потерян доступ к аккаунту или привязанной почте"
    ),
    BUG(
        code = "bug",
        titleRu = "Нашёл ошибку",
        descriptionRu = "Приложение работает некорректно или зависает"
    ),
    QUESTION(
        code = "question",
        titleRu = "Вопрос",
        descriptionRu = "Не понимаю, как работает генератор, форматирование или тарифы"
    ),
    OTHER(
        code = "other",
        titleRu = "Другое",
        descriptionRu = "Индивидуальный вопрос в свободной форме"
    );

    companion object {
        fun fromCode(code: String): SupportCategory =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OTHER
    }
}

data class SupportCategoryItem(val value: String, val label: String, val description: String)

object SupportCategories {
    val all: List<SupportCategoryItem> = SupportCategory.entries.map {
        SupportCategoryItem(value = it.code, label = it.titleRu, description = it.descriptionRu)
    }

    fun findByValue(value: String): SupportCategoryItem? =
        all.firstOrNull { it.value.equals(value, ignoreCase = true) }
}
