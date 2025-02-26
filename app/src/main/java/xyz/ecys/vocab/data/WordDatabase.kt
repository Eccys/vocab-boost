package xyz.ecys.vocab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Word::class, AppUsage::class], version = 17, exportSchema = true) // Increment version to 17
abstract class WordDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile
        private var INSTANCE: WordDatabase? = null

        // Add migration from version 16 to 17
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the exampleSentence column with a default empty string
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN exampleSentence TEXT NOT NULL DEFAULT ''
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 1
                """)
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN lastReviewed INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN timesReviewed INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN timesCorrect INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_words_word 
                    ON words(word)
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS words_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        synonym TEXT NOT NULL,
                        synonymDefinition TEXT NOT NULL,
                        synonymExampleSentence TEXT NOT NULL,
                        antonym TEXT NOT NULL,
                        antonymDefinition TEXT NOT NULL,
                        exampleSentence TEXT NOT NULL,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        difficulty INTEGER NOT NULL DEFAULT 1,
                        lastReviewed INTEGER NOT NULL DEFAULT 0,
                        timesReviewed INTEGER NOT NULL DEFAULT 0,
                        timesCorrect INTEGER NOT NULL DEFAULT 0,
                        nextReviewTime INTEGER NOT NULL DEFAULT 0,
                        intervalDays INTEGER NOT NULL DEFAULT 1,
                        easeFactor REAL NOT NULL DEFAULT 2.5
                    )
                """)
                database.execSQL("""
                    INSERT INTO words_temp (
                        id, word, definition, synonym, synonymDefinition, 
                        synonymExampleSentence, antonym, antonymDefinition, 
                        exampleSentence, isBookmarked, difficulty,
                        lastReviewed, timesReviewed, timesCorrect
                    )
                    SELECT 
                        id, word, definition, synonym, synonymDefinition,
                        synonymExampleSentence, antonym, antonymDefinition,
                        exampleSentence, isBookmarked, difficulty,
                        lastReviewed, timesReviewed, timesCorrect
                    FROM words
                """)
                database.execSQL("DROP TABLE words")
                database.execSQL("ALTER TABLE words_temp RENAME TO words")
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_words_word 
                    ON words(word)
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage (
                        date INTEGER NOT NULL PRIMARY KEY
                    )
                """)
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE app_usage 
                    ADD COLUMN duration INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE app_usage 
                    ADD COLUMN sessionCount INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5")
                database.execSQL("ALTER TABLE words ADD COLUMN interval INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE words ADD COLUMN repetitionCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE words ADD COLUMN nextReviewDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage_temp (
                        date INTEGER NOT NULL PRIMARY KEY,
                        duration INTEGER NOT NULL DEFAULT 0,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        correctAnswers INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO app_usage_temp (date, duration, sessionCount, correctAnswers)
                    SELECT date, duration, sessionCount, COALESCE(correctAnswers, 0) FROM app_usage
                """)
                database.execSQL("DROP TABLE app_usage")
                database.execSQL("ALTER TABLE app_usage_temp RENAME TO app_usage")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN fastResponses INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN mediumResponses INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words 
                    ADD COLUMN slowResponses INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage_temp (
                        date INTEGER NOT NULL PRIMARY KEY,
                        duration INTEGER NOT NULL DEFAULT 0,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        correctAnswers INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO app_usage_temp (date, duration, sessionCount, correctAnswers)
                    SELECT date, duration, sessionCount, correctAnswers FROM app_usage
                """)
                database.execSQL("DROP TABLE app_usage")
                database.execSQL("ALTER TABLE app_usage_temp RENAME TO app_usage")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS words_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        synonym1 TEXT NOT NULL,
                        synonym1Definition TEXT NOT NULL,
                        synonym1ExampleSentence TEXT NOT NULL,
                        synonym2 TEXT NOT NULL,
                        synonym2Definition TEXT NOT NULL,
                        synonym2ExampleSentence TEXT NOT NULL,
                        synonym3 TEXT NOT NULL,
                        synonym3Definition TEXT NOT NULL,
                        synonym3ExampleSentence TEXT NOT NULL,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        lastReviewed INTEGER NOT NULL DEFAULT 0,
                        timesReviewed INTEGER NOT NULL DEFAULT 0,
                        timesCorrect INTEGER NOT NULL DEFAULT 0,
                        nextReviewTime INTEGER NOT NULL DEFAULT 0,
                        intervalDays INTEGER NOT NULL DEFAULT 1,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        fastResponses INTEGER NOT NULL DEFAULT 0,
                        mediumResponses INTEGER NOT NULL DEFAULT 0,
                        slowResponses INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO words_temp (
                        id, word, definition, 
                        synonym1, synonym1Definition, synonym1ExampleSentence,
                        synonym2, synonym2Definition, synonym2ExampleSentence,
                        synonym3, synonym3Definition, synonym3ExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        nextReviewTime, intervalDays, easeFactor,
                        fastResponses, mediumResponses, slowResponses
                    )
                    SELECT 
                        id, word, definition,
                        synonym, synonymDefinition, synonymExampleSentence,
                        synonym, synonymDefinition, synonymExampleSentence,
                        synonym, synonymDefinition, synonymExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        nextReviewTime, intervalDays, easeFactor,
                        fastResponses, mediumResponses, slowResponses
                    FROM words
                """)
                database.execSQL("DROP TABLE words")
                database.execSQL("ALTER TABLE words_temp RENAME TO words")
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_words_word 
                    ON words(word)
                """)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS words_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        synonym1 TEXT NOT NULL,
                        synonym1Definition TEXT NOT NULL,
                        synonym1ExampleSentence TEXT NOT NULL,
                        synonym2 TEXT NOT NULL,
                        synonym2Definition TEXT NOT NULL,
                        synonym2ExampleSentence TEXT NOT NULL,
                        synonym3 TEXT NOT NULL,
                        synonym3Definition TEXT NOT NULL,
                        synonym3ExampleSentence TEXT NOT NULL,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        lastReviewed INTEGER NOT NULL DEFAULT 0,
                        timesReviewed INTEGER NOT NULL DEFAULT 0,
                        timesCorrect INTEGER NOT NULL DEFAULT 0,
                        nextReviewTime INTEGER NOT NULL DEFAULT 0,
                        intervalDays INTEGER NOT NULL DEFAULT 1,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        fastResponses INTEGER NOT NULL DEFAULT 0,
                        mediumResponses INTEGER NOT NULL DEFAULT 0,
                        slowResponses INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO words_temp (
                        id, word, definition,
                        synonym1, synonym1Definition, synonym1ExampleSentence,
                        synonym2, synonym2Definition, synonym2ExampleSentence,
                        synonym3, synonym3Definition, synonym3ExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        nextReviewTime, intervalDays, easeFactor,
                        fastResponses, mediumResponses, slowResponses
                    )
                    SELECT 
                        id, word, definition,
                        synonym1, synonym1Definition, synonym1ExampleSentence,
                        synonym2, synonym2Definition, synonym2ExampleSentence,
                        synonym3, synonym3Definition, synonym3ExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        nextReviewTime, intervalDays, easeFactor,
                        fastResponses, mediumResponses, slowResponses
                    FROM words
                """)
                database.execSQL("DROP TABLE words")
                database.execSQL("ALTER TABLE words_temp RENAME TO words")
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_words_word 
                    ON words(word)
                """)
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a temporary table with the new schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS words_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        synonym1 TEXT NOT NULL,
                        synonym1Definition TEXT NOT NULL,
                        synonym1ExampleSentence TEXT NOT NULL,
                        synonym2 TEXT NOT NULL,
                        synonym2Definition TEXT NOT NULL,
                        synonym2ExampleSentence TEXT NOT NULL,
                        synonym3 TEXT NOT NULL,
                        synonym3Definition TEXT NOT NULL,
                        synonym3ExampleSentence TEXT NOT NULL,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        lastReviewed INTEGER NOT NULL DEFAULT 0,
                        timesReviewed INTEGER NOT NULL DEFAULT 0,
                        timesCorrect INTEGER NOT NULL DEFAULT 0,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        interval INTEGER NOT NULL DEFAULT 0,
                        repetitionCount INTEGER NOT NULL DEFAULT 0,
                        nextReviewDate INTEGER NOT NULL DEFAULT 0,
                        quality INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Copy data from the old table to the new one, mapping old column names to new ones
                database.execSQL("""
                    INSERT INTO words_temp (
                        id, word, definition,
                        synonym1, synonym1Definition, synonym1ExampleSentence,
                        synonym2, synonym2Definition, synonym2ExampleSentence,
                        synonym3, synonym3Definition, synonym3ExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        easeFactor, interval, nextReviewDate, quality
                    )
                    SELECT 
                        id, word, definition,
                        synonym1, synonym1Definition, synonym1ExampleSentence,
                        synonym2, synonym2Definition, synonym2ExampleSentence,
                        synonym3, synonym3Definition, synonym3ExampleSentence,
                        isBookmarked, lastReviewed, timesReviewed, timesCorrect,
                        easeFactor, intervalDays, nextReviewTime, 0
                    FROM words
                """)

                // Update repetitionCount based on timesReviewed
                database.execSQL("""
                    UPDATE words_temp
                    SET repetitionCount = CASE
                        WHEN timesReviewed > 0 THEN 1
                        ELSE 0
                    END
                """)

                // Drop the old table
                database.execSQL("DROP TABLE words")

                // Rename the temporary table to the original name
                database.execSQL("ALTER TABLE words_temp RENAME TO words")

                // Recreate the index
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_words_word 
                    ON words(word)
                """)
            }
        }

        fun getDatabase(context: Context): WordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "word_database"
                )
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}