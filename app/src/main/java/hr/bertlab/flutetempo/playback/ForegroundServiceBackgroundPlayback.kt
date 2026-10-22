package hr.bertlab.flutetempo.playback

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Starts [MetronomeService], which keeps the process in the foreground until playback ends. */
class ForegroundServiceBackgroundPlayback @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackgroundPlayback {

    override fun start() {
        try {
            ContextCompat.startForegroundService(context, MetronomeService.intent(context))
        } catch (e: IllegalStateException) {
            // Android 12+ refuses foreground services started while the app is in the background.
            // Playback itself is unaffected; it just will not outlive the screen.
            Log.w(TAG, "Could not start the playback service: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "BackgroundPlayback"
    }
}
