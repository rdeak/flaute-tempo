package hr.bertlab.fluteassistant.domain.model

/** A named, saved metronome configuration (e.g. "Song 1 - Live"). */
data class Preset(
    val id: Long,
    val name: String,
    val config: MetronomeConfig,
    val createdAt: Long,
)
