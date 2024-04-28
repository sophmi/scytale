package rs.soph.scytale.whirlpool

import kotlin.collections.copyInto
import kotlin.collections.fill
import kotlin.collections.fold
import kotlin.jvm.JvmOverloads
import kotlin.math.min
import kotlin.text.encodeToByteArray

/**
 * A Kotlin implementation of the WHIRLPOOL hashing function (version 3.0).
 *
 * This implementation slightly diverges from WHIRLPOOL by limiting the maximum size of the input
 * to `2^64 - 1` bits (~2.1 million terabytes). This should still be sufficient for casual usage.
 *
 * Do not reuse the block cipher implementation as part of any encryption implementation - although
 * this implementation dose use lookup tables throughout, constant-time operations are not of
 * significant importance for hash functions (and would be essentially impossible to guarantee in
 * KMP), and no effort was made to ensure the implementation is suitable for any other use.
 *
 * ## The WHIRLPOOL hashing algorithm
 * WHIRLPOOL was developed by Paulo S. L. M. Barreto and Vincent Rijmen and originally presented at
 * the first NESSIE workshop in 2000 (see 'The WHIRLPOOL hashing function', archived at
 * [archive.org](https://web.archive.org/web/20060621195406/http://www.cosic.esat.kuleuven.ac.be/nessie/workshop/submissions/whirlpool.zip)).
 *
 * The WHIRLPOOL reference implementation for version 3.0 was published and released into the public
 * domain on 2003-03-12; see the page archived at
 * [archive.org](https://web.archive.org/web/20171129084214/http://www.larc.usp.br/~pbarreto/WhirlpoolPage.html).
 *
 * Comments throughout this implementation reference symbols and terminology from the aforementioned
 * paper 'The WHIRLPOOL hashing function' (the revised version for 3.0, 2003-03-12).
 */
public class Whirlpool {

	/** Amount of bits that have been hashed (limit in this implementation: 2^64 - 1). */
	private var plaintextBits: Long = 0

	/** Buffer containing input data that has yet to be hashed. */
	private var buffer = ByteArray(BLOCK_SIZE_BYTES)

	/** Offset into the data buffer, in bytes. Will always be `[0, BLOCK_SIZE_BYTES)`. */
	private var offset = 0

	/** Offset into the data buffer, in bits. Will always be `[0, BLOCK_SIZE_BITS)`. */
	private var bitOffset = 0

	/** 8x8 matrix the block cipher operates on, `η = μ(buffer)`. */
	private var block = Matrix()

	/** Hash value as an 8x8 matrix. */
	private var hash = Matrix()

	/** Key for the current round of the cipher. */
	private var key = Matrix()

	/** Intermediate matrix used while computing each round, cached to avoid reallocating. */
	private var im = Matrix()

	/** State of the block cipher during each use. */
	private var state = Matrix()

	/**
	 * Applies the block cipher to [input]. If a hash has already been produced from this Whirlpool
	 * instance (i.e. [finish] has been called), [reset] **must** be called before adding data
	 * again.
	 *
	 * To hash an arbitrary amount of _bits_, use [addBits].
	 *
	 * @param input The plaintext data to hash.
	 */
	public fun add(input: ByteArray) {
		val bits = input.size.toLong() * Byte.SIZE_BITS

		if (bitOffset % Byte.SIZE_BITS != 0) {
			addBits(input, bits)
		} else {
			plaintextBits += bits
			var copied = 0

			while (copied < input.size) {
				val move = min(input.size - copied, buffer.size - offset)
				input.copyInto(buffer, destinationOffset = offset, startIndex = copied, endIndex = copied + move)
				copied += move
				offset += move

				if (offset == BLOCK_SIZE_BYTES) {
					encipherBuffer()
					offset = 0
				}
			}

			bitOffset = offset * Byte.SIZE_BITS
			buffer.fill(0, offset, BLOCK_SIZE_BYTES)
		}
	}

