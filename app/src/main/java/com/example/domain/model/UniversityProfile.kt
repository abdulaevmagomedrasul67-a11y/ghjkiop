package com.example.domain.model

data class UniversityProfile(
    val id: String = "default",
    val fullName: String = "Московский государственный технический университет имени Н.Э. Баумана",
    val shortName: String = "МГТУ им. Н.Э. Баумана",
    val faculty: String = "Информатика и системы управления",
    val department: String = "Программная инженерия",
    val city: String = "Москва",
    val standard: String = "ГОСТ 7.32-2017 (ФГОС ВО 3++)"
)
