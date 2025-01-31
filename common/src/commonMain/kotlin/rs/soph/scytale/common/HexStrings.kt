@file:OptIn(ExperimentalStdlibApi::class)

package rs.soph.scytale.common

/**
 * Formats this [ByteArray] as an uppercase hex string. Each byte is represented as two chars, with leading 0s inserted
 * if necessary. Every four bytes is separated by a `_`, starting from the left:
 * - `byteArrayOf(0x1).toUpperHexString() == "0x01"`
 * - `byteArrayOf(1, 2, 3, 4, 5).toUpperHexString() == "0x01020304_05"`
 *
 * @param prefix If `true`, prepends `0x` to the output string.
 */
public fun ByteArray.toUpperHexString(prefix: Boolean = true): String {
	val hex = toHexString(BYTE_ARRAY_FORMAT)
	return if (prefix) "0x$hex" else hex
}

/**
 * Formats this [IntArray] as an uppercase hex string. Each int is represented as eight chars, with leading 0s
 * inserted if necessary. Every int is separated by a `_`, e.g.:
 * - `intArrayOf(0x1).toUpperHexString() == "0x00000001"`
 * - `intArrayOf(0x8000, 0x80).toUpperHexString() == "0x00008000_00000080"`
 *
 * @param prefix If `true`, prepends `0x` to the output string.
 */
public fun IntArray.toUpperHexString(prefix: Boolean = true): String {
	val prefix = if (prefix) "0x" else ""
	return joinToString(separator = "_", prefix) { it.toHexString(INT_FORMAT) }
}

/**
 * Parses a [ByteArray] from this hex string, ignoring case, whitespace, underscores, and the prefix `0x`. Each byte
 * **must** be represented as two chars, e.g. `01` not `1`, `0x0A` not `0xA`.
 */
public fun String.hexToBytes(): ByteArray {
	return filterNot { it == '_' || it.isWhitespace() }
		.removePrefix("0x")
		.hexToByteArray()
}

/**
 * Parses an [IntArray] from this hex string, ignoring case, whitespace, underscores, and the prefix `0x`. Each int
 *  * **must** be represented as eight chars, e.g. `00000001` not `1`, `0x00AAAAAA` not `0xAAAAAA`.
 */
public fun String.hexToInts(): IntArray {
	val bytes = filterNot { it == '_' || it.isWhitespace() }
		.removePrefix("0x")
		.hexToByteArray(INT_ARRAY_FORMAT)

	if (bytes.size % Int.SIZE_BYTES != 0) {
		throw NumberFormatException("Insufficient input: expected exactly 8 hexadecimal digits per int.")
	}

	return IntArray(bytes.size / Int.SIZE_BYTES) { bytes.getInt(it * Int.SIZE_BYTES) }
}

private val BYTE_ARRAY_FORMAT = HexFormat {
	upperCase = true
	bytes {
		bytesPerGroup = 4
		groupSeparator = "_"
	}
}

private val INT_ARRAY_FORMAT = HexFormat {
	upperCase = true
	bytes {
		bytesPerGroup = 4
		groupSeparator = ""
	}
}

private val INT_FORMAT = HexFormat {
	upperCase = true
	number.minLength = Int.SIZE_BITS / BITS_PER_HEX_CHAR // use 8 chars per int
}

private const val BITS_PER_HEX_CHAR = 4
