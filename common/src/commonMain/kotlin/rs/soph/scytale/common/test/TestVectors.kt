package rs.soph.scytale.common.test

import rs.soph.scytale.common.hexToBytes

public inline fun TestVectors(source: String, builder: TestVectors.Builder.() -> Unit): TestVectors {
	return TestVectors.Builder(source).apply(builder).build()
}

public class TestVectors(private val vectors: List<TestVector<Any>>) : Iterable<TestVector.EncodedView> {

	/**
	 * Returns a [Sequence] containing all encoded test vectors that have an exact size in bytes
	 * (i.e. are a multiple of 8 bits).
	 */
	public fun bytes(): Sequence<TestVector.EncodedView> {
		return filter { (it.sizeBits % Byte.SIZE_BITS).toInt() == 0 }
	}

	/**
	 * Returns a [Sequence] containing all Utf8 test vectors, as Strings.
	 */
	public fun utf8(): Sequence<TestVector.InputView<String>> {
		@Suppress("UNCHECKED_CAST")
		return vectors.asSequence()
			.filter { it.input is TestInput.Utf8 }
			.map { (it as TestVector<String>).inputView() }
	}

	public fun asSequence(): Sequence<TestVector.EncodedView> {
		return vectors.asSequence().map(TestVector<Any>::encodedView)
	}

	public fun filter(predicate: (TestVector<Any>) -> Boolean): Sequence<TestVector.EncodedView> {
		return vectors.asSequence()
			.filter(predicate)
			.map(TestVector<Any>::encodedView)
	}

	override fun iterator(): Iterator<TestVector.EncodedView> {
		return asSequence().iterator()
	}

	public class Builder(private val source: String) {
		private val vectors = mutableListOf<TestVector<Any>>()

		public fun <T : Any> add(plaintext: TestVector<T>) {
			vectors += plaintext
		}

		public fun bits(input: ByteArray, bits: Int): TestInput.Raw = bits(input, bits.toLong())
		public fun bits(input: ByteArray, bits: Long = input.size.toLong() * Byte.SIZE_BITS): TestInput.Raw {
			return TestInput.Raw(input, bits)
		}

		public fun bytes(input: ByteArray): TestInput.Raw = bits(input)
		public fun bytes(input: String): TestInput.Raw = bits(input.hexToBytes())

		public fun utf8(input: String): TestInput.Utf8 = TestInput.Utf8(input)

		public fun build(): TestVectors {
			return TestVectors(vectors)
		}

		public infix fun <T : Any> TestInput<T>.hashesTo(hash: String) = hashesTo(hash.hexToBytes())

		public infix fun <T : Any> TestInput<T>.hashesTo(hash: ByteArray) {
			val context = InputContext(source, vectors.size, ::text)
			add(TestVector(this, hash, context))
		}
	}
}
