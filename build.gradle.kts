plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

allprojects {
    layout.buildDirectory.set(file("${System.getProperty("user.home")}/.android-builds/${rootProject.name}/${project.name}"))
}