	/**
	 * Applies the block cipher to [input]. If a hash has already been produced from this Whirlpool instance (i.e.
	 * [finish] has been called), [reset] **must be called** before adding data again.
	 *
	 * @param input The plaintext data to hash.
	 * @param bits The amount of plaintext bits to process.
	 */
	public fun addBits(input: ByteArray, bits: Long) { // Partially derived from (public domain) reference impl
		plaintextBits += bits

		val ignored = (Byte.SIZE_BITS - (bits and 7).toInt()) and 7 // amount of bits in input[off] we aren't reading
		val usedOut = bitOffset and 7 // amount of bits in buffer[offset] that we've already written
		val freeOut = Byte.SIZE_BITS - usedOut
		var remainingInput = bits
		var pos = 0

		var b: Int
		while (remainingInput > Byte.SIZE_BITS) {
			b = (input[pos].toInt() shl ignored and 0xFF) or (input[++pos].toInt() and 0xFF ushr (8 - ignored))

			buffer[offset] = (buffer[offset].toInt() or (b ushr usedOut)).toByte()
			offset++
			bitOffset += freeOut

			if (bitOffset == BLOCK_SIZE_BITS) {
				encipherBuffer()
				offset = 0
				bitOffset = 0
			}

			buffer[offset] = (b shl freeOut).toByte()
			bitOffset += usedOut

			remainingInput -= Byte.SIZE_BITS
		}

		if (remainingInput > 0) {
			b = input[pos].toInt() shl ignored and 0xFF
			buffer[offset] = (buffer[offset].toInt() or (b ushr usedOut)).toByte()
		} else {
			b = 0
		}

		if (usedOut + remainingInput >= Byte.SIZE_BITS) {
			if (bitOffset + freeOut == BLOCK_SIZE_BITS) {
				encipherBuffer()
				offset = 0
				bitOffset = 0
				remainingInput -= freeOut
			} else {
				offset++
			}

			buffer[offset] = (b shl freeOut).toByte()
		}

		bitOffset += remainingInput.toInt()
	}

	/**
	 * Completes the application of the WHIRLPOOL hash function and returns the resulting digest.
	 *
	 * Users **must** call [reset] after this function if this [Whirlpool] instance will be reused.
	 */
	@JvmOverloads
	public fun finish(digest: ByteArray = ByteArray(DIGEST_BYTES)): ByteArray {
		// unconditionally pad with a 1-bit; every bit after this will be 0 (except for the message length at the end)
		buffer[offset] = (buffer[offset].toInt() or (0x80 ushr (bitOffset and 7))).toByte()

		if (++offset > FINAL_BLOCK_DATA_SIZE_BYTES) { // too large for MD-strengthening, need an additional block
			buffer.fill(0, fromIndex = offset, toIndex = BLOCK_SIZE_BYTES)

			encipherBuffer()
			offset = 0
		}

		// message length is 32 bytes, so pad to 32 to reach the block size.
		// (implementation-specific: also need to overwrite all but the last 8 bytes as that contains data from the
		// previous block - the final 8 are overwritten with the plaintext length below)
		buffer.fill(0, fromIndex = offset, toIndex = BLOCK_SIZE_BYTES - Long.SIZE_BYTES)

		// apply Merkle–Damgård strengthening
		plaintextBits.unpackInto(buffer, offset = BLOCK_SIZE_BYTES - Long.SIZE_BYTES)
		encipherBuffer()

		return hash.copyInto(digest)
	}

	/** Re-initialize the hashing state. */
	public fun reset() {
		plaintextBits = 0
		hash.clear()
		offset = 0
		bitOffset = 0
		buffer[0] = 0 // only need to clear buffer[0]; all subsequent bytes are overwritten anyway
	}

	/**
	 * Applies the mapping function μ (map the 512-bit data [buffer] into an 8x8 matrix [block]),
	 * then the block cipher.
	 */
	private fun encipherBuffer() {
		block.copyFrom(buffer)
		cipher()
	}

