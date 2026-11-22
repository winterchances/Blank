plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.blank.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blank.gallery"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

val generatedLauncherResDir = layout.buildDirectory.dir("generated/res/launcherIcon")

android.sourceSets.getByName("main").res.srcDir(generatedLauncherResDir)

val prepareLauncherIcon by tasks.registering(Copy::class) {
    from(rootProject.file("assets/app-icon/blank-icon.png"))
    into(generatedLauncherResDir.map { it.dir("mipmap-nodpi") })
    rename { "ic_launcher.png" }
}

tasks.named("preBuild").configure {
    dependsOn(prepareLauncherIcon)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("dev.chrisbanes.haze:haze:1.5.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.paging:paging-compose:3.3.6")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    implementation("androidx.room:room-paging:2.8.3")
    kapt("androidx.room:room-compiler:2.8.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

kapt {
    correctErrorTypes = true
}
