import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * The Compose UI both applications draw from — the player first.
 *
 * Library versions are pinned to the Compose Multiplatform release whose
 * Android artifacts are the androidx ones the phone app already runs (CMP
 * 1.10.x -> androidx 1.10.x, material3 1.9.0 -> androidx material3 1.4.0), so
 * depending on this module never moves the phone's Compose underneath it. The
 * desktop resolves its own newer versions over these at the top of its graph.
 *
 * Material 3 Expressive ALPHA (branch material3-expressive, orden de
 * Alejandro 2026-11-09): JetBrains material3 1.11.0-alpha07 is the newest
 * JetBrains line that still wraps androidx material3 on Android — its
 * android variant depends on androidx material3 1.5.0-alpha17 (verified in
 * its published Gradle module metadata 2026-11-09). JetBrains 1.12+/1.13+
 * no longer declare any androidx material3 dependency on Android (they ship
 * their own implementation of the package), so using them here would put two
 * copies of androidx.compose.material3 on the phone's classpath — duplicate
 * classes at dex time. There is NO JetBrains version wrapping androidx
 * 1.5.0-beta/rc; alpha17 is the newest wrapped one. The phone app pins
 * androidx material3 1.5.0-rc01 directly, so Gradle resolves androidx
 * material3 to rc01 everywhere at runtime; this module compiles against the
 * alpha17-level API, which already has the full Expressive set public
 * (MaterialExpressiveTheme, ButtonGroup, SplitButtonLayout, LoadingIndicator,
 * MotionScheme.expressive() — verified against the published
 * 1.5.0-alpha17 sources jar and JetBrains 1.11.0-alpha07 sources jar).
 * The alpha17 APIs are @ExperimentalMaterial3ExpressiveApi, hence the
 * file-level @OptIn in ui/theme/Expressive.kt.
 *
 * FIX 2026-11-09: material-icons-extended is pinned back to 1.7.3 — version
 * 1.9.0 does NOT exist (404 on Maven Central, verified; the directory
 * listing ends at 1.7.3 and there is no 1.11.0-alpha07 of it either), so the
 * previous bump to 1.9.0 would have failed dependency resolution in Actions.
 * Icons are stable API; Gradle resolves material3 itself to 1.11.0-alpha07.
 *
 * Cost of the alpha (honest): JetBrains 1.11.0-alpha07 pulls compose
 * runtime/foundation/ui 1.11.0-beta03 transitively, so those resolve above
 * this module's $cmp = 1.10.3 while the compose compiler plugin stays
 * 1.10.3, and kotlin-stdlib resolves to the project's 2.3.20 (above the
 * 2.2.20 the alpha was built against). If Actions fails on a version
 * conflict here, the fallback is sharedUi back to JetBrains 1.9.0
 * (androidx 1.4.0) with the app alone on 1.5.0-rc01 — the Expressive
 * wrappers in this module would then go back to their custom
 * implementations. See M3X-NOTES.md.
 *
 * The pin is the *Android* anchor only. On the JVM it is a floor, not a
 * ceiling — and there it matters which version compiles this module, because
 * the desktop resolves ui-desktop 1.12.0 / skiko 0.150.x at runtime. Skia's
 * Matrix33 became an inline float[9] between skiko 0.9 and 0.15
 * (`makeTranslate` went from returning a Matrix33 to a name-mangled float[]
 * overload), so bytecode compiled against 0.9 linked its object call and
 * crashed with NoSuchMethodError on the desktop the first time a lyric line
 * grew. Compiling the module against the version the desktop actually
 * resolves keeps the compile-time link and the runtime link the same.
 */
val cmp = "1.10.3"

/** What the desktop graph resolves; see the comment above. */
val CMP_DESKTOP =
    // Haze 1.7.x, which the Windows build uses, was compiled
    // against a ShaderBrush.createShader that returns skia's Shader; 1.12 changed that return
    // type, so on 1.12 every blur dies with NoSuchMethodError. Windows stays on the plugin's
    // own 1.10.3 / skiko 0.9; only Linux moves to 1.12.
    if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "1.10.3" else "1.12.0"

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    // AGP 9.1.1 (branch material3-expressive, run 42667341916): the legacy
    // 'com.android.library' plugin cannot be combined with KMP anymore, so
    // this module uses 'com.android.kotlin.multiplatform.library'. Per its
    // docs there is no top-level android {} block: namespace, compileSdk
    // and minSdk live in kotlin { android { ... } }.
    android {
        namespace = "com.music.bitchord.sharedui"
        // Material 3 Expressive: innertubex-android v0.7.0 exige compileSdk
        // 37+ (checkAndroidMainAarMetadata, run 42672443562); app/ y tv/ ya
        // están en 37.
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // Both targets are JVMs, so the UI is written once against the JDK here
        // rather than in commonMain.
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmSharedMain)
        jvmMain.get().dependsOn(jvmSharedMain)

        commonMain.dependencies {
            api(project(":shared"))
            api("org.jetbrains.compose.runtime:runtime:$cmp")
            api("org.jetbrains.compose.foundation:foundation:$cmp")
            api("org.jetbrains.compose.animation:animation:$cmp")
            api("org.jetbrains.compose.ui:ui:$cmp")
            api("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
            api("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            api("org.jetbrains.compose.components:components-resources:$cmp")
            api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            api("io.coil-kt.coil3:coil-compose:3.0.4")
            api("dev.chrisbanes.haze:haze:1.3.1")
            api("dev.chrisbanes.haze:haze-materials:1.3.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        // Compiled against what the desktop actually resolves, not the phone's
        // pin — see the CMP_DESKTOP comment at the top of this file. Skia's
        // Matrix33 changed shape between skiko 0.9 and 0.15, and bytecode built
        // against 0.9 NoSuchMethodError'd the desktop's 0.15 at draw time.
        jvmMain.dependencies {
            api("org.jetbrains.compose.ui:ui-desktop:$CMP_DESKTOP")
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("junit:junit:4.13.2")
        }
        androidMain.dependencies {
            // The phone's own versions, so nothing here moves them.
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.core:core-ktx:1.15.0")
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("androidx.palette:palette-ktx:1.0.0")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.music.bitchord.sharedui.resources"
    generateResClass = always
}
