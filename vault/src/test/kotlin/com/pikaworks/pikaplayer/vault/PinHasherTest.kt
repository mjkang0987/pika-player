package com.pikaworks.pikaplayer.vault

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinHasherTest {

    // 테스트에서는 반복 횟수를 낮춘다. 검증하려는 것은 세기가 아니라 동작이다.
    private val iters = 1000

    @Test
    fun `같은 PIN 과 같은 소금이면 같은 값이 나온다`() {
        val salt = ByteArray(16) { it.toByte() }
        assertContentEquals(
            PinHasher.hash("1234", salt, iters),
            PinHasher.hash("1234", salt, iters),
        )
    }

    @Test
    fun `소금이 다르면 같은 PIN 이라도 값이 다르다`() {
        val a = PinHasher.hash("1234", ByteArray(16) { 1 }, iters)
        val b = PinHasher.hash("1234", ByteArray(16) { 2 }, iters)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `PIN 이 다르면 값이 다르다`() {
        val salt = ByteArray(16) { it.toByte() }
        assertFalse(
            PinHasher.hash("1234", salt, iters).contentEquals(PinHasher.hash("1235", salt, iters))
        )
    }

    @Test
    fun `맞는 PIN 은 통과하고 틀린 PIN 은 막힌다`() {
        val salt = PinHasher.newSalt()
        val stored = PinHasher.hash("482913", salt, iters)
        assertTrue(PinHasher.verify("482913", salt, stored, iters))
        assertFalse(PinHasher.verify("482914", salt, stored, iters))
        assertFalse(PinHasher.verify("", salt, stored, iters))
        assertFalse(PinHasher.verify("4829130", salt, stored, iters))
    }

    /** 아무것도 안 넣고 확인을 눌러도 앱이 죽으면 안 된다. */
    @Test
    fun `빈 PIN 은 예외가 아니라 불일치다`() {
        val salt = PinHasher.newSalt()
        val stored = PinHasher.hash("1234", salt, iters)
        assertFalse(PinHasher.verify("", salt, stored, iters))
    }

    @Test
    fun `빈 PIN 은 설정할 수 없다`() {
        assertFailsWith<IllegalArgumentException> { PinHasher.hash("", PinHasher.newSalt(), iters) }
    }

    @Test
    fun `길이가 다른 값과 비교해도 터지지 않는다`() {
        val salt = PinHasher.newSalt()
        assertFalse(PinHasher.verify("1234", salt, ByteArray(4), iters))
        assertFalse(PinHasher.verify("1234", salt, ByteArray(0), iters))
    }

    @Test
    fun `요청한 길이만큼 나온다`() {
        val salt = PinHasher.newSalt()
        assertEquals(PinHasher.KEY_BYTES, PinHasher.hash("1234", salt, iters).size)
    }

    @Test
    fun `소금은 매번 다르다`() {
        val seen = (1..50).map { PinHasher.newSalt().toList() }.toSet()
        assertEquals(50, seen.size)
        assertEquals(PinHasher.SALT_BYTES, PinHasher.newSalt().size)
    }

    /**
     * RFC 6070 은 SHA-1 기준이라 그대로 쓸 수 없다. 대신 널리 인용되는
     * PBKDF2-HMAC-SHA256 벡터로 직접 구현이 규격과 맞는지 확인한다.
     * 이게 틀리면 기기를 바꾸거나 라이브러리를 갈아끼울 때 기존 PIN 이 전부 깨진다.
     */
    @Test
    fun `PBKDF2 HMAC SHA256 표준 벡터와 일치한다`() {
        val out = PinHasher.hash("password", "salt".toByteArray(), iterations = 1)
        val hex = out.joinToString("") { "%02x".format(it) }
        assertTrue(
            hex.startsWith("120fb6cffcf8b32c43e7225256c4f837a86548c9"),
            "PBKDF2-HMAC-SHA256(password, salt, 1) 앞부분이 다르다: $hex",
        )
    }
}
