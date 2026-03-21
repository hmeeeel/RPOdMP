// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

ext {
    val properties = java.util.Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        properties.load(localFile.inputStream())
    }
    extra["mapkitApiKey"] = properties.getProperty("MAPKIT_API_KEY", "")
}
