plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "kr.ac.kopo.android_school_meal"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.ac.kopo.android_school_meal"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.fragment)
    implementation(libs.coordinatorlayout)

    // 네트워크 통신
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // 이미지 처리
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // 날짜 처리
    implementation(libs.joda.time)

    // 이미지 선택
    implementation(libs.ted.imagepicker)

    // 권한 처리
    implementation(libs.dexter)

    // 로컬 스토리지
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

}