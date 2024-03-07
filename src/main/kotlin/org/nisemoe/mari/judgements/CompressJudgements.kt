package org.nisemoe.mari.judgements

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.Decoder
import com.aayushatharva.brotli4j.encoder.Encoder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.round

/**
 * Compresses and decompresses score judgements with lossless accuracy.
 */
class CompressJudgements {

    companion object {

        init {
            Brotli4jLoader.ensureAvailability()
        }

        private val brotliParameters: Encoder.Parameters = Encoder.Parameters()
            .setQuality(11)

        /**
         * Writes a variable-length quantity to the buffer.
         * See: https://en.wikipedia.org/wiki/Variable-length_quantity
         */
        private fun ByteBuffer.putVLQ(value: Int) {
            var currentValue = value
            do {
                var temp = (currentValue and 0x7F)
                currentValue = currentValue ushr 7
                if (currentValue != 0) {
                    temp = temp or 0x80
                }
                this.put(temp.toByte())
            } while (currentValue != 0)
        }

        /**
         * Reads a variable-length quantity from the buffer.
         * See: https://en.wikipedia.org/wiki/Variable-length_quantity
         */
        private fun ByteBuffer.getVLQ(): Int {
            var result = 0
            var shift = 0
            var b: Byte
            do {
                b = this.get()
                result = result or ((b.toInt() and 0x7F) shl shift)
                shift += 7
            } while (b.toInt() and 0x80 != 0)
            return result
        }

        fun compress(judgements: List<Judgement>): ByteArray {
            val byteStream = ByteArrayOutputStream()
            var lastTimestamp = 0

            judgements.forEach { judgement ->
                byteStream.use { stream ->
                    /**
                     * We allocate an arbitrary amount of buffer which *hopefully* is enough.
                     */
                    ByteBuffer.allocate(4096).let { buffer ->
                        buffer.putVLQ((judgement.time - lastTimestamp))
                        buffer.putVLQ(round(judgement.x * 100).toInt())
                        buffer.putVLQ(round(judgement.y * 100).toInt())
                        buffer.put(judgement.type.ordinal.toByte())
                        buffer.putVLQ((judgement.distanceToCenter * 100).toInt())
                        buffer.putVLQ((judgement.distanceToEdge * 100).toInt())
                        buffer.putVLQ(judgement.error.toInt())

                        lastTimestamp = judgement.time
                        stream.write(buffer.array(), 0, buffer.position())
                    }
                }
            }

            return Encoder.compress(byteStream.toByteArray(), brotliParameters)
        }

        fun decompress(compressedData: ByteArray): List<Judgement> {
            val data = Decoder.decompress(compressedData).decompressedData ?: return emptyList()

            val buffer = ByteBuffer.wrap(data)
            val judgements = mutableListOf<Judgement>()
            var lastTime = 0

            while (buffer.hasRemaining()) {
                val deltaTime = buffer.getVLQ()
                lastTime += deltaTime

                judgements.add(
                    Judgement(
                        time = lastTime,
                        x = buffer.getVLQ() / 100.0,
                        y = buffer.getVLQ() / 100.0,
                        type = Judgement.Type.entries[buffer.get().toInt()],
                        distanceToCenter = buffer.getVLQ() / 100.0,
                        distanceToEdge = buffer.getVLQ() / 100.0,
                        error = buffer.getVLQ().toDouble()
                    ))
            }

            return judgements
        }

    }

}