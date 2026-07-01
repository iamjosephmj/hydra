plugins {
    id("com.android.application") version "8.13.2" apply false
    // Bumped 2.0.21 → 2.2.10 so the sample can consume Poseidon 0.1.4, whose
    // runtime AARs ship Kotlin 2.2.0 metadata (older compilers can't read it).
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

group = "com.github.iamjosephmj"
version = "1.2.1"
