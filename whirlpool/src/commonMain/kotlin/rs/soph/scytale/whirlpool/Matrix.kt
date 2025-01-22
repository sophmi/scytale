@file:Suppress("NOTHING_TO_INLINE")

package rs.soph.scytale.whirlpool

import rs.soph.scytale.common.getLong
import rs.soph.scytale.common.putLong
import kotlin.collections.copyInto
import kotlin.collections.fill
import kotlin.jvm.JvmInline

/**
 * An 8x8 Matrix used by [Whirlpool].
 */
@JvmInline
internal value class Matrix(private val data: LongArray = LongArray(WIDTH)) {

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
		repeat(WIDTH) { row ->
			this[row] = function(row)
		}
	}

	fun copyFrom(source: ByteArray) {
		repeat(WIDTH) { row ->
			data[row] = source.getLong(row * Long.SIZE_BYTES)
		}
	}

	fun copyInto(destination: ByteArray): ByteArray {
		repeat(WIDTH) { row ->
			destination.putLong(row * Long.SIZE_BYTES, data[row])
		}

		return destination
	}

	inline fun copyInto(destination: Matrix): LongArray {
		return data.copyInto(destination.data)
	}

	companion object {

		/** Width of the NxN matrix (i.e. N = 8). */
		const val WIDTH: Int = 8
	}
}
