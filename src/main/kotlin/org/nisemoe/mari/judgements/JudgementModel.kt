package org.nisemoe.mari.judgements

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

/**
 * Backwards compatibility with time values that were persisted as doubles.
 */
object TimeSerializer : KSerializer<Int> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Time", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val doubleValue = decoder.decodeDouble()
        return doubleValue.roundToInt()
    }

}

/**
 * Represents a judgement on a hit object.
 * The structure *might* seem arbitrary but it's more or less what Circleguard provides.
 */
@Serializable
data class Judgement(
    @Serializable(with = TimeSerializer::class)
    val time: Int,

    val x: Double,
    val y: Double,
    val type: Type,

    @SerialName("distance_center")
    val distanceToCenter: Double,

    @SerialName("distance_edge")
    val distanceToEdge: Double,


    val error: Double
) {

    @Serializable
    enum class Type {

        @SerialName("Hit300")
        THREE_HUNDRED,

        @SerialName("Hit100")
        ONE_HUNDRED,

        @SerialName("Hit50")
        FIFTY,

        @SerialName("Miss")
        MISS

    }

}

