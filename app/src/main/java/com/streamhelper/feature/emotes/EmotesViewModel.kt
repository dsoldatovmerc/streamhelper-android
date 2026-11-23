package com.streamhelper.feature.emotes

import androidx.lifecycle.ViewModel
import com.streamhelper.feature.emotes.data.EmoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the loaded emote sets to chat message rendering, so rows recompose (and earlier
 * messages pick up emotes) when the sets arrive or are cleared.
 */
@HiltViewModel
class EmotesViewModel @Inject constructor(repository: EmoteRepository) : ViewModel() {
    val emotes: StateFlow<EmoteSets> = repository.emotes
}
