// 하위 모듈이 쓰는 플러그인은 전부 여기에 apply false 로 한 번 선언한다.
// kotlin.android 는 없다 — AGP 9 부터 Kotlin 지원이 AGP 에 내장이라
// 따로 적용하면 오히려 막힌다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
