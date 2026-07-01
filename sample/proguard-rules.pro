# hydra sample — R8 keep rules.
#
# The DeviceIntelligence runtime AAR ships its own consumer ProGuard rules that
# keep the native JNI anchor (io.ssemaj.dx.K) and the FrameworkShim up-call
# surface — those bind by name from native code and must not be renamed/stripped.
#
# The one consumer-side class R8 doesn't know about is the plugin-generated
# secrets accessor, so keep it intact (its encrypted map + StrDec.g call site).
-keep class com.github.iamjosephmj.hydra.Hydra { *; }

# Poseidon's runtime ships adapters for OkHttp / Volley / Cronet, but the sample
# uses none of them, so those symbols aren't on the classpath. Suppress R8's
# missing-class warnings for the unused adapters (the adapter code is only
# reachable when the corresponding HTTP client is actually present).
-dontwarn com.android.volley.**
-dontwarn okhttp3.**
-dontwarn org.chromium.net.**
