import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.34.0"
}

// Maven Central coordinates live under the verified tech.thessemaj namespace.
// JitPack consumers are unaffected: it archives whatever the build publishes
// and keeps serving historical tags built with the old group.
group = "tech.thessemaj"
version = findProperty("VERSION_NAME")?.toString() ?: "2.7.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Vendored baking logic + closed key derivation (the released 4.7.0 jars).
    implementation(files("libs/deviceintelligence-gradle-4.7.0.jar"))
    implementation(files("libs/deviceintelligence-baker-4.7.0.jar"))

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

// SHADE the vendored jars into the published plugin jar. `implementation(files(..))`
// keeps them on the in-repo composite-build runtime classpath, but local file
// dependencies do NOT survive publishing — so an external (JitPack) consumer
// would be missing DeviceIntelligencePlugin + DiBaker classes. Merging the class
// entries into our own jar makes the published plugin self-contained.
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree("libs/deviceintelligence-gradle-4.7.0.jar")) { exclude("META-INF/**") }
    from(zipTree("libs/deviceintelligence-baker-4.7.0.jar")) { exclude("META-INF/**") }
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
        // Maven Central id: plugin-marker groups must sit under a verified
        // namespace, and com.github.* is not allowed there. Same plugin class;
        // the legacy id above keeps existing JitPack consumers working.
        create("hydraCentral") {
            id = "tech.thessemaj.hydra"
            implementationClass = "com.github.iamjosephmj.hydra.HydraPlugin"
            displayName = "hydra"
            description = "Bake DeviceIntelligence RASP checks into any Android app with one plugin id."
        }
    }
}

// The legacy com.github.* plugin marker must never reach Maven Central —
// that namespace is disallowed there and would fail the whole deployment.
// It still publishes locally, so JitPack keeps serving the legacy id.
tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { !(publication.name == "hydraPluginMarkerMaven" && repository.name == "mavenCentral") }
}

mavenPublishing {
    configure(GradlePlugin(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))
    publishToMavenCentral(automaticRelease = true)
    // Sign only when a key is supplied (CI) — JitPack's keyless
    // publishToMavenLocal must keep working.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    coordinates("tech.thessemaj", "hydra", version.toString())
    pom {
        name = "hydra"
        description = "Bake DeviceIntelligence RASP checks into any Android app with one plugin id."
        url = "https://github.com/iamjosephmj/hydra"
        licenses {
            license {
                name = "CC BY-ND 4.0"
                url = "https://creativecommons.org/licenses/by-nd/4.0/legalcode"
            }
        }
        developers {
            developer {
                id = "iamjosephmj"
                name = "Joseph MJ"
                url = "https://github.com/iamjosephmj"
            }
        }
        scm {
            url = "https://github.com/iamjosephmj/hydra"
            connection = "scm:git:git://github.com/iamjosephmj/hydra.git"
            developerConnection = "scm:git:ssh://git@github.com/iamjosephmj/hydra.git"
        }
    }
}
