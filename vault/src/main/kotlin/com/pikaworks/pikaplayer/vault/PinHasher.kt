package com.pikaworks.pikaplayer.vault

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PIN 을 저장 가능한 형태로 바꾼다.
 *
 * **이 방식으로 막을 수 있는 것과 없는 것을 분명히 해둔다.**
 *
 * 막는 것: 남이 내 폰을 집어 들고 앨범을 넘겨보는 상황. 저장된 값만 봐서는
 * PIN 을 알 수 없고, 앱 안에서는 [LockoutPolicy] 가 반복 시도를 막는다.
 *
 * 못 막는 것: 기기를 루팅해 저장값을 꺼낸 뒤 오프라인에서 대입하는 공격.
 * 숫자 4~6 자리는 경우의 수가 백만 이하라 반복 횟수를 아무리 올려도 시간 문제다.
 * 서버 없이 이걸 막을 방법은 없고, 기획서 3장이 감수하기로 한 수준이다.
 * "잠긴 폴더" 이지 "암호화된 폴더" 가 아니라는 점을 화면에서도 그렇게 말해야 한다.
 *
 * PBKDF2 를 `SecretKeyFactory` 대신 [Mac] 으로 직접 돌리는 이유: SHA-256 을 쓰는
 * `PBKDF2WithHmacSHA256` 은 API 26 부터라 minSdk 24 에서 기기마다 없을 수 있다.
 * HmacSHA256 자체는 어디에나 있다.
 */
object PinHasher {

    const val SALT_BYTES = 16
    const val KEY_BYTES = 32
    const val ITERATIONS = 120_000

    fun newSalt(random: SecureRandom = SecureRandom()): ByteArray =
        ByteArray(SALT_BYTES).also(random::nextBytes)

    /** 빈 PIN 은 설정할 수 없다. HMAC 키가 비면 라이브러리가 예외를 던진다. */
    fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        require(pin.isNotEmpty()) { "PIN 이 비어 있다" }
        return pbkdf2(pin, salt, iterations, KEY_BYTES)
    }

    /**
     * 시간이 일정한 비교.
     *
     * 앞에서부터 다르면 바로 빠져나오는 비교는 어디까지 맞았는지가 걸린 시간에
     * 드러난다. 여기서 새는 정보가 크진 않지만, 맞추기 어렵지도 않은 방어다.
     */
    fun verify(pin: String, salt: ByteArray, expected: ByteArray, iterations: Int = ITERATIONS): Boolean {
        // 빈 입력은 그냥 틀린 것으로 본다. 여기서 예외를 던지면 화면에서 아무것도
        // 안 넣고 확인을 누른 것만으로 앱이 죽는다.
        if (pin.isEmpty() || expected.isEmpty()) return false
        val actual = hash(pin, salt, iterations)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray {
        require(iterations > 0) { "반복 횟수는 1 이상이어야 한다" }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(pin.toByteArray(Charsets.UTF_8), "HmacSHA256"))

        val out = ByteArray(keyBytes)
        val blockSize = mac.macLength
        var offset = 0
        var block = 1

        while (offset < keyBytes) {
            // U1 = HMAC(salt || block index), 이후 U(n) = HMAC(U(n-1)) 을 XOR 로 누적
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            val acc = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (i in acc.indices) acc[i] = (acc[i].toInt() xor u[i].toInt()).toByte()
            }
            val take = minOf(blockSize, keyBytes - offset)
            acc.copyInto(out, offset, 0, take)
            offset += take
            block += 1
        }
        return out
    }
}
