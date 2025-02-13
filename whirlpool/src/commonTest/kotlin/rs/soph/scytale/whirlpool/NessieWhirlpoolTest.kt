package rs.soph.scytale.whirlpool

import rs.soph.scytale.common.test.TestVectors
import kotlin.collections.withIndex

/**
 * Tests the [Whirlpool] hash function using the NESSIE test vectors.
 *
 * The [Whirlpool] authors included a set of test vectors with their original submission to the
 * NESSIE cryptographic conference. WHIRLPOOL version 3.0 invalidated those original test vectors,
 * but the 3.0 reference implementation includes an updated set (still referred to as "nessie test
 * vectors").
 *
 * The reference implementation for Whirlpool 3.0 and containing these test vectors is archived at
 * [archive.org](https://web.archive.org/web/20171129084214/http://www.larc.usp.br/~pbarreto/whirlpool.zip).
 */
class NessieWhirlpoolTest : WhirlpoolTest() {

	override val vectors = VECTORS

	private companion object {

		private val VECTORS = TestVectors("NESSIE") {
			for ((length, hash) in NessieTestVectors.ZEROES.withIndex()) {
				val input = ByteArray((length + 7) / Byte.SIZE_BITS)
				bits(input, length) hashesTo hash
			}

			for ((index, hash) in NessieTestVectors.SET_BIT.withIndex()) {
				val input = ByteArray(Whirlpool.BLOCK_SIZE_BYTES)
				val byte = index / Byte.SIZE_BITS
				val bit = index % Byte.SIZE_BITS
				input[byte] = (0x80 ushr bit).toByte()

				bytes(input) hashesTo hash
			}
		}
	}
}
