package com.fake.soundremote.util

import com.fake.soundremote.network.KeepAliveData
import com.fake.soundremote.network.KeystrokeData
import com.fake.soundremote.network.PacketData
import com.fake.soundremote.network.PacketHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED
}

object Net {
    const val PROTOCOL_SIGNATURE: Char = 0xA571.toChar()
    const val RECEIVE_BUFFER_CAPACITY = 2048
    const val SERVER_TIMEOUT_SECONDS = 5

    enum class PacketType(val value: Int) {
        CLIENT_KEEP_ALIVE(0x01),
        SERVER_KEEP_ALIVE(0x02),
        KEYSTROKE(0x10),
        AUDIO_DATA_OPUS(0x20),
    }

    private val keepAlivePacket: ByteBuffer
    private val BYTE_ORDER: ByteOrder = ByteOrder.LITTLE_ENDIAN

    init {
        keepAlivePacket = createPacket(PacketType.CLIENT_KEEP_ALIVE, KeepAliveData())
    }

    fun readUByte(data: ByteBuffer): Int {
        return data.get().toInt() and 0xFF
    }

    fun createPacketBuffer(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(BYTE_ORDER)
    }

    private fun createPacket(type: PacketType, data: PacketData): ByteBuffer {
        val packetSize = PacketHeader.SIZE + data.size
        val packet = createPacketBuffer(packetSize)
        val header = PacketHeader(type, packetSize)
        header.write(packet)
        data.writeToBuffer(packet)
        packet.rewind()
        return packet
    }

    fun getKeystrokePacket(keyCode: Int, mods: Int): ByteBuffer {
        val data = KeystrokeData(keyCode, mods)
        return createPacket(PacketType.KEYSTROKE, data)
    }

    fun getKeepAlivePacket(): ByteBuffer {
        keepAlivePacket.rewind()
        return keepAlivePacket
    }
}
