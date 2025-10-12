plugins { `kotlin-dsl` }

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.sonarqube.plugin)
    implementation(libs.spotless.plugin)
}
