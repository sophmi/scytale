package rs.soph.scytale.xtea

import rs.soph.scytale.common.getInt
import rs.soph.scytale.common.putInt
import kotlin.jvm.JvmOverloads

/**
 * The XTEA block cipher, a lightweight Feistel cipher with a 128-bit key.
 *
 * ### Thread safety
 * All functions in this class support safe concurrent access.
 *
 * #### The XTEA (eXtended TEA) block cipher
 * XTEA was developed by David Wheeler and Roger Needham as an extension to their earlier block
 * cipher TEA (see 'TEA, a Tiny Encryption Algorithm', archived at
 * [archive.org](https://web.archive.org/web/20240522143340/https://www.cix.co.uk/~klockstone/tea.pdf)).
 * XTEA was first released in an (unpublished) paper in 1997 (see 'Tea extensions', archived at
 * [archive.org](https://web.archive.org/web/20241215001824/https://www.cix.co.uk/~klockstone/xtea.pdf)).
 */
public object Xtea {

	/**
	 * Enciphers the input [ByteArray] in-place using the given [key].
	 *
	 * XTEA uses a 64-bit block size and so the input must be a multiple of 8 bytes, i.e.
	 * `(length - offset) % 8 == 0 && length > offset`.
	 *
	 * Blocks are read and written in big-endian order.
	 *
	 * @param input The plaintext to encipher. Must contain at least 8 bytes.
	 * @param key The 128-bit key to encipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param offset The offset into the [input] array. Must be `[0, length - 8]`.
	 * @param length The amount of bytes to encipher, starting from the [offset].
	 */
	@JvmOverloads
	public fun encipher(input: ByteArray, key: IntArray, offset: Int = 0, length: Int = input.size - offset) {
		for (index in offset until offset + length step BLOCK_SIZE_BYTES) {
			val v0 = input.getInt(index)
			val v1 = input.getInt(index + Int.SIZE_BYTES)

			encipher(v0, v1, key) { y, z ->
				input.putInt(index, y)
				input.putInt(index + Int.SIZE_BYTES, z)
			}
		}
	}

	/**
	 * Enciphers the input [IntArray] in-place using the given [key].
	 *
	 * XTEA uses a 64-bit block size and so the input must be a multiple of 2 ints, i.e.:
	 * - `(length - offset) >= 2`.
	 * - `(length - offset) % 2 == 0`.
	 *
	 * @param input The plaintext to encipher. Must contain at least 2 ints.
	 * @param key The 128-bit key to encipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param offset The offset into the [input] array. Must be `[0, length - 2]`.
	 * @param length The amount of ints to encipher, starting from the [offset].
	 */
	@JvmOverloads
	public fun encipher(input: IntArray, key: IntArray, offset: Int = 0, length: Int = input.size - offset) {
		for (index in offset until offset + length step BLOCK_SIZE_INTS) {
			encipher(input[index], input[index + 1], key) { y, z ->
				input[index] = y
				input[index + 1] = z
			}
		}
	}

	/**
	 * Enciphers the two ints [v0], [v1] using the given [key].
	 *
	 * @param v0 The first int to encipher.
	 * @param v1 The second int to encipher.
	 * @param key The 128-bit key to encipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param out A lambda to pass the enciphered ([v0], [v1]) to.
	 */
	public inline fun encipher(v0: Int, v1: Int, key: IntArray, out: (y: Int, z: Int) -> Unit) {
		var y = v0
		var z = v1

		var sum = 0
		repeat(CYCLES) {
			y += (((z shl 4) xor (z ushr 5)) + z) xor (sum + key[sum and 3])
			sum += GOLDEN_RATIO
			z += (((y shl 4) xor (y ushr 5)) + y) xor (sum + key[sum ushr 11 and 3])
		}

		out(y, z)
	}

