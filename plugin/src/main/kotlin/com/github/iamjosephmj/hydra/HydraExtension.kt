package com.github.iamjosephmj.hydra

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * hydra DSL. Forwarded onto the underlying `deviceintelligence {}` extension by
 * [HydraPlugin]. Mirrors the subset of options a host app cares about.
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
}
