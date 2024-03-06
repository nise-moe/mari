package org.nisemoe.mari.judgements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a judgement on a hit object.
 * The structure *might* seem arbitrary but it's more or less what Circleguard provides.
 */
@Serializable
data class Judgement(
    val time: Double,
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

