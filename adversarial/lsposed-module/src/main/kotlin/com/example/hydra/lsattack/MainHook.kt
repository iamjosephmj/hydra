package com.example.hydra.lsattack

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Attack 4 — LSPosed / Xposed ART method hook.
 *
 * LSPosed installs its hooks in-process at Zygote fork via Zygisk, leaving NONE of
 * frida's tells (no `pool-frida` thread, no server port, no RWX page). Installing a
 * hook on a monitored ART method rewrites that method's `ArtMethod` — it flips
 * `ACC_NATIVE` and redirects the JNI entry — exactly the primitive the protected
 * runtime's ART-integrity detector watches for. The callback here is a harmless
 * no-op; the mere *installation* is the attack. A protected app should notice the
 * ART tamper and self-terminate.
 *
 * Scope this module to `com.example.hydrasample` in LSPosed Manager.
 */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lp: XC_LoadPackage.LoadPackageParam) {
        if (lp.packageName != TARGET) return
        runCatching {
            XposedHelpers.findAndHookMethod(
                "java.lang.String", lp.classLoader, "length",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { /* no-op */ }
                },
            )
            XposedBridge.log("[hydra-lsattack] String#length hooked (ArtMethod tampered; no frida artifacts)")
        }.onFailure { XposedBridge.log("[hydra-lsattack] hook failed: ${it.message}") }
    }

    private companion object { const val TARGET = "com.example.hydrasample" }
}