	/**
	 * Apply the block cipher W to the current [block].
	 */
	private fun cipher() {
		hash.copyInto(key)
		state.set { block[it] xor key[it] } // σ[K^0]

		// compute K^r and ρ[K^r] for 1 <= r <= 10
		for (r in 1..ROUNDS) {
			// compute the current round key (i.e. the current round of the key schedule) from K^(r-1)
			// pass k=0 and perform σ[c^r] below instead, otherwise we have to branch (to apply rc on one row only)
			im.set { row -> round(row, key, 0) }
			im.copyInto(key, endIndex = Matrix.WIDTH)

			// perform the final step of K^r = ρ[c^r](K^{r-1}): σ[c^r](key)
			// only key[0] needs to be set as the round constants for all other rows would be 0
			key[0] = key[0] xor ROUND_CONSTANTS[r]

			// apply ρ[K^r]
			im.set { row -> round(row, state, key[row]) }
			im.copyInto(state, endIndex = Matrix.WIDTH)
		}

		// apply the Miyaguchi-Preneel compression function
		hash.set { hash[it] xor state[it] xor block[it] }
	}

	/**
	 * Applies the round function `ρ[k] = σ[k] ◦ θ ◦ π ◦ γ` to one row.
	 *
	 * Note that `θ ◦ γ` has been precomputed (see [CirculantTables]).
	 */
	private fun round(rowIndex: Int, key: Matrix, k: Long): Long {
		return (0..<Matrix.WIDTH).fold(k) { acc, column ->
			val element = key[rowIndex - column and 7, column] // apply the cyclical permutation π
			acc xor tables[element, column] // σ[k] ◦ θ ◦ γ
		}
	}

	public companion object {

		/** Number of **bits** used by a WHIRLPOOL digest. */
		public const val DIGEST_BITS: Int = 64 * Byte.SIZE_BITS

		/** Number of **bytes** used by a WHIRLPOOL digest. */
		public const val DIGEST_BYTES: Int = 64 * Byte.SIZE_BYTES

		/** Number of **bits** the block cipher is applied to at once. */
		private const val BLOCK_SIZE_BITS: Int = Matrix.WIDTH * Matrix.WIDTH * Byte.SIZE_BITS // 512

		/** Number of **bytes** the block cipher is applied to at once. */
		private const val BLOCK_SIZE_BYTES: Int = Matrix.WIDTH * Matrix.WIDTH * Byte.SIZE_BYTES // 64

		/** Number of times to apply the [round] function. */
		private const val ROUNDS: Int = 10

		/** Constants used in the key schedule, applied as part of `σ[c^r]`. */
		private val ROUND_CONSTANTS = LongArray(ROUNDS + 1)

		/** Maximum amount of **data** that can be stored in the final block. */
		private const val FINAL_BLOCK_DATA_SIZE_BYTES: Int = 256 / Byte.SIZE_BITS

		/** Lookup tables for `θ ◦ γ`. */
		private val tables = CirculantTables

		init {
			for (r in 1..ROUNDS) {
				val index = 8 * (r - 1)
				ROUND_CONSTANTS[r] = (tables[index, 0] and (0xFFL shl 56)) xor
					(tables[index + 1, 1] and (0xFFL shl 48)) xor
					(tables[index + 2, 2] and (0xFFL shl 40)) xor
					(tables[index + 3, 3] and (0xFFL shl 32)) xor
					(tables[index + 4, 4] and (0xFFL shl 24)) xor
					(tables[index + 5, 5] and (0xFFL shl 16)) xor
					(tables[index + 6, 6] and (0xFFL shl 8)) xor
					(tables[index + 7, 7] and 0xFFL)
			}
		}

		/**
		 * Applies the WHIRLPOOL hash function to the [data].
		 */
		public fun hash(data: ByteArray): ByteArray {
			return with(Whirlpool()) {
				add(data)
				finish()
				// no need to reset(), Whirlpool instance is discarded
			}
		}
	}
}

private fun Long.unpackInto(bytes: ByteArray, offset: Int) {
	bytes[offset] = (this ushr 56).toByte()
	bytes[offset + 1] = (this ushr 48).toByte()
	bytes[offset + 2] = (this ushr 40).toByte()
	bytes[offset + 3] = (this ushr 32).toByte()
	bytes[offset + 4] = (this ushr 24).toByte()
	bytes[offset + 5] = (this ushr 16).toByte()
	bytes[offset + 6] = (this ushr 8).toByte()
	bytes[offset + 7] = this.toByte()
}
