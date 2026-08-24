// 하위 모듈이 쓰는 플러그인은 전부 여기에 apply false 로 한 번 선언한다.
// kotlin.android 와 kotlin.jvm 은 같은 jar(kotlin-gradle-plugin)에서 나온다.
// 루트에 하나만 선언하면, 다른 하나를 하위 모듈에서 버전과 함께 요청할 때
// "already on the classpath with an unknown version" 으로 막힌다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
