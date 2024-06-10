package se.warting.signaturecore

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DBEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
