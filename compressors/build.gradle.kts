plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.rabindradev.compressors"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).let {
        it.addStringOption("Xdoclint:none", "-quiet")
        it.addStringOption("encoding", "UTF-8")
        it.addStringOption("charSet", "UTF-8")
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(files("libs/aspectjrt-1.7.3.jar"))
    implementation ("com.googlecode.mp4parser:isoparser:1.1.22")
    implementation ("androidx.exifinterface:exifinterface:1.3.0")
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")
}