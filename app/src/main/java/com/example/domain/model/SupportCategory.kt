package com.example.domain.model

enum class SupportCategory(
    val code: String,
    val titleRu: String,
    val descriptionRu: String,
    val icon: String = "•",
    val hint: String = "Опишите ваш вопрос подробно..."
) {
    REFUND(
        code = "refund",
        titleRu = "Подписка и оплата",
        descriptionRu = "Вопросы по активации подписки, продлению или возврату средств",
        icon = "₽",
        hint = "Укажите дату перевода и выбранный тариф подписки..."
    ),
    PASSWORD(
        code = "password",
        titleRu = "Забыл пароль",
        descriptionRu = "Потерян доступ к аккаунту или привязанной почте",
        icon = "🔑",
        hint = "Укажите логин или почту вашего аккаунта..."
    ),
    BUG(
        code = "bug",
        titleRu = "Нашёл ошибку",
        descriptionRu = "Приложение работает некорректно или зависает",
        icon = "⚙",
        hint = "Опишите, на каком этапе возникла ошибка..."
    ),
    QUESTION(
        code = "question",
        titleRu = "Вопрос",
        descriptionRu = "Не понимаю, как работает генератор, форматирование или тарифы",
        icon = "?",
        hint = "Задайте ваш вопрос в свободной форме..."
    ),
    OTHER(
        code = "other",
        titleRu = "Другое",
        descriptionRu = "Индивидуальный вопрос в свободной форме",
        icon = "✉",
        hint = "Опишите вашу ситуацию..."
    );

    val value: String get() = code
    val label: String get() = titleRu
    val subtitle: String get() = descriptionRu

    companion object {
        fun fromCode(code: String): SupportCategory =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OTHER
    }
}

object SupportCategories {
    val ALL: List<SupportCategory> = SupportCategory.entries

    fun findByValue(value: String): SupportCategory? =
        SupportCategory.entries.firstOrNull {
            it.code.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
}