	/**
	 * Deciphers the input [ByteArray] in-place using the given [key].
	 *
	 * XTEA uses a 64-bit block size and so the input must be a multiple of 8 bytes, i.e.:
	 * - `(length - offset) >= 8`.
	 * - `(length - offset) % 8 == 0`.
	 *
	 * Blocks are read and written in big-endian order.
	 *
	 * @param input The ciphertext to decipher. Must contain at least 8 bytes.
	 * @param key The 128-bit key to decipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param offset The offset into the [input] array. Must be `[0, length - 8]`.
	 * @param length The amount of bytes to decipher, starting from the [offset].
	 */
	@JvmOverloads
	public fun decipher(input: ByteArray, key: IntArray, offset: Int = 0, length: Int = input.size - offset) {
		for (index in offset until offset + length step BLOCK_SIZE_BYTES) {
			val v0 = input.getInt(index)
			val v1 = input.getInt(index + Int.SIZE_BYTES)

			decipher(v0, v1, key) { y, z ->
				input.putInt(index, y)
				input.putInt(index + Int.SIZE_BYTES, z)
			}
		}
	}

	/**
	 * Deciphers the input [IntArray] in-place using the given [key].
	 *
	 * XTEA uses a 64-bit block size and so the input must be a multiple of 2 ints, i.e.:
	 * - `(length - offset) >= 2`.
	 * - `(length - offset) % 2 == 0`.
	 *
	 * @param input The ciphertext to decipher. Must contain at least 2 ints.
	 * @param key The 128-bit key to decipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param offset The offset into the [input] array. Must be `[0, length - 2]`.
	 * @param length The amount of ints to decipher, starting from the [offset].
	 */
	@JvmOverloads
	public fun decipher(input: IntArray, key: IntArray, offset: Int = 0, length: Int = input.size - offset) {
		for (index in offset until offset + length step BLOCK_SIZE_INTS) {
			decipher(input[index], input[index + 1], key) { y, z ->
				input[index] = y
				input[index + 1] = z
			}
		}
	}

	/**
	 * Deciphers the two ints [v0], [v1] using the given [key].
	 *
	 * @param v0 The first int to decipher.
	 * @param v1 The second int to decipher.
	 * @param key The 128-bit key to decipher with. Must contain at least 4 ints (additional indices are ignored).
	 * @param out A lambda to pass the deciphered ([v0], [v1]) to.
	 */
	public inline fun decipher(v0: Int, v1: Int, key: IntArray, out: (y: Int, z: Int) -> Unit) {
		var y = v0
		var z = v1

		var sum = GOLDEN_RATIO * CYCLES
		repeat(CYCLES) {
			z -= (((y shl 4) xor (y ushr 5)) + y) xor (sum + key[sum ushr 11 and 3])
			sum -= GOLDEN_RATIO
			y -= (((z shl 4) xor (z ushr 5)) + z) xor (sum + key[sum and 3])
		}

		out(y, z)
	}

	/** The size of an XTEA key: 128 bits. */
	public const val KEY_SIZE_BITS: Int = 4 * Int.SIZE_BITS

	/** The size of an XTEA key, in bytes: 16 bytes. */
	public const val KEY_SIZE_BYTES: Int = KEY_SIZE_BITS / Byte.SIZE_BITS

	/** The size of an XTEA key, in ints: 4 ints. */
	public const val KEY_SIZE_INTS: Int = KEY_SIZE_BITS / Int.SIZE_BITS

	/** The size of an XTEA block: 64 bits. */
	public const val BLOCK_SIZE_BITS: Int = 64

	/** The size of an XTEA block, in bytes: 8 bytes. */
	public const val BLOCK_SIZE_BYTES: Int = BLOCK_SIZE_BITS / Byte.SIZE_BITS

	/** The size of an XTEA block, in ints: 2 ints. */
	public const val BLOCK_SIZE_INTS: Int = BLOCK_SIZE_BITS / Int.SIZE_BITS

	private const val FEISTEL_ROUNDS = 64

	@PublishedApi
	internal const val CYCLES: Int = FEISTEL_ROUNDS / 2

	@PublishedApi
	internal const val GOLDEN_RATIO: Int = 0x9E3779B9.toInt()
}
