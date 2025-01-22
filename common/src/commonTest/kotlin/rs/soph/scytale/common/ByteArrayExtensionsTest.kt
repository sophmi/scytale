package rs.soph.scytale.common

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ByteArrayExtensionsTest {

	@Test
	fun `getLong correctly reads at index 0`() {
		assertEquals(0L, ByteArray(Long.SIZE_BYTES).getLong(0))
		assertEquals(0xFFFFFFFF, bytes(0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF).getLong(0))
		assertEquals(-1L shl Int.SIZE_BITS, bytes(0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0).getLong(0))
		assertEquals(0x01234567_89ABCDEF, bytes(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF).getLong(0))
	}

	@Test
	fun `getLong correctly reads at offsets`() {
		assertEquals(0L, ByteArray(Long.SIZE_BYTES).getLong(0))
		assertEquals(0xFFFFFFFF, bytes(0, 0, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF).getLong(2))
		assertEquals(-1L shl Int.SIZE_BITS, bytes(0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0).getLong(4))
		assertEquals(0x01234567_89ABCDEF, bytes(0, 0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF).getLong(1))
	}

	@Test
	fun `putLong correctly writes at index 0`() {
		assertContentEquals(ByteArray(Long.SIZE_BYTES), putLong(0))
		assertContentEquals(bytes(0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF), putLong(0xFFFFFFFF))
		assertContentEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0), putLong(-1L shl Int.SIZE_BITS))
		assertContentEquals(bytes(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF), putLong(0x01234567_89ABCDEF))
	}

	@Test
	fun `putLong correctly writes at offsets`() {
		assertContentEquals(ByteArray(1 + Long.SIZE_BYTES), putLong(1, 0))
		assertContentEquals(bytes(0, 0, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF), putLong(2, 0xFFFFFFFF))
		assertContentEquals(bytes(0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0), putLong(4, -1L shl Int.SIZE_BITS))
		assertContentEquals(bytes(0, 0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF), putLong(1, 0x01234567_89ABCDEF))
	}

	private fun bytes(vararg bytes: Int) = ByteArray(bytes.size) { bytes[it].toByte() }

	private fun putLong(value: Long) = putLong(0, value)

	private fun putLong(offset: Int, value: Long): ByteArray {
		return ByteArray(offset + Long.SIZE_BYTES).apply { putLong(offset, value) }
	}
}
