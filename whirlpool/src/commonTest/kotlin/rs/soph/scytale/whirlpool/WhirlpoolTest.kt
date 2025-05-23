package rs.soph.scytale.whirlpool

import rs.soph.scytale.common.test.InputContext
import rs.soph.scytale.common.test.TestVectors
import kotlin.test.Test
import kotlin.test.assertContentEquals

@OptIn(DelicateWhirlpoolApi::class)
abstract class WhirlpoolTest {

	protected abstract val vectors: TestVectors

	@Test
	fun `can hash input`() {
		for ((input, _, hash, context) in vectors.bytes()) {
			expectWhirlpool(hash, context) {
				add(input)
				finish()
			}
		}
	}

	@Test
	fun `can hash UTF8 strings`() {
		for ((input, ciphertext, context) in vectors.utf8()) {
			expectWhirlpool(ciphertext, context) {
				Whirlpool.hashUtf8(input)
			}
		}
	}

	@Test
	fun `can hash part of a ByteArray`() {
		val offset = 5

		for ((input, _, hash, context) in vectors.bytes()) {
			val buffer = input.copyInto(ByteArray(input.size + offset), offset)

			expectWhirlpool(hash, context) {
				add(buffer, offset, input.size)
				finish()
			}
		}
	}

	@Test
	fun `addBits can add less than 8 bits per call`() {
		for ((input, sizeBits, hash, context) in vectors) {
			expectWhirlpool(hash, context) {
				val buffer = ByteArray(1)

				for (bitOffset in 0 until sizeBits - 7 step Byte.SIZE_BITS.toLong()) {
					val offset = (bitOffset / Byte.SIZE_BITS).toInt()
					val shift = (bitOffset % Byte.SIZE_BITS).toInt()

					buffer[0] = (input[offset].toInt() ushr shift).toByte()
					addBits(buffer, 0, Byte.SIZE_BITS - shift)

					addBits(input, offset, shift)
				}

				val remainingBits = sizeBits % Byte.SIZE_BITS
				addBits(input, input.size - 1, remainingBits)

				finish()
			}
		}
	}

	@Test
	fun `addBits can add more than 8 bits per call`() {
		for ((input, sizeBits, hash, context) in vectors) {
			expectWhirlpool(hash, context) {
				addChunks(input, sizeBits, chunkSize = 3)
				finish()
			}

			expectWhirlpool(hash, context) {
				addChunks(input, sizeBits, chunkSize = Whirlpool.BLOCK_SIZE_BITS)
				finish()
			}
		}
	}

	@Test
	fun `can interleave add and addBits calls`() {
		for ((input, sizeBits, hash, context) in vectors) {
			expectWhirlpool(hash, context) {
				var i = 0
				addChunks(input, sizeBits, chunkSize = 3) { input, offset, length ->
					if (i++ and 1 == 0) {
						add(input, offset, length)
					} else {
						addBits(input, offset, length * Byte.SIZE_BITS)
					}
				}

				finish()
			}
		}
	}

	protected fun Whirlpool.addChunks(
		input: ByteArray,
		sizeBits: Long,
		chunkSize: Int,
		feed: Whirlpool.(ByteArray, Int, Int) -> Unit = Whirlpool::add,
	) {
		if (sizeBits == 0L) {
			return
		}

		// Feed `chunkSize * 8` bits at a time until there are at most `chunkSize * 8` bits remaining
		for (index in 0 until input.size - chunkSize step chunkSize) {
			feed(input, index, chunkSize)
		}

		// Feed the last `1..(chunkSize * 8)` bits
		val byteOffset = input.size - ((input.size - 1) % chunkSize + 1)
		val remainingBits = (sizeBits - 1) % (chunkSize * Byte.SIZE_BITS) + 1
		addBits(input, byteOffset, remainingBits)
	}
}

internal inline fun expectWhirlpool(
	expected: ByteArray,
	context: InputContext? = null,
	actual: Whirlpool.() -> ByteArray,
) {
	val value = Whirlpool().actual()

	// Wrap the kotlin.test assert function, so the message is created lazily
	if (!expected.contentEquals(value)) {
		val message = context?.let { "${it.name} failed (input=${it.abbreviateInput()})" }
		assertContentEquals(expected, value, message)
	}
}
