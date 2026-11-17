package com.example.suicareader.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import java.io.ByteArrayOutputStream
import java.util.Locale

object FeliCaParser {

    private const val COMMAND_READ_WITHOUT_ENCRYPTION: Byte = 0x06
    private const val SERVICE_CODE_SUICA_HISTORY = 0x090f

    data class ParsedHistory(
        val dateString: String,
        val transactionType: Int,
        val inStationCode: String,
        val outStationCode: String,
        val balance: Int,
        val blockHex: String,
        val inStationName: String? = null,
        val outStationName: String? = null
    )

    data class SuicaData(
        val idm: ByteArray,
        val balance: Int,
        val history: List<ParsedHistory>
    )

    sealed class ReadResult {
        data class Success(val data: SuicaData) : ReadResult()
        data object TagLost : ReadResult()
        data class Failed(val error: Throwable? = null) : ReadResult()
    }

    fun readCard(tag: Tag): SuicaData? {
        return (readCardDetailed(tag) as? ReadResult.Success)?.data
    }

    fun readCardDetailed(tag: Tag): ReadResult {
        val nfcF = NfcF.get(tag) ?: return ReadResult.Failed()
        return try {
            nfcF.connect()
            runCatching { nfcF.timeout = 1200 }

            val tagIdm = tag.id

            // Batch 1: blocks 0..9 (newest)
            val history1 = readRangeWithFallback(nfcF, tagIdm, startBlock = 0, count = 10)
            if (history1.isEmpty()) {
                return ReadResult.Failed()
            }

            // Batch 2: blocks 10..19 (older), tolerant of TagLost or cards with <20 entries
            val history2 = if (history1.size == 10) {
                try {
                    readRangeWithFallback(nfcF, tagIdm, startBlock = 10, count = 10)
                } catch (_: TagLostException) {
                    // Keep the newest 10 records we already read cleanly
                    emptyList()
                }
            } else {
                emptyList()
            }

            val allHistory = history1 + history2
            val currentBalance = allHistory.firstOrNull()?.balance ?: 0
            ReadResult.Success(SuicaData(tagIdm, currentBalance, allHistory))
        } catch (e: TagLostException) {
            ReadResult.TagLost
        } catch (e: Exception) {
            e.printStackTrace()
            ReadResult.Failed(e)
        } finally {
            try {
                nfcF.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun readRangeWithFallback(
        nfcF: NfcF,
        idm: ByteArray,
        startBlock: Int,
        count: Int
    ): List<ParsedHistory> {
        val batchCmd = buildReadCommand(idm, SERVICE_CODE_SUICA_HISTORY, startBlock, count)
        val batchResp = nfcF.transceive(batchCmd)
        val parsedBatch = parseResponse(batchResp)
        if (parsedBatch != null) {
            return parsedBatch
        }

        // Fallback: card may have fewer blocks than requested; read block-by-block until end
        val singleResults = mutableListOf<ParsedHistory>()
        for (blockIdx in startBlock until (startBlock + count)) {
            val cmd = buildReadCommand(idm, SERVICE_CODE_SUICA_HISTORY, blockIdx, 1)
            val resp = nfcF.transceive(cmd)
            val parsed = parseResponse(resp) ?: break
            if (parsed.isEmpty()) break
            singleResults.addAll(parsed)
        }
        return singleResults
    }

    private fun buildReadCommand(
        idm: ByteArray,
        serviceCode: Int,
        startBlock: Int,
        numBlocks: Int
    ): ByteArray {
        val bout = ByteArrayOutputStream()
        bout.write(0)
        bout.write(COMMAND_READ_WITHOUT_ENCRYPTION.toInt())
        bout.write(idm)
        bout.write(1)
        bout.write(serviceCode and 0xFF)
        bout.write((serviceCode shr 8) and 0xFF)
        bout.write(numBlocks)

        for (i in startBlock until startBlock + numBlocks) {
            bout.write(0x80)
            bout.write(i)
        }

        val command = bout.toByteArray()
        command[0] = command.size.toByte()
        return command
    }

    private fun parseResponse(response: ByteArray): List<ParsedHistory>? {
        if (response.size < 13) return null
        if (response[1] != 0x07.toByte()) return null
        if (response[10] != 0x00.toByte() || response[11] != 0x00.toByte()) return null

        val numBlocks = response[12].toInt() and 0xFF
        val historyBlocks = mutableListOf<ParsedHistory>()

        var offset = 13
        for (i in 0 until numBlocks) {
            if (offset + 16 > response.size) break
            val block = response.copyOfRange(offset, offset + 16)
            offset += 16

            // Skip empty zero-filled blocks
            if (block.all { it == 0.toByte() }) continue

            val dateValue = ((block[4].toInt() and 0xFF) shl 8) or (block[5].toInt() and 0xFF)
            val year = ((dateValue shr 9) and 0x7F) + 2000
            val month = (dateValue shr 5) and 0x0F
            val day = dateValue and 0x1F
            val dateString = if (month in 1..12 && day in 1..31) {
                String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            } else {
                ""
            }

            val balance = (block[10].toInt() and 0xFF) or ((block[11].toInt() and 0xFF) shl 8)
            val transactionType = block[1].toInt() and 0xFF

            val inLine = block[6].toInt() and 0xFF
            val inStation = block[7].toInt() and 0xFF
            val outLine = block[8].toInt() and 0xFF
            val outStation = block[9].toInt() and 0xFF
            val inStationCode = String.format(Locale.US, "%02X-%02X", inLine, inStation)
            val outStationCode = String.format(Locale.US, "%02X-%02X", outLine, outStation)

            val regionCode = block[15].toInt() and 0xFF
            val inStationName = StationResolver.getStationName(regionCode, inLine, inStation)
            val outStationName = StationResolver.getStationName(regionCode, outLine, outStation)

            val blockHex = block.joinToString("") { "%02X".format(it) }

            historyBlocks.add(
                ParsedHistory(
                    dateString = dateString,
                    transactionType = transactionType,
                    inStationCode = inStationCode,
                    outStationCode = outStationCode,
                    balance = balance,
                    blockHex = blockHex,
                    inStationName = inStationName,
                    outStationName = outStationName
                )
            )
        }

        return historyBlocks
    }
}
