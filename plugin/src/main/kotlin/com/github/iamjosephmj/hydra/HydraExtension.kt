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

    /**
     * Asset relative paths (under `assets/`) to encrypt at build time. The
     * plaintext is removed from the APK; only ciphertext + a per-build seed ship.
     * Retrieved at runtime via `Hydra.asset(context, "name")`, which decrypts only
     * after the first clean detection sweep (same gating as [secrets]).
     */
    abstract val encryptAssets: SetProperty<String>

    /** DSL sugar: `hydra { encryptAssets { include("config.json") } }`. */
    fun encryptAssets(action: Action<AssetsHandler>) {
        action.execute(AssetsHandler(encryptAssets))
    }

    class AssetsHandler(private val backing: SetProperty<String>) {
        fun include(vararg paths: String) {
            for (p in paths) backing.add(p)
        }
    }

    /**
     * Opt-in App Bundle integrity ("bundle mode") for apps shipped as `.aab`
     * through Play. Forwarded onto `deviceintelligence { appBundle { ... } }`.
     */
    @get:org.gradle.api.tasks.Nested
    abstract val appBundle: HydraAppBundleOptions

    /** DSL sugar: `hydra { appBundle { enabled = true; playSigningCertSha256("...") } }`. */
    fun appBundle(action: Action<HydraAppBundleOptions>) = action.execute(appBundle)

    abstract class HydraAppBundleOptions {
        /** Enable bundle mode for AAB builds. Default `false`. */
        abstract val enabled: Property<Boolean>

        /**
         * Play App Signing cert SHA-256(s) to pin (normalized to lowercase hex,
         * `:` stripped). Copy from Play Console → App integrity → App signing key
         * certificate. The upload-key signer is added automatically.
         */
        abstract val playSigningCertSha256: SetProperty<String>

        /** DSL sugar: `appBundle { playSigningCertSha256("AB:CD:...") }`. */
        fun playSigningCertSha256(vararg hex: String) {
            for (h in hex) playSigningCertSha256.add(h.replace(":", "").lowercase())
        }
    }
}
