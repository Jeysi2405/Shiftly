// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register<Copy>("stageReleaseApk") {
    val signedReleaseApk = layout.projectDirectory.file("app/release/app-release.apk")
    val debugApk = layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk")
    from(if (signedReleaseApk.asFile.exists()) signedReleaseApk else debugApk)
    into(layout.projectDirectory.dir("release"))
    rename { "Shiftly-v3.0.0.apk" }
}
