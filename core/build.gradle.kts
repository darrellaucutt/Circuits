plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "net.aucutt.circuits.core"
    compileSdk {
        version = release(37) {

        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.org.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
