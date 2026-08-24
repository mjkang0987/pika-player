plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Android 의존성이 없는 순수 Kotlin 모듈.
// PIN 검증과 잠금 정책은 틀리면 "산 사람이 자기 폴더를 못 연다" 또는
// "아무나 연다" 둘 중 하나가 된다. 기기 없이 테스트로 못박을 수 있어야 한다.
dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
