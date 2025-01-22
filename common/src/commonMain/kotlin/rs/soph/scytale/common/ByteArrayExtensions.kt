@file:Suppress("NOTHING_TO_INLINE")

package rs.soph.scytale.common

/**
 * Returns eight bytes as a `Long`, in big-endian order.
 * @param offset The index of the most-significant byte of the `Long`.
 */
public inline fun ByteArray.getLong(offset: Int = 0): Long {
	return this[offset].toLong() shl 56 or
		(this[offset + 1].toLong() and 0xFF shl 48) or
		(this[offset + 2].toLong() and 0xFF shl 40) or
		(this[offset + 3].toLong() and 0xFF shl 32) or
		(this[offset + 4].toLong() and 0xFF shl 24) or
		(this[offset + 5].toLong() and 0xFF shl 16) or
		(this[offset + 6].toLong() and 0xFF shl 8) or
		(this[offset + 7].toLong() and 0xFF)
}

/**
 * Writes a `Long` to this [ByteArray], in big-endian order.
 * @param offset The index to write the first byte to.
 * @param value The `Long` to write.
 */
public inline fun ByteArray.putLong(offset: Int, value: Long) {
	this[offset] = (value ushr 56).toByte()
	this[offset + 1] = (value ushr 48).toByte()
	this[offset + 2] = (value ushr 40).toByte()
	this[offset + 3] = (value ushr 32).toByte()
	this[offset + 4] = (value ushr 24).toByte()
	this[offset + 5] = (value ushr 16).toByte()
	this[offset + 6] = (value ushr 8).toByte()
	this[offset + 7] = value.toByte()
}
