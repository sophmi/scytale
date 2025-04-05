package rs.soph.scytale.whirlpool

import kotlin.test.Test

@OptIn(DelicateWhirlpoolApi::class)
class WhirlpoolApiTest {

	@Test
	fun `can hash array`() {
		val input = "test".encodeToByteArray()

		val expected = with(Whirlpool()) {
			add(input)
			finish()
		}

		expectWhirlpool(expected) {
			Whirlpool.hash(input)
		}
	}

	@Test
	fun `can write digest at an offset`() {
		val input = "test".encodeToByteArray()
		val offset = 7

		val expected = Whirlpool.hash(input).copyInto(ByteArray(Whirlpool.DIGEST_SIZE_BYTES + offset), offset)

		expectWhirlpool(expected) {
			add(input)
			finish(ByteArray(Whirlpool.DIGEST_SIZE_BYTES + offset), offset)
		}

		expectWhirlpool(expected) {
			add(input)
			finishLazy(ByteArray(Whirlpool.DIGEST_SIZE_BYTES + offset), offset)
		}
	}

	@Test
	fun `finish clears the internal state`() {
		val emptyHash = Whirlpool.hash(ByteArray(0))

		expectWhirlpool(emptyHash) {
			// Change the internal state from the default
			add("test".encodeToByteArray())

			// Compute a hash and (hopefully) clear the internal state
			finish()

			// Compute a digest, which will give `emptyHash` if the internal state was cleared correctly
			finishLazy()
		}
	}

	@Test
	fun `resetLazy correctly reinitializes the hash state`() {
		val input = "test".encodeToByteArray()
		val expected = Whirlpool.hash(input)

		expectWhirlpool(expected) {
			add("data".encodeToByteArray())
			finishLazy()
			resetLazy()

			add(input)
			finish()
		}
	}
}
