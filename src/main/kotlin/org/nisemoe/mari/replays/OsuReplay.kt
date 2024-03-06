package org.nisemoe.mari.replays

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Decodes an osu! replay file from a byte array.
 */
class OsuReplay(fileContent: ByteArray) {

    companion object {

        /**
         * We restrict the maximum replay file size to roughly 4MB.
         */
        private val EXPECTED_FILE_SIZE = 0 .. 4194304

        /**
         * We restrict the maximum string length to roughly 500KB.
         */
        private const val MAX_STRING_LENGTH = 512000

        private val EXPECTED_STRING_LENGTH = 0 .. MAX_STRING_LENGTH

        private val EXPECTED_INT_RANGE = Int.MIN_VALUE..Int.MAX_VALUE

        private val EXPECTED_LONG_RANGE = Long.MIN_VALUE..Long.MAX_VALUE

        private val EXPECTED_DOUBLE_RANGE = Double.MIN_VALUE..Double.MAX_VALUE

    }

    private val dis = DataInputStream(fileContent.inputStream())

    var gameMode: Int = 0
    var gameVersion: Int = 0
    var beatmapHash: String? = null
    var playerName: String? = null
    var replayHash: String? = null
    var numberOf300s: Short = 0
    var numberOf100s: Short = 0
    var numberOf50s: Short = 0
    var numberOfGekis: Short = 0
    var numberOfKatus: Short = 0
    var numberOfMisses: Short = 0
    var totalScore: Int = 0
    var greatestCombo: Short = 0
    var perfectCombo: Boolean = false
    var modsUsed: Int = 0
    var lifeBarGraph: String? = null
    var timestamp: Long = 0
    var replayLength: Int = 0
    var replayData: String? = null
    var onlineScoreID: Long? = null
    var additionalModInfo: Double = 0.0

    init {
        if (fileContent.size !in EXPECTED_FILE_SIZE) {
            throw SecurityException("File size out of expected bounds")
        }

        decode()
    }

    private fun decode() {
        try {
            gameMode = dis.readByte().toInt()

            gameVersion = readIntLittleEndian()
            beatmapHash = dis.readCompressedReplayData()
            playerName = dis.readCompressedReplayData()
            replayHash = dis.readCompressedReplayData()
            numberOf300s = readShortLittleEndian()
            numberOf100s = readShortLittleEndian()
            numberOf50s = readShortLittleEndian()
            numberOfGekis = readShortLittleEndian()
            numberOfKatus = readShortLittleEndian()
            numberOfMisses = readShortLittleEndian()
            totalScore = readIntLittleEndian()
            greatestCombo = readShortLittleEndian()
            perfectCombo = dis.readByte() != 0.toByte()
            modsUsed = readIntLittleEndian()
            lifeBarGraph = dis.readCompressedReplayData()
            timestamp = readLongLittleEndian()
            replayLength = readIntLittleEndian()
            replayData = dis.readCompressedReplayData(replayLength)
            onlineScoreID = readLongLittleEndian()
            if ((modsUsed and (1 shl 24)) != 0) {
                additionalModInfo = readDoubleLittleEndian()
            }
        } catch (e: Exception) {
            println("Failed to decode .osr file content: ${e.message}")
        }
    }

    private fun DataInputStream.readCompressedReplayData(): String? {
        return when (readByte()) {
            0x0b.toByte() -> {
                val length = readULEB128()
                if (length !in EXPECTED_STRING_LENGTH) {
                    throw SecurityException("String length out of expected bounds")
                }
                ByteArray(length.toInt()).also { readFully(it) }.toString(StandardCharsets.UTF_8)
            }
            else -> null
        }
    }

    private fun DataInputStream.readCompressedReplayData(length: Int): String {
        val compressedData = ByteArray(length)
        readFully(compressedData)

        val compressedOutputStream = ByteArrayOutputStream().use { outputStream ->
            LZMACompressorInputStream(compressedData.inputStream()).use { decompressedStream ->
                LZMACompressorOutputStream(outputStream).use { lzmaCompressorOutputStream ->
                    decompressedStream.copyTo(lzmaCompressorOutputStream)
                }
            }
            outputStream
        }

        return Base64.getEncoder().encodeToString(compressedOutputStream.toByteArray())
    }

    private fun DataInputStream.readULEB128(): Long {
        var result = 0L
        var shift = 0
        var size = 0

        do {
            if (size == 10) { // Prevent reading more than 10 bytes, the maximum needed for a 64-bit number
                throw SecurityException("Invalid LEB128 sequence.")
            }

            val byte = readByte()
            size++

            // Check for overflow: If we're on the last byte (10th), it should not have more than 1 bit before the continuation bit
            if (size == 10 && byte.toInt() and 0x7F > 1) {
                throw SecurityException("LEB128 sequence overflow.")
            }

            val value = (byte.toInt() and 0x7F)
            if (shift >= 63 && value > 0) { // prevent shifting into oblivion
                throw SecurityException("LEB128 sequence overflow.")
            }

            result = result or (value.toLong() shl shift)
            shift += 7
        } while (byte.toInt() and 0x80 > 0)

        return result
    }

    private fun readShortLittleEndian(): Short {
        if (dis.available() < Short.SIZE_BYTES) {
            throw SecurityException("Insufficient data available to read short")
        }
        val bytes = ByteArray(Short.SIZE_BYTES)
        dis.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short
    }

    private fun readIntLittleEndian(): Int {
        if (dis.available() < Int.SIZE_BYTES) {
            throw SecurityException("Insufficient data available to read int")
        }
        val bytes = ByteArray(Int.SIZE_BYTES)
        dis.readFully(bytes)
        val value = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
        if (value !in EXPECTED_INT_RANGE) {
            throw SecurityException("Decoded integer value out of expected bounds")
        }
        return value
    }

    private fun readLongLittleEndian(): Long {
        if (dis.available() < Long.SIZE_BYTES) {
            throw SecurityException("Insufficient data available to read long")
        }
        val bytes = ByteArray(Long.SIZE_BYTES)
        dis.readFully(bytes)
        val value = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long
        if (value !in EXPECTED_LONG_RANGE) {
            throw SecurityException("Decoded long value out of expected bounds")
        }
        return value
    }

    private fun readDoubleLittleEndian(): Double {
        if (dis.available() < Double.SIZE_BYTES) {
            throw SecurityException("Insufficient data available to read double")
        }
        val bytes = ByteArray(Double.SIZE_BYTES)
        dis.readFully(bytes)
        val value = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
        if (value !in EXPECTED_DOUBLE_RANGE) {
            throw SecurityException("Decoded double value out of expected bounds")
        }
        return value
    }

}