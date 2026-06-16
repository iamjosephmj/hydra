# hydra sample — R8 keep rules.
#
# The DeviceIntelligence runtime AAR ships its own consumer ProGuard rules that
# keep the native JNI anchor (io.ssemaj.dx.K) and the FrameworkShim up-call
# surface — those bind by name from native code and must not be renamed/stripped.
#
# The one consumer-side class R8 doesn't know about is the plugin-generated
# secrets accessor, so keep it intact (its encrypted map + StrDec.g call site).
-keep class com.github.iamjosephmj.hydra.Hydra { *; }
