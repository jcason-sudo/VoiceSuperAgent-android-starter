package io.livekit.android.example.voiceassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.livekit.android.LiveKit
import io.livekit.android.example.voiceassistant.fromVsaGateway
import io.livekit.android.example.voiceassistant.screen.VoiceAssistantRoute
import io.livekit.android.example.voiceassistant.vsaGatewayUrl
import io.livekit.android.token.TokenSource
import io.livekit.android.token.cached

/**
 * This ViewModel handles holding onto the Room object, so that it is
 * maintained across configuration changes, such as rotation.
 */
class VoiceAssistantViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val room = LiveKit.create(application)

    val tokenSource: TokenSource

    init {
        val (sandboxId, url, token) = savedStateHandle.toRoute<VoiceAssistantRoute>()

        tokenSource = when {
            // VSA gateway path — preferred. Set vsaGatewayUrl in TokenExt.kt.
            vsaGatewayUrl.isNotEmpty() ->
                TokenSource.fromVsaGateway().cached()
            sandboxId.isNotEmpty() ->
                TokenSource.fromSandboxTokenServer(sandboxId = sandboxId).cached()
            else ->
                TokenSource.fromLiteral(url, token).cached()
        }
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()
    }
}