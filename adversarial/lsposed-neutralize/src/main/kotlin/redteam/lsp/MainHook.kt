package redteam.lsp

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * red-team LSPosed module (research / transparency only). Java-boundary neutralizations of
 * the DeviceIntelligence runtime, scoped to com.example.hydrasample.
 *
 * Modes (adb shell setprop hydra.rt.lsp <mode>):
 *
 *   starve-fp  (default) — hook `FrameworkShim.q` and return empty for op 6 (the
 *              fingerprint asset up-call). This is the exact A1 neutralization: the
 *              build-baked .text hash never arrives, so the G2 build layer would go
 *              dark. CERTIFIES A1 — the runtime should read the asset natively from
 *              the APK anyway (fingerprint-recovery log), keep G2 armed, and still
 *              kill on a real .so mismatch. A survive here means A1 regressed.
 *
 *   noop-orch  — hook `io.ssemaj.dx.K.o()` (the native orchestrator entry, registered
 *              via RegisterNatives) to return 0, so the entire detector sweep never
 *              runs. This is the strongest Java-boundary funnel neutralization: the
 *              ART-integrity check that would notice this very hook runs INSIDE the
 *              sweep it just skipped (circular). The only independent backstop is the
 *              forked watchdog. TEST: does anything still kill when orchestration is
 *              skipped at its entry? If the app survives, that is a real gap — the
 *              watchdog deadman fires on heartbeat failure, not on "the sweep never
 *              ran", so this measures whether an entry-point hook is a live bypass.
 *
 * Both hooks target `external fun` native methods on Kotlin `object` singletons, so
 * findAndHookMethod by class name applies to the single instance.
 */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lp: XC_LoadPackage.LoadPackageParam) {
        if (lp.packageName != TARGET) return
        val mode = prop("hydra.rt.lsp", "starve-fp")
        XposedBridge.log("$TAG loaded; mode=$mode")
        when (mode) {
            "noop-orch" -> hookOrchestrator(lp)
            else        -> starveFingerprint(lp)
        }
    }

    /** A1 test: FrameworkShim.q(op, arg) — return ByteArray(0) for op 6. */
    private fun starveFingerprint(lp: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                SHIM, lp.classLoader, "q",
                Int::class.javaPrimitiveType, Any::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[0] as Int == 6) {
                            param.result = ByteArray(0)   // starve the fingerprint asset
                            XposedBridge.log("$TAG starve-fp: op 6 -> ByteArray(0)")
                        }
                    }
                },
            )
            XposedBridge.log("$TAG hooked $SHIM.q (starve op 6)")
        }.onFailure { XposedBridge.log("$TAG starveFingerprint failed: ${it.message}") }
    }

    /** Funnel-at-entry test: io.ssemaj.dx.K.o() -> 0, skipping the whole sweep. */
    private fun hookOrchestrator(lp: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                ANCHOR, lp.classLoader, "o",
                XC_MethodReplacement.returnConstant(0),
            )
            XposedBridge.log("$TAG noop-orch: $ANCHOR.o() -> 0 (orchestration skipped at entry)")
        }.onFailure { XposedBridge.log("$TAG hookOrchestrator failed: ${it.message}") }
    }

    private fun prop(key: String, def: String): String = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java)
            .invoke(null, key, def) as String
    }.getOrDefault(def)

    private companion object {
        const val TARGET = "com.example.hydrasample"
        const val TAG = "[di-rt-lsp]"
        const val SHIM = "io.ssemaj.deviceintelligence.internal.FrameworkShim"
        const val ANCHOR = "io.ssemaj.dx.K"
    }
}
