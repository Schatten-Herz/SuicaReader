package com.example.suicareader.nfc

import android.nfc.Tag
import android.nfc.tech.NfcF
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    fun readCard(tag: Tag): SuicaData? {
        val nfcF = NfcF.get(tag) ?: return null
        return try {
            nfcF.connect()
            
            val tagIdm = tag.id // 对于 FeliCa，NfcF 的 tag ID 即为 IDm (8 bytes)
            
            // 物理 Suica 卡片最多只保存最新的 20 条记录。
            // 由于 FeliCa 单次通讯有长度限制（最多约 15 块），我们需要分两次读取：0-9 块，10-19 块。
            val command1 = buildReadCommand(tagIdm, SERVICE_CODE_SUICA_HISTORY, 0, 10)
            val response1 = nfcF.transceive(command1)
            val history1 = parseResponse(tagIdm, response1) ?: emptyList()

            val command2 = buildReadCommand(tagIdm, SERVICE_CODE_SUICA_HISTORY, 10, 10)
            val response2 = nfcF.transceive(command2)
            val history2 = parseResponse(tagIdm, response2) ?: emptyList()
            
            val allHistory = history1 + history2
            
            // 当前余额取最新的一条（索引0）
            val currentBalance = allHistory.firstOrNull()?.balance ?: 0

            SuicaData(tagIdm, currentBalance, allHistory)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { nfcF.close() } catch (e: Exception) {}
        }
    }

    private fun buildReadCommand(idm: ByteArray, serviceCode: Int, startBlock: Int, numBlocks: Int): ByteArray {
        val bout = ByteArrayOutputStream()
        bout.write(0) // 占位符，最后写入长度
        bout.write(COMMAND_READ_WITHOUT_ENCRYPTION.toInt())
        bout.write(idm)
        bout.write(1) // Number of Services
        // Service Code 是 Little Endian 格式发送
        bout.write(serviceCode and 0xFF)
        bout.write((serviceCode shr 8) and 0xFF)
        bout.write(numBlocks) // Number of Blocks

        // Block List (对于 <= 255 的 block 数，长度为 2 字节)
        for (i in startBlock until startBlock + numBlocks) {
            bout.write(0x80) // Block Element: 0x80 表示 2-byte block list element
            bout.write(i)    // Block Number
        }

        val command = bout.toByteArray()
        command[0] = command.size.toByte() // 设置第一个字节为总长度
        return command
    }

    private fun parseResponse(idm: ByteArray, response: ByteArray): List<ParsedHistory>? {
        // Response 格式: Length (1) + Response Code (1) + IDm (8) + Status Flag 1 (1) + Status Flag 2 (1) + Num of Blocks (1) + Block Data (16 * N)
        if (response.size < 13) return null
        if (response[1] != 0x07.toByte()) return null // 0x07 是 Read Without Encryption 的正确 Response Code
        if (response[10] != 0x00.toByte() || response[11] != 0x00.toByte()) return null // Status Flags 检查

        val numBlocks = response[12].toInt() and 0xFF
        val historyBlocks = mutableListOf<ParsedHistory>()

        var offset = 13
        for (i in 0 until numBlocks) {
            if (offset + 16 > response.size) break
            val block = response.copyOfRange(offset, offset + 16)
            
            // 解析日期 (Byte 4-5)
            val dateValue = ((block[4].toInt() and 0xFF) shl 8) or (block[5].toInt() and 0xFF)
            val year = ((dateValue shr 9) and 0x7F) + 2000
            val month = (dateValue shr 5) and 0x0F
            val day = dateValue and 0x1F
            val dateString = String.format("%04d-%02d-%02d", year, month, day)

            // 解析余额 (Byte 10-11, Little Endian)
            val balance = (block[10].toInt() and 0xFF) or ((block[11].toInt() and 0xFF) shl 8)

            // 交易类型 (Byte 1)
            val transactionType = block[1].toInt() and 0xFF

            // 站点代码 (Byte 6-9)
            val inLine = block[6].toInt() and 0xFF
            val inStation = block[7].toInt() and 0xFF
            val outLine = block[8].toInt() and 0xFF
            val outStation = block[9].toInt() and 0xFF
            val inStationCode = String.format("%02X-%02X", inLine, inStation)
            val outStationCode = String.format("%02X-%02X", outLine, outStation)
            
            // Region is usually at block[15]
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
            offset += 16
        }

        return historyBlocks
    }
}
