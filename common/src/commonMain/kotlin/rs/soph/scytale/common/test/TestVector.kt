package rs.soph.scytale.common.test

public class TestVector<out T : Any>(
	public val input: TestInput<T>,
	public val hash: ByteArray,
	public val context: InputContext,
) {

	public val sizeBits: Long
		get() = input.sizeBits()

	/**
	 * Returns a view of this [TestVector] where the input is encoded to a [ByteArray].
	 */
	public fun encodedView(): EncodedView = EncodedView(input.encode(), input.sizeBits(), hash, context)

	/**
	 * Returns a view of this [TestVector] where the input is not encoded (i.e. is of type [T]).
	 */
	public fun inputView(): InputView<T> = InputView(input.input, hash, context)

	@Suppress("ArrayInDataClass")
	public data class InputView<out T : Any>(val input: T, val hash: ByteArray, val context: InputContext)

	@Suppress("ArrayInDataClass")
	public data class EncodedView(val input: ByteArray, val bits: Long, val hash: ByteArray, val context: InputContext)
}

public class InputContext(public val source: String, public val index: Int, public val inputText: () -> String) {
	public val name: String = "$source test vector #$index"

	public fun abbreviateInput(): String {
		val text = inputText()

		return if (text.length <= MAX_PRINTABLE_INPUT) {
			text
		} else {
			val abbreviated = text.take(MAX_PRINTABLE_INPUT).apply {
				if (last() in HIGH_SURROGATE_RANGE) { // We've split a surrogate pair, so drop the high surrogate too
					dropLast(1)
				}
			}

			"$abbreviated..."
		}
	}

	private companion object {
		private val HIGH_SURROGATE_RANGE = Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE
		private const val MAX_PRINTABLE_INPUT = 30
	}
}
