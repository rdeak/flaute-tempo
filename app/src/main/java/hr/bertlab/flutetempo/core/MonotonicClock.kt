package hr.bertlab.flutetempo.core

/** A monotonic millisecond clock, abstracted so ViewModels can be unit-tested with fake time. */
fun interface MonotonicClock {
    fun nowMillis(): Long
}
