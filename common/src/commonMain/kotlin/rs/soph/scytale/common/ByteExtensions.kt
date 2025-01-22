@file:Suppress("NOTHING_TO_INLINE")

package rs.soph.scytale.common

/**
 * Assembles a `Long` from eight bytes (represented as `Int`s). Each `Int` must **not** have any of
 * its 24 highest bits set.
 */
public inline fun longFromBytes(msb: Int, b6: Int, b5: Int, b4: Int, b3: Int, b2: Int, b1: Int, lsb: Int): Long {
	return (msb.toLong() shl 56) or
		(b6.toLong() shl 48) or
		(b5.toLong() shl 40) or
		(b4.toLong() shl 32) or
		(b3.toLong() shl 24) or
		(b2.toLong() shl 16) or
		(b1.toLong() shl 8) or
		lsb.toLong()
}
