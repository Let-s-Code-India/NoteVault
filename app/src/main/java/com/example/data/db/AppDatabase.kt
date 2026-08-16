package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DiagramEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TagReminderRuleEntity
import com.example.data.model.TaskEntity
import com.example.data.security.SecurityManager
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TaskEntity::class,
        DiagramEntity::class,
        ReminderEntity::class,
        TagReminderRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun taskDao(): TaskDao
    abstract fun diagramDao(): DiagramDao
    abstract fun reminderDao(): ReminderDao
    abstract fun tagReminderRuleDao(): TagReminderRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            try {
                net.sqlcipher.database.SQLiteDatabase.loadLibs(appContext)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            val passphrase = SecurityManager.getDatabasePassphrase(appContext)
            val factory = SupportFactory(passphrase)

            fun createDb(): AppDatabase {
                return Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "notevault_db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
            }

            val db = createDb()
            return try {
                // Test writable access to ensure database is properly encrypted and accessible
                db.openHelper.writableDatabase
                db
            } catch (e: Exception) {
                // If database failed to open (e.g. existing unencrypted SQLite database), delete and recreate
                e.printStackTrace()
                try {
                    db.close()
                } catch (ignored: Exception) {}
                appContext.deleteDatabase("notevault_db")

                val freshDb = createDb()
                try {
                    freshDb.openHelper.writableDatabase
                } catch (ignored: Exception) {}
                freshDb
            }
        }
    }
}
