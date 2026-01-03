package com.choreapp.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChoreEntity::class, SyncQueueEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChoreDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: ChoreDatabase? = null

        fun getDatabase(context: Context): ChoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChoreDatabase::class.java,
                    "chore_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}