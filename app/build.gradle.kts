import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 서명 정보는 저장소 밖 keystore.properties 에 둡니다. 없으면 디버그 키로 서명해 누구나 빌드할 수 있습니다.
val keyProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.drivingdoctor"
    compileSdk = 36

    defaultConfig {
        // 실차(arm64)와 에뮬레이터(x86_64)용 기계어만 넣는다
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        applicationId = "app.drivingdoctor"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keyProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 쓰지 않는 코드·리소스를 빼서 APK를 줄이고 시작을 빠르게 한다. Pleos SDK는 리플렉션을 써서 통째로 보존한다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keyProps.isNotEmpty()) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    // Pleos Connect Vehicle SDK: 방향지시등·브레이크·기어·문·경고등·주행거리
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    // 차 안 음성 엔진(Pleos CaaS)으로 말하기
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")

    val bom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
}

android {
    // Pleos 음성 SDK의 보안 라이브러리(.so)를 압축해 APK를 줄인다
    packaging { jniLibs { useLegacyPackaging = true } }
}
