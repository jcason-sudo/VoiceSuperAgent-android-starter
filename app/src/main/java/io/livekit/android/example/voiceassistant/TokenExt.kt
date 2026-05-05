package io.livekit.android.example.voiceassistant

import io.livekit.android.token.TokenSource
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ──────────────────────────────────────────────────────────────────────────
// VoiceSuperAgent gateway token fetch
// ──────────────────────────────────────────────────────────────────────────
// Set vsaGatewayUrl to your gateway's POST endpoint and the app will fetch
// token + server URL from there. Leave blank to fall back to the LiveKit
// sandbox flow (sandboxID) or hardcoded values below.
//
// Tailscale (recommended for dev — phone needs Tailscale app installed and
// logged into the same tailnet as the dev box):
//   const val vsaGatewayUrl = "http://100.110.57.122:8088/v1/voice/token"
//
// Public IP (works without Tailscale; requires gateway port reachable from
// the internet):
//   const val vsaGatewayUrl = "http://YOUR.PUBLIC.IP:8088/v1/voice/token"
//
// Production: switch to https with a real cert (Caddy, Cloudflare, etc.).
// ──────────────────────────────────────────────────────────────────────────
const val vsaGatewayUrl = "http://100.110.57.122:8088/v1/voice/token"

// Voice backend: "livekit" (existing path, default) or "pipecat" (OpenAI
// Realtime API; requires the pipecat worker running locally).
const val vsaVoiceBackend = "livekit"

// User identifier — anything you like; shows up in worker logs and metadata.
const val vsaUserId = "phone-jcason"

// TODO: Add your Sandbox ID here (only used if vsaGatewayUrl is blank)
const val sandboxID = ""

// NOTE: If you prefer not to use the token server for testing, you can generate your
// tokens manually by visiting https://cloud.livekit.io/projects/p_/settings/keys
// and using one of your API Keys to generate a token with custom TTL and permissions.
const val hardcodedUrl = ""
const val hardcodedToken = ""

private val httpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

/**
 * Fetch a LiveKit room token from the VoiceSuperAgent gateway.
 * POST <vsaGatewayUrl> {"user_id":..., "voice_backend":...}
 * Returns Pair<serverUrl, participantToken>.
 */
@Throws(Exception::class)
fun fetchVsaToken(
    gatewayUrl: String = vsaGatewayUrl,
    userId: String = vsaUserId,
    voiceBackend: String = vsaVoiceBackend,
): Pair<String, String> {
    val body = JSONObject().apply {
        put("user_id", userId)
        put("voice_backend", voiceBackend)
    }.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(gatewayUrl)
        .post(body)
        .build()

    httpClient.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
            error("Token fetch failed: HTTP ${resp.code} ${resp.message}")
        }
        val payload = JSONObject(resp.body?.string().orEmpty())
        val serverUrl = payload.optString("serverUrl").ifEmpty { error("missing serverUrl") }
        val token = payload.optString("participantToken").ifEmpty { error("missing participantToken") }
        return serverUrl to token
    }
}

/**
 * TokenSource that re-fetches from the gateway on demand. Use .cached() to
 * reuse for the room lifetime.
 */
fun TokenSource.Companion.fromVsaGateway(
    gatewayUrl: String = vsaGatewayUrl,
    userId: String = vsaUserId,
    voiceBackend: String = vsaVoiceBackend,
): TokenSource = TokenSource {
    val (serverUrl, token) = fetchVsaToken(gatewayUrl, userId, voiceBackend)
    TokenSource.Result(url = serverUrl, token = token)
}
