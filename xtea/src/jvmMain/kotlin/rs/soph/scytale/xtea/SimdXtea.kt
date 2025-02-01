@file:Suppress("NOTHING_TO_INLINE")

package rs.soph.scytale.xtea

import jdk.incubator.vector.IntVector
import jdk.incubator.vector.VectorOperators.LSHL
import jdk.incubator.vector.VectorOperators.LSHR
import jdk.incubator.vector.VectorOperators.XOR
import rs.soph.scytale.xtea.Xtea.CYCLES
import rs.soph.scytale.xtea.Xtea.GOLDEN_RATIO
import kotlin.jvm.JvmOverloads

public object SimdXtea {

	@JvmOverloads
	public fun encipherSimd(input: IntArray, key: IntArray, offset: Int = 0) {
		val sequential = (input.size - offset) % LANES

		val offset = if (sequential != 0) {
			Xtea.encipher(input, key, offset, length = sequential)
			offset + sequential
		} else {
			offset
		}

		var y = IntVector.fromArray(IntVector.SPECIES_PREFERRED, input, offset, Y_INDEX_MAP, /* mapOffset = */ 0)
		var z = IntVector.fromArray(IntVector.SPECIES_PREFERRED, input, offset, Z_INDEX_MAP, /* mapOffset = */ 0)

		encipherSimd(y, z, key) { v0, v1 ->
			v0.intoArray(input, offset, Y_INDEX_MAP, /* mapOffset = */ 0)
			v1.intoArray(input, offset, Z_INDEX_MAP, /* mapOffset = */ 0)
		}
	}

	public inline fun encipherSimd(y: IntVector, z: IntVector, key: IntArray, out: (IntVector, IntVector) -> Unit) {
		var y = y
		var z = z

		var sum = 0
		repeat(CYCLES) {
			val dy = z.lanewise(LSHL, 4).lanewise(XOR, z.lanewise(LSHR, 5)).add(z) // ((z << 4) ^ (z >>> 5) + z)
				.lanewise(XOR, sum + key[sum and 3]) // ^ (sum + key[sum & 3])
			y = y.add(dy)

			sum += GOLDEN_RATIO

			val dz = y.lanewise(LSHL, 4).lanewise(XOR, y.lanewise(LSHR, 5)).add(y) // ((y << 4) ^ (y >>> 5) + y)
				.lanewise(XOR, sum + key[sum ushr 11 and 3]) // ^ (sum + key[sum >>> 11 & 3])
			z = z.add(dz)
		}

		out(y, z)
	}

	@JvmOverloads
	public fun decipherSimd(input: IntArray, key: IntArray, offset: Int = 0) {
		val sequential = (input.size - offset) % LANES

		val offset = if (sequential != 0) {
			Xtea.decipher(input, key, offset, length = sequential)
			offset + sequential
		} else {
			offset
		}

		var y = IntVector.fromArray(IntVector.SPECIES_PREFERRED, input, offset, Y_INDEX_MAP, /* mapOffset = */ 0)
		var z = IntVector.fromArray(IntVector.SPECIES_PREFERRED, input, offset, Z_INDEX_MAP, /* mapOffset = */ 0)

		decipherSimd(y, z, key) { v0, v1 ->
			v0.intoArray(input, offset, Y_INDEX_MAP, /* mapOffset = */ 0)
			v1.intoArray(input, offset, Z_INDEX_MAP, /* mapOffset = */ 0)
		}
	}

	public inline fun decipherSimd(y: IntVector, z: IntVector, key: IntArray, out: (IntVector, IntVector) -> Unit) {
		var y = y
		var z = z

		var sum = CYCLES * GOLDEN_RATIO
		repeat(CYCLES) {
			val dz = y.lanewise(LSHL, 4).lanewise(XOR, y.lanewise(LSHR, 5)).add(y) // ((y << 4) ^ (y >>> 5) + y)
				.lanewise(XOR, sum + key[sum ushr 11 and 3]) // ^ (sum + key[sum >>> 11 & 3])
			z = z.sub(dz)

			sum -= GOLDEN_RATIO

			val dy = z.lanewise(LSHL, 4).lanewise(XOR, z.lanewise(LSHR, 5)).add(z) // ((z << 4) ^ (z >>> 5) + z)
				.lanewise(XOR, sum + key[sum and 3]) // ^ (sum + key[sum & 3])
			y = y.sub(dy)
		}

		out(y, z)
	}

	private val LANES = IntVector.SPECIES_PREFERRED.length()

	/** Maps every even-numbered index into a lane. */
	private val Y_INDEX_MAP = IntArray(LANES) { index -> index * 2 }

	/** Maps every odd-numbered index into a lane. */
	private val Z_INDEX_MAP = IntArray(LANES) { index -> index * 2 + 1 }
}
