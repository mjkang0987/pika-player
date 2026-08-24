import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Android 의존성이 없는 순수 Kotlin 모듈.
// 자막 파싱과 인코딩 판별은 이 앱에서 가장 깨지기 쉬운 로직인데,
// 여기 두면 에뮬레이터나 기기 없이 JVM 테스트로 검증할 수 있다.
dependencies {
    testImplementation(kotlin("test"))
}

// JDK 21(안드로이드 스튜디오 번들 JBR)로 컴파일하되 결과물은 Java 17 바이트코드로
// 맞춘다. `jvmToolchain(17)` 을 쓰면 JDK 17 이 따로 설치돼 있어야 하고, 없으면
// "No matching toolchains found" 로 동기화 자체가 막힌다. 안드로이드가 요구하는 건
// 바이트코드 수준이지 컴파일에 쓴 JDK 가 아니다.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}
