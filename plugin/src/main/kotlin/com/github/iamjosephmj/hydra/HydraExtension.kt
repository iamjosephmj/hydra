package com.github.iamjosephmj.hydra

import org.gradle.api.Action
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * hydra DSL. Forwarded onto the underlying `deviceintelligence {}` extension by
 * [HydraPlugin]; the `secrets {}` block is handled by hydra itself.
 */
abstract class HydraExtension {
    /** Configuration-time verbosity. */
    abstract val verbose: Property<Boolean>

    /** Extra exact-match String constants to dex-encrypt (needs -Pdi.dexstrings=true). */
    abstract val encryptStrings: SetProperty<String>

    /** Inject ACCESS_NETWORK_STATE so runtime VPN detection can populate. */
    abstract val enableVpnDetection: Property<Boolean>

    /** Inject USE_BIOMETRIC so runtime biometric-enrollment detection can populate. */
    abstract val enableBiometricsDetection: Property<Boolean>

    /**
     * Named secrets, retrieved at runtime via `Hydra.secret("name")`. Each value
     * is encrypted at build time with a fresh per-build key (derived in the
     * closed baker, byte-identical to the native derivation); only ciphertext is
     * generated into the app, and decryption happens through the obfuscated
     * native runtime at the point of use.
     */
    abstract val secrets: MapProperty<String, String>

    /** DSL sugar: `hydra { secrets { put("apiUrl", "https://...") } }`. */
    fun secrets(action: Action<SecretsHandler>) {
        action.execute(SecretsHandler(secrets))
    }

    class SecretsHandler(private val backing: MapProperty<String, String>) {
        fun put(name: String, value: String) {
            backing.put(name, value)
        }
    }
}
