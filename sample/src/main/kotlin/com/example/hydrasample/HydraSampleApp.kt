package com.example.hydrasample

import android.app.Application
import tech.ssemaj.poseidon.runtime.model.EgressEvent
import tech.ssemaj.poseidon.runtime.pipeline.Observer

/**
 * Registers a Poseidon egress sink at process start so the in-app audit panel can
 * show what the app talks to on the wire.
 *
 * Poseidon is applied as a build-time Gradle plugin and self-initialises via its
 * own `InitializationProvider`; all this class does is attach a sink that maps
 * each [EgressEvent] into the UI-facing [EgressLog]. Registering here (rather than
 * in the Activity) captures any event that fires before the UI exists.
 */
class HydraSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Observer.addSink { event: EgressEvent ->
            EgressLog.record(
                tier = event.tier.name,
                // host can be null for a bare-IP connection Poseidon couldn't map
                // back to a name; fall back to the IP, then a placeholder.
                host = event.host ?: event.ip ?: "(unknown)",
                // A null decision means pass-through / allowed; only an explicit
                // block counts as blocked.
                blocked = event.decision?.block == true,
            )
        }
    }
}
