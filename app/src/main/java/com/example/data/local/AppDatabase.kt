package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.UniversityProfileDao
import com.example.data.local.dao.WorkDao
import com.example.data.local.entity.UniversityProfileEntity
import com.example.data.local.entity.WorkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Основная база данных приложения StudTool (Room).
 */
@Database(
    entities = [
        UniversityProfileEntity::class,
        WorkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun universityProfileDao(): UniversityProfileDao
    abstract fun workDao(): WorkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studtool_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultProfiles(database.universityProfileDao())
                    }
                }
            }
        }

        suspend fun populateDefaultProfiles(dao: UniversityProfileDao) {
            if (dao.countProfiles() > 0) return

            val defaults = listOf(
                UniversityProfileEntity(
                    id = 1L,
                    name = "ГОСТ 7.32-2017 (Стандарт РФ)",
                    universityName = "Министерство науки и высшего образования Российской Федерации",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 15f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "ГОСТ Р 7.0.100-2018",
                    isDefault = true
                ),
                UniversityProfileEntity(
                    id = 2L,
                    name = "МГУ им. М.В. Ломоносова",
                    universityName = "Московский государственный университет имени М.В. Ломоносова",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 15f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "ГОСТ Р 7.0.100-2018",
                    isDefault = false
                ),
                UniversityProfileEntity(
                    id = 3L,
                    name = "НИУ ВШЭ (Высшая школа экономики)",
                    universityName = "Национальный исследовательский университет «Высшая школа экономики»",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 15f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "APA / ГОСТ",
                    isDefault = false
                ),
                UniversityProfileEntity(
                    id = 4L,
                    name = "СПбГУ",
                    universityName = "Санкт-Петербургский государственный университет",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 10f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "ГОСТ Р 7.0.100-2018",
                    isDefault = false
                ),
                UniversityProfileEntity(
                    id = 5L,
                    name = "МГТУ им. Н.Э. Баумана",
                    universityName = "Московский государственный технический университет имени Н.Э. Баумана",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 15f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "ГОСТ 7.32",
                    isDefault = false
                ),
                UniversityProfileEntity(
                    id = 6L,
                    name = "ГПК (Гуманитарно-педагогический колледж)",
                    universityName = "ПРОФЕССИОНАЛЬНОЕ ОБРАЗОВАТЕЛЬНОЕ ЧАСТНОЕ УЧРЕЖДЕНИЕ «ГУМАНИТАРНО-ПЕДАГОГИЧЕСКИЙ КОЛЛЕДЖ»",
                    facultyName = "",
                    departmentName = "",
                    leftMarginMm = 30f,
                    rightMarginMm = 15f,
                    topMarginMm = 20f,
                    bottomMarginMm = 20f,
                    fontName = "Times New Roman",
                    fontSizePt = 14f,
                    lineSpacing = 1.5f,
                    paragraphIndentMm = 12.5f,
                    startPageNumbering = 2,
                    bibliographyFormat = "ГОСТ Р 7.0.100-2018",
                    isDefault = false
                )
            )

            if (dao.countProfiles() == 0) {
                dao.insertAll(defaults)
            } else {
                if (dao.getProfileById(6L) == null) {
                    dao.insert(defaults.last())
                }
            }
        }
    }
}
