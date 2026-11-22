// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

evaluationDependsOn(":app")

tasks.register<Copy>("stageReleaseApk") {
    dependsOn(":app:assembleRelease")
    val appVersionName = project(":app").extensions
        .getByType(ApplicationExtension::class.java)
        .defaultConfig.versionName ?: "0.0.0"
    val releaseDir = layout.projectDirectory.dir("release")
    val releaseApk = project(":app").layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    doFirst {
        releaseDir.asFile.listFiles()?.forEach { file ->
            if (file.name.startsWith("Shiftly-") && file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }
    from(releaseApk)
    into(releaseDir)
    rename { "Shiftly-v$appVersionName.apk" }
}
