@file:Suppress("NOTHING_TO_INLINE")

package rs.soph.scytale.whirlpool

import kotlin.collections.copyInto
import kotlin.collections.fill
import kotlin.jvm.JvmInline

/**
 * An 8x8 Matrix used by [Whirlpool].
 */
@JvmInline
internal value class Matrix(private val data: LongArray = LongArray(SIZE_LONGS)) {

	inline fun clear() {
		data.fill(0)
	}

	inline operator fun get(index: Int): Long = data[index]

	inline operator fun get(row: Int, column: Int): Int {
		return (data[row] ushr (56 - column * Byte.SIZE_BITS)).toInt() and 0xFF
	}

	inline operator fun set(index: Int, value: Long) {
		data[index] = value
	}

	/**
	 * Sets each row in this Matrix to the value returned by the [function] (called once per row).
	 */
	inline fun set(function: (Int) -> Long) {
		repeat(SIZE_LONGS) { row ->
			this[row] = function(row)
		}
	}

	fun copyFrom(source: ByteArray, offset: Int = 0) {
		var index = offset
		repeat(SIZE_LONGS) { row ->
			data[row] = source[index++].toLong() shl 56 or
				(source[index++].ulong() shl 48) or
				(source[index++].ulong() shl 40) or
				(source[index++].ulong() shl 32) or
				(source[index++].ulong() shl 24) or
				(source[index++].ulong() shl 16) or
				(source[index++].ulong() shl 8) or
				source[index++].ulong()
		}
	}

	fun copyInto(
		destination: ByteArray,
		destinationOffset: Int = 0,
		startIndex: Int = 0,
		endIndex: Int = SIZE_LONGS,
	): ByteArray {
		var offset = destinationOffset
		for (row in startIndex..<endIndex) {
			val value = data[row]
			destination[offset++] = (value ushr 56).toByte()
			destination[offset++] = (value ushr 48).toByte()
			destination[offset++] = (value ushr 40).toByte()
			destination[offset++] = (value ushr 32).toByte()
			destination[offset++] = (value ushr 24).toByte()
			destination[offset++] = (value ushr 16).toByte()
			destination[offset++] = (value ushr 8).toByte()
			destination[offset++] = value.toByte()
		}

		return destination
	}

	inline fun copyInto(
		destination: Matrix,
		destinationOffset: Int = 0,
		startIndex: Int = 0,
		endIndex: Int = SIZE_LONGS,
	): LongArray {
		return data.copyInto(destination.data, destinationOffset, startIndex, endIndex)
	}

	companion object {

		/** Size of the matrix in `Long`s. */
		const val SIZE_LONGS: Int = 8

		/** Width of the NxN hash state matrix (i.e. N = 8). */
		const val WIDTH: Int = 8
	}
}

private inline fun Byte.ulong(): Long = toLong() and 0xFF
