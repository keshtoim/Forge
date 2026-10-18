package com.keshtoim.forge.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiStatus {
    // The assistant is off or has no key: nothing to show.
    data object Off : AiStatus
    data object Offline : AiStatus
    data object Checking : AiStatus
    data object Ready : AiStatus
    data class Failed(val failure: AiFailure) : AiStatus
}

// Every request to the AI goes through here, so the status always reflects how the last one went.
// [baseUrl] is only for tests, as in [AiAssistant].
class AiConnection(
    context: Context,
    private val settings: AiSettings,
    private val scope: CoroutineScope,
    private val baseUrl: String? = null,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val online = MutableStateFlow(isOnline())

    // How the last request went for a given configuration; a new key or model starts out unverified.
    private val outcome = MutableStateFlow<Pair<AiConfig, AiFailure?>?>(null)
    private val checking = MutableStateFlow(false)

    init {
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online.value = true
            }

            override fun onLost(network: Network) {
                online.value = isOnline()
            }
        })
    }

    val status: StateFlow<AiStatus> = combine(settings.state, online, outcome, checking) { state, isOnline, last, busy ->
        val config = state.config
        when {
            config == null -> AiStatus.Off
            !isOnline -> AiStatus.Offline
            busy || last?.first != config -> AiStatus.Checking
            else -> last.second?.let { AiStatus.Failed(it) } ?: AiStatus.Ready
        }
    }.stateIn(scope, SharingStarted.Eagerly, AiStatus.Off)

    // Confirms the current key and model once, unless a request already did.
    fun verify() {
        val config = settings.state.value.config ?: return
        if (!online.value || checking.value || outcome.value?.first == config) return
        checking.value = true
        scope.launch {
            runCatching { record(config) { AiAssistant.ping(config, baseUrl) } }
            checking.value = false
        }
    }

    suspend fun ask(system: String, turns: List<AiTurn>): String {
        val config = settings.state.value.config ?: throw AiException(AiFailure.BAD_KEY)
        return record(config) { AiAssistant.reply(config, system, turns, baseUrl) }
    }

    private suspend fun <T> record(config: AiConfig, block: suspend () -> T): T = try {
        block().also { outcome.value = config to null }
    } catch (e: AiException) {
        // A dropped connection says nothing about the key; the network callback reports it instead.
        if (e.failure != AiFailure.NO_CONNECTION) outcome.value = config to e.failure
        throw e
    }

    private fun isOnline() = connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
