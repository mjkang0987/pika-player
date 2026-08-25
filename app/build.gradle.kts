import java.util.Properties

// AGP 9 부터 Kotlin 지원이 AGP 안에 들어왔다. kotlin.android 를 따로 적용하면
// "no longer required for Kotlin support since AGP 9.0" 으로 막힌다.
// https://kotl.in/gradle/agp-built-in-kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * 서명 정보. 저장소에 넣지 않는다.
 *
 * keystore.properties 와 .jks 는 .gitignore 에 있다. 파일이 없으면 서명 설정을
 * 달지 않고, 릴리스는 서명 없이 빌드된다 — 다른 기계에서 체크아웃했을 때
 * 빌드가 통째로 막히는 것보다, R8 검증까지는 되고 설치만 안 되는 편이 낫다.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

/**
 * .jks 파일. 지정하지 않았으면 null.
 *
 * `~` 를 손으로 풀어 준다. 셸이 아니라 Java 가 읽는 파일이라 물결표가 글자
 * 그대로 경로에 들어가고, 그러면 프로젝트 폴더 아래 `~` 라는 디렉터리를 찾는다.
 */
val keystoreFile = keystoreProperties.getProperty("storeFile")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { if (it.startsWith("~/")) System.getProperty("user.home") + it.drop(1) else it }
    ?.let { rootProject.file(it) }

android {
    namespace = "com.pikaworks.pikaplayer"
    // Compose 1.12(BOM 2026.08)와 core-ktx 1.19 가 요구하는 값.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pikaworks.pikaplayer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    // buildTypes 보다 먼저 와야 한다. 아래에서 이름으로 찾아 쓴다.
    signingConfigs {
        create("release") {
            keystoreFile?.let { keystore ->
                // 경로가 틀리면 Gradle 은 "not found" 만 말하고 끝난다. 어디를
                // 고쳐야 하는지 여기서 함께 알려 준다 — keystore.properties 는
                // 저장소에 없어서, 무엇이 빠졌는지 알 방법이 그것뿐이다.
                if (!keystore.exists()) {
                    throw GradleException(
                        "서명 키를 찾을 수 없습니다: ${keystore.absolutePath}\n" +
                            "keystore.properties 의 storeFile 을 .jks 의 절대 경로로 고치세요."
                    )
                }
                storeFile = keystore
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.VERSION_NAME 을 설정 화면에서 쓴다
    }
}

// jvmTarget 은 위 compileOptions 를 AGP 내장 Kotlin 이 따라간다. 따로 지정하지 않는다.

// Room 스키마를 파일로 남긴다.
//
// 다음 버전의 마이그레이션을 쓰려면 지금 스키마가 무엇이었는지 알아야 한다.
// 코드만 보고 손으로 옮겨 적으면 컬럼 하나를 빠뜨려도 알아채지 못한다.
// 빌드하면 app/schemas/<버전>.json 이 생기고, 이 파일은 저장소에 넣는다.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":subtitle"))
    implementation(project(":vault"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
}
