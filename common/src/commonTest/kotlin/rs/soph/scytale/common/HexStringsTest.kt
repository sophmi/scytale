package rs.soph.scytale.common

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HexStringsTest {

	@Test
	fun `zeroes are prepended when formatting short hex strings`() {
		assertEquals("00", byteArrayOf(0).toUpperHexString(prefix = false))
		assertEquals("0x00", byteArrayOf(0).toUpperHexString())
		assertEquals("0x0F", byteArrayOf(0xF).toUpperHexString())

		assertEquals("00000000", intArrayOf(0).toUpperHexString(prefix = false))
		assertEquals("0x00000000", intArrayOf(0).toUpperHexString())
		assertEquals("0x0000000F", intArrayOf(0xF).toUpperHexString())
		assertEquals("0x0FFFFFFF", intArrayOf(0xFFFFFFF).toUpperHexString())
	}

	@Test
	fun `can format ByteArray as hex string`() {
		assertEquals("0x77", byteArrayOf(0x77).toUpperHexString())
		assertEquals("0x557A", byteArrayOf(0x55, 0x7A).toUpperHexString())

		assertEquals("0x01010101", byteArrayOf(1, 1, 1, 1).toUpperHexString())
		assertEquals("0x11223344_5566", byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66).toUpperHexString())
	}

	@Test
	fun `can format ByteArray as prefixless hex string`() {
		assertEquals("77", byteArrayOf(0x77).toUpperHexString(prefix = false))
		assertEquals("557A", byteArrayOf(0x55, 0x7A).toUpperHexString(prefix = false))

		assertEquals("01010101", byteArrayOf(1, 1, 1, 1).toUpperHexString(prefix = false))
		assertEquals("11223344_5566", byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66).toUpperHexString(prefix = false))
	}

	@Test
	fun `can format IntArray as hex string`() {
		assertEquals("0x00009999", intArrayOf(0x9999).toUpperHexString())
		assertEquals("0x80000000", intArrayOf(-0x80000000).toUpperHexString())
		assertEquals("0x00880055_0000007A", intArrayOf(0x880055, 0x7A).toUpperHexString())

		assertEquals("0x0FFFFFFF_00000099_00001000", intArrayOf(0xFFFFFFF, 0x99, 0x1000).toUpperHexString())
	}

	@Test
	fun `can format IntArray as prefixless hex string`() {
		assertEquals("00009999", intArrayOf(0x9999).toUpperHexString(prefix = false))
		assertEquals("80000000", intArrayOf(-0x80000000).toUpperHexString(prefix = false))
		assertEquals("00880055_0000007A", intArrayOf(0x880055, 0x7A).toUpperHexString(prefix = false))

		assertEquals("0FFFFFFF_00000099_00001000", intArrayOf(0xFFFFFFF, 0x99, 0x1000).toUpperHexString(prefix = false))
	}

	@Test
	fun `can parse ByteArray from hex string with any case`() {
		assertContentEquals(byteArrayOf(0xA), "0x0A".hexToBytes())
		assertContentEquals(byteArrayOf(0x55, 0x7A), "557a".hexToBytes())

		assertContentEquals(byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D), "1A2b3c4D".hexToBytes())
		assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66), "112233445566".hexToBytes())
	}

	@Test
	fun `can parse ByteArray from hex string prefixed with 0x`() {
		assertContentEquals(byteArrayOf(0x77), "0x77".hexToBytes())
		assertContentEquals(byteArrayOf(0x55, 0x7A), "0x557A".hexToBytes())

		assertContentEquals(byteArrayOf(1, 1, 1, 1), "0x01010101".hexToBytes())
		assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66), "0x112233445566".hexToBytes())
	}

	@Test
	fun `can parse ByteArray from hex string containing whitespace and underscores`() {
		assertContentEquals(byteArrayOf(0x77), "7_7".hexToBytes())
		assertContentEquals(byteArrayOf(0x55, 0x7A), "55 7A".hexToBytes())

		assertContentEquals(byteArrayOf(1, 1, 1, 1), "01_010_101".hexToBytes())
		assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66), "1122 3344_5566".hexToBytes())
	}

	@Test
	fun `can parse ByteArray from mixed-case hex string with 0x whitespace and underscores`() {
		assertContentEquals(byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D, 0x5E), "0x1A2b3 c_4D 5 e".hexToBytes())
	}

	@Test
	fun `can parse IntArray from hex string with any case`() {
		assertContentEquals(intArrayOf(0xA), "0x0000000A".hexToInts())
		assertContentEquals(intArrayOf(0x557A), "0000557a".hexToInts())

		assertContentEquals(intArrayOf(0x1A2B_3C4D), "1A2b3c4D".hexToInts())
		assertContentEquals(intArrayOf(0x1122_3344, 0x5566), "1122334400005566".hexToInts())
	}

	@Test
	fun `can parse IntArray from hex string prefixed with 0x`() {
		assertContentEquals(intArrayOf(0x77), "0x00000077".hexToInts())
		assertContentEquals(intArrayOf(0x557A), "0x0000557A".hexToInts())

		assertContentEquals(intArrayOf(0x01010101), "0x01010101".hexToInts())
		assertContentEquals(intArrayOf(0x1122_3344, 0x5566), "0x1122334400005566".hexToInts())
	}

	@Test
	fun `can parse IntArray from hex string containing whitespace and underscores`() {
		assertContentEquals(intArrayOf(0x77), "0000_007_7".hexToInts())
		assertContentEquals(intArrayOf(0x557A), "0000 55 7A".hexToInts())

		assertContentEquals(intArrayOf(0x01010101), "01_010_101".hexToInts())
		assertContentEquals(intArrayOf(0x1122_3344, 0x5566), "1122 3344 0000_5566".hexToInts())
	}

	@Test
	fun `can parse IntArray from mixed-case hex string with 0x whitespace and underscores`() {
		assertContentEquals(intArrayOf(0x1A2B3C4D, 0x5E6F0000), "0x1A2b3 c_4D 5 e6F 0000".hexToInts())
	}

	@Test
	fun `can parse IntArray from unsigned 32-bit hex strings`() {
		assertContentEquals(intArrayOf(-1, -0x80000000), "0xFFFFFFFF 80000000".hexToInts())
	}

	@Test
	fun `failure parsing ByteArray from a string without 2 digits per byte`() {
		assertFailsWith<NumberFormatException> {
			"0x0".hexToBytes()
		}

		assertFailsWith<NumberFormatException> {
			"F".hexToBytes()
		}
	}

	@Test
	fun `failure parsing IntArray from a string without 8 digits per int`() {
		assertFailsWith<NumberFormatException> {
			"0x0".hexToBytes()
		}

		assertFailsWith<NumberFormatException> {
			"0xFFF_FFFF".hexToBytes()
		}
	}
}
