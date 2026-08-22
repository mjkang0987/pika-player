plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Android 의존성이 없는 순수 Kotlin 모듈.
// 자막 파싱과 인코딩 판별은 이 앱에서 가장 깨지기 쉬운 로직인데,
// 여기 두면 에뮬레이터나 기기 없이 JVM 테스트로 검증할 수 있다.
dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
