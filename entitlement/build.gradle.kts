import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Android 의존성이 없는 순수 Kotlin 모듈.
// "이 사용자가 이 기능을 쓸 수 있는가" 는 틀리면 매출이 새거나 산 사람이 못 쓰는
// 쪽으로 직결된다. 결제 SDK 와 떼어놓아야 기기 없이 테스트로 못박을 수 있다.
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
