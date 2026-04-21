package me.shirobyte42.glosso.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * One-shot cleanup for users upgrading from v2.0 (Allosaurus EN + wav2vec2 FR) to v2.1
 * (unified eSpeak wav2vec2 + new DB schema). Runs once, gated by the `migrated_to_v10`
 * shared pref. Rewrites v2.0 "en" language rows in the progress DB to "en_GB" so mastery
 * /streaks/reviews carry forward into the new dual-English scheme.
 */
object MigrationRunner {

    private const val TAG = "MigrationRunner"
    private const val PREFS_FILE = "glosso_prefs"
    private const val FLAG_KEY = "migrated_to_v11b"
    private const val PROGRESS_DB_NAME = "glosso_progress_db"

    private val OBSOLETE_MODEL_FILES = listOf(
        "allosaurus_en.onnx",
        "allosaurus_eng2102.onnx",      // pre-2.0 name
        "phone_en.txt",
        "phone_eng.txt",                // pre-2.0 name
        "wav2vec2_fr_phoneme_int8.onnx",
        "vocab.json",                    // FR tokenizer vocab (conflicts with new espeak_vocab.json)
        "config.json",
        "tokenizer_config.json",
        "special_tokens_map.json",
        "preprocessor_config.json",
        "added_tokens.json",
    )

    private val OBSOLETE_DB_REGEX = Regex(
        """^sentences(_(en|fr))?_\d+\.db(-shm|-wal)?$|^sentences_v10_.*\.db(-shm|-wal)?$"""
    )

    fun runIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(FLAG_KEY, false)) return

        Log.i(TAG, "Running v2.0 → v2.1 cleanup")
        deleteObsoleteModelFiles(context)
        deleteObsoleteSentenceDbs(context)
        migratePrefLanguage(prefs)
        migrateProgressDbLanguage(context)

        prefs.edit().putBoolean(FLAG_KEY, true).apply()
        Log.i(TAG, "Cleanup complete")
    }

    private fun migratePrefLanguage(prefs: android.content.SharedPreferences) {
        val current = prefs.getString("target_language", null)
        if (current == "en") {
            prefs.edit().putString("target_language", "en_GB").apply()
            Log.d(TAG, "Migrated target_language pref: en → en_GB")
        }
    }

    private fun migrateProgressDbLanguage(context: Context) {
        val dbFile = context.getDatabasePath(PROGRESS_DB_NAME)
        if (!dbFile.exists()) {
            Log.d(TAG, "Progress DB not present — skipping language migration")
            return
        }
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
            val tables = listOf("mastered_sentences", "activity_days", "review_queue")
            tables.forEach { table ->
                try {
                    db.execSQL("UPDATE $table SET language = 'en_GB' WHERE language = 'en'")
                } catch (t: Throwable) {
                    Log.w(TAG, "Skip table $table: ${t.message}")
                }
            }
            Log.d(TAG, "Migrated progress DB rows: language en → en_GB")
        } catch (t: Throwable) {
            Log.w(TAG, "Progress DB language migration failed: ${t.message}")
        } finally {
            db?.close()
        }
    }

    private fun deleteObsoleteModelFiles(context: Context) {
        OBSOLETE_MODEL_FILES.forEach { name ->
            val f = File(context.filesDir, name)
            if (f.exists() && f.delete()) {
                Log.d(TAG, "Deleted obsolete model asset: $name")
            }
        }
    }

    private fun deleteObsoleteSentenceDbs(context: Context) {
        val dbDir = context.getDatabasePath("placeholder").parentFile ?: return
        if (!dbDir.exists()) return
        dbDir.listFiles()?.forEach { file ->
            if (OBSOLETE_DB_REGEX.matches(file.name)) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted obsolete sentence DB: ${file.name}")
                }
            }
        }
    }
}
