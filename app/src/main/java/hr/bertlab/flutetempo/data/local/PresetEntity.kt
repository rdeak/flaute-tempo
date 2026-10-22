package hr.bertlab.flutetempo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import hr.bertlab.flutetempo.domain.model.SoundProfile
import hr.bertlab.flutetempo.domain.model.Subdivision
import hr.bertlab.flutetempo.domain.model.TimeSignature

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bpm: Int,
    @ColumnInfo(name = "time_signature") val timeSignature: TimeSignature,
    val subdivision: Subdivision,
    val sound: SoundProfile,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
