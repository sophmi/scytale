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
	fun `input is buffered until block size is reached`() {
		for ((input, _, hash, context) in vectors.bytes()) {
			expectWhirlpool(hash, context) {
				input.asSequence()
					.chunked((input.size / 3).coerceAtLeast(1))
					.forEach { add(it.toByteArray()) }

				finish()
			}
		}
	}

	@Test
	fun `addBits can add less than 8 bits per call`() {
		for ((input, sizeBits, hash, context) in vectors) {
			expectWhirlpool(hash, context) {
				val buffer = ByteArray(1)

				for (i in 0 until sizeBits) {
					val byte = (i / Byte.SIZE_BITS).toInt()
					val bit = i.toInt() % Byte.SIZE_BITS
					buffer[0] = (input[byte].toInt() ushr (7 - bit) and 1).toByte()

					addBits(buffer, count = 1)
				}

				finish()
			}
		}
	}

	@Test
	fun `addBits can add more than 8 bits per call`() {
		for ((input, sizeBits, hash, context) in vectors.filter { it.sizeBits > 0 }) {
			val chunkSize = 2

			expectWhirlpool(hash, context) {
				val buffer = ByteArray(chunkSize)

				// Feed `chunkSize * 8` bits at a time until there are at most `chunkSize * 8` bits remaining
				for (index in 0 until input.size - chunkSize step chunkSize) {
					input.copyInto(buffer, startIndex = index, endIndex = index + chunkSize)
					addBits(buffer, chunkSize * Byte.SIZE_BITS)
				}

				// Take the last 1..<chunkSize bytes
				input.copyInto(buffer, startIndex = input.size - ((input.size - 1) % chunkSize + 1))
				addBits(buffer, (sizeBits - 1) % (chunkSize * Byte.SIZE_BITS) + 1)

				finish()
			}
		}
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
