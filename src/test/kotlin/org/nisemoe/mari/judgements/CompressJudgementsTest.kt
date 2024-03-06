package org.nisemoe.mari.judgements

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompressJudgementsTest {

    @Test
    fun testCompress() {
        val judgements = listOf(
            Judgement(time = 1.0, x = 1.0, y = 1.0, type = Judgement.Type.THREE_HUNDRED, distanceToCenter = 1.0, distanceToEdge = 1.0, error = 1.0),
            Judgement(time = 2.0, x = 2.0, y = 2.0, type = Judgement.Type.THREE_HUNDRED, distanceToCenter = 2.0, distanceToEdge = 2.0, error = 2.0)
        )
        val compressedData = CompressJudgements.compress(judgements)
        assertTrue(compressedData.isNotEmpty())
    }

    @Test
    fun testCompressAndDecompress() {
        val originalJudgements = listOf(
            Judgement(time = 1.123456789123456, x = 1.0, y = 1.0, type = Judgement.Type.THREE_HUNDRED, distanceToCenter = 1.0, distanceToEdge = 1.0, error = 1.0),
            Judgement(time = 2.123456789123456, x = 2.0, y = 2.0, type = Judgement.Type.THREE_HUNDRED, distanceToCenter = 2.0, distanceToEdge = 2.0, error = 2.0)
        )
        val compressedData = CompressJudgements.compress(originalJudgements)
        val decompressedJudgements = CompressJudgements.decompress(compressedData)
        assertEquals(originalJudgements, decompressedJudgements)
    }

}