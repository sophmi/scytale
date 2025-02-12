package rs.soph.scytale.common.test

public abstract class TestInput<out T : Any>(public val input: T) {
	public abstract fun encode(): ByteArray
	public abstract fun sizeBits(): Long
	public abstract fun text(): String

	public class Raw(input: ByteArray, public val bits: Long) : TestInput<ByteArray>(input) {
		override fun encode(): ByteArray = input
		override fun sizeBits(): Long = bits
		override fun text(): String = input.contentToString()
	}

	public class Utf8(input: String) : TestInput<String>(input) {
		private val encoded by lazy(LazyThreadSafetyMode.PUBLICATION, input::encodeToByteArray)

		override fun encode(): ByteArray = encoded
		override fun sizeBits(): Long = encoded.size.toLong() * Byte.SIZE_BITS
		override fun text(): String = input
	}
}
