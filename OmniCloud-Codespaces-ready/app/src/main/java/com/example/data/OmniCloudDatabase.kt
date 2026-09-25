package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CloudProviderEntity
import com.example.model.CloudProviderType
import com.example.model.FileCloudSyncEntity
import com.example.model.FileTypeCategory
import com.example.model.ProjectFolderEntity
import com.example.model.SyncLogEntity
import com.example.model.VaultFileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VaultFileEntity::class,
        ProjectFolderEntity::class,
        CloudProviderEntity::class,
        FileCloudSyncEntity::class,
        SyncLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class OmniCloudDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun projectDao(): ProjectDao
    abstract fun cloudProviderDao(): CloudProviderDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: OmniCloudDatabase? = null

        fun getDatabase(context: Context, coroutineScope: CoroutineScope): OmniCloudDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniCloudDatabase::class.java,
                    "omnicloud_vault.db"
                )
                    .addCallback(DatabaseCallback(coroutineScope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: OmniCloudDatabase) {
                // Pre-populate Default Projects
                val defaultProjects = listOf(
                    ProjectFolderEntity(
                        name = "Personal Vault",
                        description = "Strictly encrypted private records & identity",
                        colorHex = 0xFF2563EB,
                        iconName = "Security",
                        isDefault = true
                    ),
                    ProjectFolderEntity(
                        name = "Work Documents",
                        description = "Contracts, reports, spreadsheets and project notes",
                        colorHex = 0xFF059669,
                        iconName = "Work",
                        isDefault = false
                    ),
                    ProjectFolderEntity(
                        name = "Cloud Backups",
                        description = "System snapshots, database dumps, archives",
                        colorHex = 0xFF7C3AED,
                        iconName = "CloudSync",
                        isDefault = false
                    )
                )
                db.projectDao().insertProject(defaultProjects[0])
                db.projectDao().insertProject(defaultProjects[1])
                db.projectDao().insertProject(defaultProjects[2])
                // Clean startup: No cloud providers pre-connected. User selects and authorizes them on demand.
            }
        }
    }
}
