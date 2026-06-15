plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.iamjosephmj"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Vendored baking logic + closed key derivation (the released 3.0.0 jars).
    implementation(files("libs/deviceintelligence-gradle-3.0.0.jar"))
    implementation(files("libs/deviceintelligence-baker-3.0.0.jar"))

    // The bundled DeviceIntelligence plugin uses apksig at runtime to re-sign
    // the instrumented APK; it was `implementation` there, so we put it on
    // hydra's runtime classpath too (vendoring a jar does not bring its deps).
    implementation("com.android.tools.build:apksig:8.13.2")

    // AGP Variant API — compileOnly: provided by the consumer build at runtime.
    compileOnly("com.android.tools.build:gradle-api:8.13.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(gradleTestKit())
}

// Align Kotlin's JVM target with Java's (17) so the build is consistent on
// JDK 23 (where Kotlin would otherwise fall back to JVM target 22).
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test { useJUnitPlatform() }

gradlePlugin {
    plugins {
        create("hydra") {
            id = "com.github.iamjosephmj.hydra"
            implementationClass = "com.github.iamjosephmj.hydra.HydraPlugin"
            displayName = "hydra"
            description = "Bake DeviceIntelligence RASP checks into any Android app with one plugin id."
        }
    }
}
