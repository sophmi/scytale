package rs.soph.scytale.whirlpool

import rs.soph.scytale.common.putLong
import kotlin.collections.copyInto
import kotlin.collections.fill
import kotlin.collections.fold
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.jvm.JvmOverloads
import kotlin.text.encodeToByteArray

/**
 * The WHIRLPOOL hash function (version 3.0), a cryptographic hash function with a 512-bit digest.
 *
 * This implementation slightly diverges from WHIRLPOOL by limiting the maximum size of the input
 * to `2^64 - 1` bits (~2.1 million terabytes). This should still be sufficient for casual usage.
 *
 * ### Thread safety
 * This implementation does **not** support safe concurrent access: any multithreaded usage of a
 * [Whirlpool] instance must be synchronized externally. However, the accompanying utility
 * functions, such as [Whirlpool.hash], may be invoked concurrently.
 *
 * #### The WHIRLPOOL hashing algorithm
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

	/** 8x8 matrix the block cipher operates on, `η = μ(buffer)`. */
	private var block = Matrix()

	/** Hash value as an 8x8 matrix. */
	private var hash = Matrix()

	/** Key for the current round. */
	private var key = Matrix()

	/** Intermediate matrix used while computing each round, cached to avoid reallocating. */
	private var im = Matrix()

	/** Output of the internal block cipher W. */
	private var state = Matrix()

	/**
	 * Feeds [count] bytes of [input] into the block cipher.
	 *
	 * To hash an arbitrary amount of _bits_, use [addBits].
	 *
	 * @param input The data to hash. Will not be modified.
	 */
	public fun add(input: ByteArray, offset: Int = 0, count: Int = input.size - offset) {
		if (plaintextBits % Byte.SIZE_BITS != 0L) {
			addBits(input)
		} else {
			val endIndex = offset + count
			val start = if (count < BLOCK_SIZE_BYTES - this.offset) {
				offset
			} else {
				val remaining = encipher(input, offset, endIndex)
				endIndex - remaining
			}

			input.copyInto(buffer, this.offset, start, endIndex)
			this.offset += endIndex - start
			plaintextBits += count.toLong() * Byte.SIZE_BITS
		}
	}

	/**
	 * Enciphers as many blocks from the [input] as possible.
	 *
	 * @return The amount of bytes that were not enciphered. Will be `[0, BLOCK_SIZE_BYTES)`.
	 */
	private fun encipher(input: ByteArray, start: Int, end: Int): Int {
		var nextBlock = start
		if (offset >= 0) {
			// internal buffer is partially filled, so fill it and encipher it
			nextBlock += BLOCK_SIZE_BYTES - offset
			input.copyInto(buffer, offset, start, nextBlock)
			encipherBuffer()
		}

		return encipherDirect(input, nextBlock, end)
	}

	/**
	 * Enciphers by reading directly from the [input] [ByteArray] (i.e. without copying to the
	 * internal [buffer]).
	 */
	private fun encipherDirect(input: ByteArray, start: Int, end: Int): Int {
		val limit = end - (BLOCK_SIZE_BYTES - 1)
		var offset = start

		while (offset < limit) {
			block.copyFrom(input, offset)
			cipher()
			offset += BLOCK_SIZE_BYTES
		}

		return end - offset
	}

	/**
	 * Applies the mapping function μ (map the 512-bit data [buffer] into an 8x8 matrix [block]),
	 * then the block cipher.
	 */
	private fun encipherBuffer() {
		block.copyFrom(buffer)
		offset = 0
		cipher()
	}

	/**
	 * Feeds [count] bits of [input] into the block cipher.
	 *
	 * @param input The data to hash. Will not be modified.
	 * @param count The number of bits to add.
	 */
	public fun addBits(input: ByteArray, count: Int) {
		return addBits(input, count.toLong())
	}

	/**
	 * Feeds [count] bits of [input] into the block cipher.
	 *
	 * @param input The data to hash. Will not be modified.
	 * @param count The number of bits to add.
	 */
	@JvmOverloads
	public fun addBits(input: ByteArray, count: Long = input.size.toLong() * Byte.SIZE_BITS) {
		if (count == 0L) return
		var bitOffset = (plaintextBits % BLOCK_SIZE_BITS).toInt()

		val ignored = (Byte.SIZE_BITS - (count and 7).toInt()) and 7 // amount of bits in input[off] that aren't copied
		val usedOut = bitOffset % Byte.SIZE_BITS // amount of occupied bits in buffer[offset]
		val freeOut = Byte.SIZE_BITS - usedOut // amount of available bits in buffer[offset]
		var remainingInput = count
		var pos = 0

		while (remainingInput > Byte.SIZE_BITS) {
			val b = (input[pos].toInt() shl ignored and 0xFF) or (input[++pos].toInt() and 0xFF ushr (8 - ignored))

			buffer[offset] = buffer[offset] or (b ushr usedOut).toByte()
			offset++
			bitOffset += freeOut

			if (bitOffset == BLOCK_SIZE_BITS) {
				encipherBuffer()
				bitOffset = 0
			}

			buffer[offset] = (b shl freeOut).toByte()
			bitOffset += usedOut

			remainingInput -= Byte.SIZE_BITS
		}

		val b = input[pos].toInt() shl ignored and 0xFF
		buffer[offset] = buffer[offset] or (b ushr usedOut).toByte()

		if (usedOut + remainingInput >= Byte.SIZE_BITS) {
			if (bitOffset + freeOut == BLOCK_SIZE_BITS) {
				encipherBuffer()
			} else {
				offset++
			}

			buffer[offset] = (b shl freeOut).toByte()
		}

		plaintextBits += count
	}

	/**
	 * Completes the application of the WHIRLPOOL hash function and returns the digest.
	 *
	 * The internal hash state is cleared prior to returning the digest.
	 *
	 * @param digest The [ByteArray] to write the digest to. Must be at least 64 bytes.
	 * @param offset The offset into [digest] to begin writing at. Must be `[0, digest.size - 64]`.
	 */
	@JvmOverloads
	public fun finish(digest: ByteArray = ByteArray(DIGEST_SIZE_BYTES), offset: Int = 0): ByteArray {
		try {
			@OptIn(DelicateWhirlpoolApi::class)
			finishLazy(digest, offset)
		} finally {
			reset()
		}

		return digest
	}

	/**
	 * Clears the internal hash state.
	 *
	 * Note that this does not guarantee the input no longer exists in memory, due to runtime
	 * implementation details such as the garbage collector.
	 */
	public fun reset() {
		buffer.fill(0)
		block.clear()
		hash.clear()
		key.clear()
		im.clear()
		state.clear()

		plaintextBits = 0
	}

	/**
	 * Completes the application of the WHIRLPOOL hash function and returns the digest. Unlike the
	 * safer [finish] function, this does not clear the internal cipher state.
	 *
	 * ### Delicate API
	 * This function does not clear any internal state. If this [Whirlpool] instance will be reused,
	 * [reset] or [resetLazy] **must** be called prior to feeding any additional data.
	 *
	 * @param digest The [ByteArray] to write the digest to. Must be at least 64 bytes.
	 * @param off The offset into [digest] to begin writing at. Must be `[0, digest.size - 64]`.
	 */
	@DelicateWhirlpoolApi
	public fun finishLazy(digest: ByteArray = ByteArray(DIGEST_SIZE_BYTES), off: Int = 0): ByteArray {
		// Finished reading input, so bit pad it
		val usedBits = (plaintextBits and 7).toInt()
		buffer[offset] = buffer[offset] and ((1 shl usedBits) - 1).toByte() or (0x80 ushr usedBits).toByte()

		if (++offset > FINAL_BLOCK_DATA_SIZE_BYTES) { // too large for MD-strengthening, need an additional block
			buffer.fill(0, fromIndex = offset)
			encipherBuffer()
		}

		// Pad until the bytes that will contain the message length
		// (implementation-specific: as the plaintext size is limited, need to pad until the last 8 bytes)
		buffer.fill(0, fromIndex = offset, toIndex = BLOCK_SIZE_BYTES - Long.SIZE_BYTES)

		// Merkle–Damgård strengthen
		buffer.putLong(BLOCK_SIZE_BYTES - Long.SIZE_BYTES, plaintextBits)
		encipherBuffer()

		return hash.copyInto(digest, off)
	}

	/**
	 * Re-initializes the hashing state.
	 *
	 * ### Delicate API
	 * This function clears only the minimum amount necessary to reuse this instance - input and
	 * internal state from the prior use will remain until sufficient data is written.
	 */
	@DelicateWhirlpoolApi
	public fun resetLazy() {
		hash.clear()
		plaintextBits = 0
		buffer[0] = 0 // only need to clear buffer[0]; all subsequent bytes are overwritten anyway
	}

	/**
	 * Enciphers the current [block] by applying the internal block cipher W, then Miyaguchi-Preneel.
	 */
	private fun cipher() {
		hash.copyInto(key)
		state.set { row -> block[row] xor key[row] } // σ[K^0]

		// compute K^r and ρ[K^r] for 1 <= r <= 10
		for (r in 1..ROUNDS) {
			// compute the current round key (i.e. the current round of the key schedule) from K^(r-1)
			im.set { row -> round(row, key, 0) }
			im[0] = im[0] xor ROUND_CONSTANTS[r] // σ[c^r] (only row 0 has a non-zero c^r)

			im.copyInto(key)

			// apply ρ[K^r]
			im.set { row -> round(row, state, key[row]) }
			im.copyInto(state)
		}

		// apply the Miyaguchi-Preneel compression function
		hash.set { hash[it] xor state[it] xor block[it] }
	}

	/**
	 * Applies the round function `ρ[k] = σ[k] ◦ θ ◦ π ◦ γ` to one row.
	 *
	 * Note that `θ ◦ γ` has been precomputed (see [DiffusionTables]).
	 */
	private fun round(rowIndex: Int, key: Matrix, k: Long): Long {
		return (0..<Matrix.WIDTH).fold(k) { acc, column ->
			val b = key[(rowIndex - column) and 7, column] // cycle the column (apply π)
			acc xor diffusion[b, column] // σ[k] ◦ θ ◦ γ
		}
	}

	public companion object {

		/** Number of **bits** used by a WHIRLPOOL digest. */
		public const val DIGEST_SIZE_BITS: Int = 64 * Byte.SIZE_BITS

		/** Number of **bytes** used by a WHIRLPOOL digest. */
		public const val DIGEST_SIZE_BYTES: Int = 64 * Byte.SIZE_BYTES

		/** Number of **bits** the block cipher is applied to at once. */
		internal const val BLOCK_SIZE_BITS: Int = Matrix.WIDTH * Matrix.WIDTH * Byte.SIZE_BITS // 512

		/** Number of **bytes** the block cipher is applied to at once. */
		internal const val BLOCK_SIZE_BYTES: Int = Matrix.WIDTH * Matrix.WIDTH * Byte.SIZE_BYTES // 64

		/** Number of times to apply the [round] function. */
		private const val ROUNDS: Int = 10

		/**
		 * Constants used in the key schedule, computed by `(θ ◦ π ◦ γ)`.
		 *
		 * Although the round constants are matrices, only the first row in each matrix has a
		 * non-zero value, so the others are discarded: ```ROUND_CONSTANTS[r]``` is cʳ₀ (and the
		 * `j`-th byte in the long is the constant for the column `j`).
		 *
		 * c⁰ is undefined and `ROUND_CONSTANTS[0]` is unused.
		 */
		private val ROUND_CONSTANTS = LongArray(ROUNDS + 1)

		/** Maximum amount of **data** that can be stored in the final block. */
		private const val FINAL_BLOCK_DATA_SIZE_BYTES: Int = 256 / Byte.SIZE_BITS

		/** Lookup tables for `θ ◦ γ`. */
		private val diffusion = DiffusionTables

		init {
			for (r in 1..ROUNDS) {
				val base = 8 * (r - 1)
				ROUND_CONSTANTS[r] = (diffusion[base, 0] and (0xFFL shl 56)) xor
					(diffusion[base + 1, 1] and (0xFFL shl 48)) xor
					(diffusion[base + 2, 2] and (0xFFL shl 40)) xor
					(diffusion[base + 3, 3] and (0xFFL shl 32)) xor
					(diffusion[base + 4, 4] and (0xFFL shl 24)) xor
					(diffusion[base + 5, 5] and (0xFFL shl 16)) xor
					(diffusion[base + 6, 6] and (0xFFL shl 8)) xor
					(diffusion[base + 7, 7] and 0xFFL)
			}
		}

		/**
		 * Applies the WHIRLPOOL hash function to the [data].
		 *
		 * @param data The data to hash. Will not be modified.
		 */
		public fun hash(data: ByteArray): ByteArray {
			return with(Whirlpool()) {
				add(data)
				finish()
			}
		}

		/**
		 * Applies the WHIRLPOOL hash function to the [input] String, treating it as UTF-8.
		 *
		 * @param input The plaintext to hash.
		 */
		public fun hashUtf8(input: String): ByteArray {
			return hash(input.encodeToByteArray())
		}
	}
}
