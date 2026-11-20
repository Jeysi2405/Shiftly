// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register<Copy>("stageReleaseApk") {
    dependsOn(":app:assembleDebug")
    val releaseDir = layout.projectDirectory.dir("release")
    val debugApk = layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk")
    doFirst {
        releaseDir.asFile.listFiles()?.forEach { file ->
            if (file.name.startsWith("Shiftly-") && file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }
    from(debugApk)
    into(releaseDir)
    rename { "Shiftly-v3.2.0.apk" }
}
