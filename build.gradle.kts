// Top-level build file where you can add configuration options common to all sub-projects/modules.
/*
 * R8 override REMOVED (branch material3-expressive, AGP bump 2026-11-09):
 * it used to pin R8 to 8.13.23 because AGP 8.10.1 bundled R8 8.10.9, which
 * miscompiled `NowPlayingScreen` in debug dexing (VerifyError, see the old
 * comment) and could not read Kotlin 2.3 @Metadata. The override's own note
 * said it was removable once AGP bundled something past 8.11.32 — AGP 9.1.1
 * bundles R8 9.1.x, so the override is gone and the bundled R8 applies.
 * If a VerifyError on NowPlayingScreen ever comes back in a dev build, this
 * is the first place to look.
 */
/*
 * The version of the phone app and of the watch app that ships inside it, in
 * one place. The watch has to move with the phone: it is installed and updated
 * from the phone's own APK (see app/build.gradle.kts), and Android refuses an
 * update whose versionCode is lower than the one installed.
 *
 * No "v" in the name — the in-app updater reads it as a number; the "v" is the
 * release tag's alone.
 */
extra["bitchordVersionCode"] = 39
extra["bitchordVersionName"] = "1.9.1"

/*
 * Bump this by hand before cutting each sideloaded test build ("beta2",
 * "beta3", ...) and blank it out before cutting the real release. Marks the
 * versionName above as a pre-release: AppUpdateChecker.isNewer() treats any
 * "-suffix" as older than a clean release of the same number, so testers
 * still get the update prompt once the matching tag is actually published.
 *
 * Applied to release builds as well as debug ones, and that is the whole
 * point of it. A sideloaded beta is a *release* build — signed with the real
 * key, installed over the real package — so leaving the marker off it is
 * exactly the case that strands a tester: their build calls itself 1.6.1,
 * the published 1.6.1 then matches it, isNewer() says no, and no prompt ever
 * comes. Blanking this line is the one step that turns a beta into a release,
 * so it is the one place to get right.
 *
 * The watch app carries the same marker. It needs no prompt logic of its own:
 * the phone compares the watch's installed APK with the one it carries, so a
 * beta watch app shows "Update available" as soon as the phone has the release.
 */
extra["bitchordBetaSuffix"] = ""

/*
 * AGP 9.1.1 (branch material3-expressive, 2026-11-09): the Material 3 alpha
 * (1.5.0-rc01) and Compose 1.12.0 AARs require AGP 9.1.0+, and the app's
 * compileSdk 37 needs AGP 9.1.1 as its documented minimum — so 9.1.1 is the
 * smallest version that satisfies both, not 9.1.0. It in turn requires
 * Gradle 9.3.1 (see gradle-wrapper.properties) and JDK 17 (workflow already
 * uses 17). Kotlin stays 2.3.20 and Compose Multiplatform stays 1.10.3:
 * Kotlin's own docs list KGP 2.3.20 as fully supported only up to AGP 9.0.0
 * and Gradle 9.3.0, but the same docs say newer AGP/Gradle versions "usually
 * work" with possible deprecation warnings — bumping Kotlin to 2.4.x would
 * force a CMP bump and re-open the JetBrains material3 mapping, so the
 * minimal AGP-only bump goes first and the build will tell us if KGP
 * complains. See M3X-NOTES.md §11.
 */
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
}
