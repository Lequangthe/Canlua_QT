package com.quangthe.canluav3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RiceTicket::class,
        RiceSheet::class,
        RiceCell::class,
        AppSettings::class
    ],
    version = 6,
    exportSchema = false
)
abstract class RiceDatabase : RoomDatabase() {
    abstract fun riceDao(): RiceDao

    companion object {
        @Volatile
        private var INSTANCE: RiceDatabase? = null

        fun getDatabase(context: Context): RiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RiceDatabase::class.java,
                    "rice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
