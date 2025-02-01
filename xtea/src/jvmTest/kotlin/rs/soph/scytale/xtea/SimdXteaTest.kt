package rs.soph.scytale.xtea

import kotlin.test.Test
import kotlin.test.assertContentEquals

class SimdXteaTest {

	@Test
	fun `can encipher and decipher SIMD xtea`() {
		val key = intArrayOf(1, 2, 3, 4)

		val input = intArrayOf(
			0x227D20BC.toInt(), 0x509A07B3, 0x3468E912, 0xD6BA27C7.toInt(),
			0x868B46D4.toInt(), 0x12A4874F, 0x842CF970.toInt(), 0xD7ABDCB0.toInt(),
			0x84059FEF.toInt(), 0x6F5B1E8D, 0xBC3F779C.toInt(), 0x6F18C567,
			0x1685F874, 0x452D5011, 0xB42E7D4A.toInt(), 0x2F7641E6,
		)

		val expected = intArrayOf(
			0x784E1CD1, 0x5BF1588D, 0xE2224E86.toInt(), 0xF697C5B8.toInt(),
			0xE59C8A1A.toInt(), 0xEC39BA55.toInt(), 0xCF14E384.toInt(), 0xC98425D4.toInt(),
			0x6A86C432, 0xB2BFC7A7.toInt(), 0xD7262439.toInt(), 0x178B86F7,
			0x7371E5AA, 0x4EEFFA15, 0xEADB3C6B.toInt(), 0xC266CC31.toInt(),
		)

		val enciphered = input.clone()

		SimdXtea.encipherSimd(enciphered, key)
		assertContentEquals(expected, enciphered)

		SimdXtea.decipherSimd(enciphered, key)
		assertContentEquals(input, enciphered)
	}
}
