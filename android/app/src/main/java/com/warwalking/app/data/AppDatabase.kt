package com.warwalking.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WalkSessionEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walkSessionDao(): WalkSessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "warwalker.db"
            )
                // No real user data to preserve at this stage (pre-release,
                // test devices only) - a proper Migration should replace this
                // before shipping anything people would mind losing.
                .fallbackToDestructiveMigration(true)
                .build().also { instance = it }
        }
    }
}
