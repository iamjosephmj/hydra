package com.example.hydrasample

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * A single outbound-egress decision observed by Poseidon, flattened to the few
 * fields the UI renders. Kept free of Poseidon types so it stays trivial to hold,
 * test, and display.
 */
data class EgressEntry(
    val tier: String,
    val host: String,
    val blocked: Boolean,
)

/**
 * Process-wide sink for Poseidon egress events.
 *
 * [HydraSampleApp] registers a Poseidon `Observer` sink that funnels every
 * decision here; the Compose UI collects [flow] and renders it. Poseidon calls
 * sinks on an arbitrary thread, so updates go through [MutableStateFlow.update]
 * (atomic, thread-safe). The list is capped so a chatty app can't grow it without
 * bound.
 */
object EgressLog {
    private const val MAX_ENTRIES = 100

    private val _flow = MutableStateFlow<List<EgressEntry>>(emptyList())
    val flow: StateFlow<List<EgressEntry>> = _flow

    fun record(tier: String, host: String, blocked: Boolean) {
        val entry = EgressEntry(tier = tier, host = host, blocked = blocked)
        _flow.update { current -> (current + entry).takeLast(MAX_ENTRIES) }
    }
}
