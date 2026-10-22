package hr.bertlab.flutetempo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PresetEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class FluteTempoDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        const val NAME = "flutetempo.db"
    }
}
