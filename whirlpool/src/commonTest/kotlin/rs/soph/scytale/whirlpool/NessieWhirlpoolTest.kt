package rs.soph.scytale.whirlpool

import rs.soph.scytale.common.hexToBytes
import kotlin.collections.map
import kotlin.collections.withIndex
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Tests the [Whirlpool] hash function using the "nessie test vectors", an updated version of the
 * test vectors provided as part of the original reference implementation of Whirlpool-0.
 *
 * The authors included a set of test vectors with their original submission to the NESSIE
 * cryptographic conference. The later version, WHIRLPOOL 3.0, invalidated those original test
 * vectors, but the 3.0 reference implementation includes an updated set (still referred to as
 * "nessie test vectors").
 *
 * The reference implementation for Whirlpool 3.0 and containing these test vectors is archived at
 * [archive.org](https://web.archive.org/web/20171129084214/http://www.larc.usp.br/~pbarreto/whirlpool.zip).
 */
class NessieWhirlpoolTest {

	@Test
	fun `inputs of zeroes match`() {
		for ((length, expected) in ZEROES_VECTORS.withIndex()) {
			val digest = with(Whirlpool()) {
				addBits(ByteArray((length + 7) / Byte.SIZE_BITS), length.toLong())
				finish()
			}

			assertContentEquals(expected, digest, "Input $length failed to match")
		}
	}

	@Test
	fun `single set bit inputs match`() {
		for ((index, expected) in SET_BIT_VECTORS.withIndex()) {
			val input = ByteArray(Whirlpool.DIGEST_SIZE_BYTES)
			val byte = index / Byte.SIZE_BITS
			val bit = index % Byte.SIZE_BITS
			input[byte] = (0x80 ushr bit).toByte()

			val actual = Whirlpool.hash(input)
			assertContentEquals(expected, actual, "Input $index failed to match")
		}
	}

	private companion object {

		private val ZEROES_VECTORS = NessieTestVectors.ZEROES.map(String::hexToBytes)
		private val SET_BIT_VECTORS = NessieTestVectors.SET_BIT.map(String::hexToBytes)
	}
}
