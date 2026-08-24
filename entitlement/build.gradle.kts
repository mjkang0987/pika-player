plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Android 의존성이 없는 순수 Kotlin 모듈.
// "이 사용자가 이 기능을 쓸 수 있는가" 는 틀리면 매출이 새거나 산 사람이 못 쓰는
// 쪽으로 직결된다. 결제 SDK 와 떼어놓아야 기기 없이 테스트로 못박을 수 있다.
dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
