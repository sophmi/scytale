package rs.soph.scytale.whirlpool

import kotlin.collections.map
import kotlin.collections.withIndex
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.text.hexToByteArray

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
	@JsName("input_of_zeroes") // TODO remove after 2.0.0
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
	@JsName("input_of_single_set_bit") // TODO remove after 2.0.0
	fun `inputs containing a single set bit match`() {
		for ((index, expected) in SET_BIT_VECTORS.withIndex()) {
			val input = ByteArray(SINGLE_SET_BIT_INPUT_SIZE)
			val byte = index / Byte.SIZE_BITS
			val bit = index % Byte.SIZE_BITS
			input[byte] = (0x80 ushr bit).toByte()

			val actual = Whirlpool.hash(input)
			assertContentEquals(expected, actual, "Input $index failed to match")
		}
	}

	@OptIn(ExperimentalStdlibApi::class)
	private companion object {

		private const val SINGLE_SET_BIT_INPUT_SIZE = 512 / Byte.SIZE_BITS

		private val ZEROES_VECTORS = NessieTestVectors.ZEROES.map(String::hexToByteArray)
		private val SET_BIT_VECTORS = NessieTestVectors.SET_BIT.map(String::hexToByteArray)

	}

}
