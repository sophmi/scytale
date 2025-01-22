package rs.soph.scytale.common

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteExtensionsTest {

	@Test
	fun `can assemble long from bytes`() {
		assertEquals(0, longFromBytes(0, 0, 0, 0, 0, 0, 0, 0))
		assertEquals(0xFFFFFFFF, longFromBytes(0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF))
		assertEquals(-1L shl Int.SIZE_BITS, longFromBytes(0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0))
		assertEquals(0x01234567_89ABCDEF, longFromBytes(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF))
	}
}
