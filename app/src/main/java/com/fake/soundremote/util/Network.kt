package com.fake.soundremote.util

import androidx.annotation.IntDef
import com.fake.soundremote.network.ConnectData
import com.fake.soundremote.network.DisconnectData
import com.fake.soundremote.network.KeepAliveData
import com.fake.soundremote.network.KeystrokeData
import com.fake.soundremote.network.PacketData
import com.fake.soundremote.network.PacketHeader
import com.fake.soundremote.network.SetFormatData
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
        CONNECT(0x03),
        DISCONNECT(0x04),
        SET_FORMAT(0x05),
        KEYSTROKE(0x10),
        AUDIO_DATA_PCM(0x20),
        AUDIO_DATA_OPUS(0x21),
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        COMPRESSION_NONE,
        COMPRESSION_64,
        COMPRESSION_128,
        COMPRESSION_192,
        COMPRESSION_256,
        COMPRESSION_320
    )
    annotation class Compression

    const val COMPRESSION_NONE = 0
    const val COMPRESSION_64 = 1
    const val COMPRESSION_128 = 2
    const val COMPRESSION_192 = 3
    const val COMPRESSION_256 = 4
    const val COMPRESSION_320 = 5

    private val keepAlivePacket: ByteBuffer
    private val disconnectPacket: ByteBuffer
    private val BYTE_ORDER: ByteOrder = ByteOrder.LITTLE_ENDIAN

    init {
        keepAlivePacket = createPacket(PacketType.CLIENT_KEEP_ALIVE, KeepAliveData())
        disconnectPacket = createPacket(PacketType.DISCONNECT, DisconnectData())
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

    fun getConnectPacket(@Compression compression: Int): ByteBuffer {
        val data = ConnectData(compression)
        return createPacket(PacketType.CONNECT, data)
    }

    fun getDisconnectPacket(): ByteBuffer {
        disconnectPacket.rewind()
        return disconnectPacket
    }

    fun getSetFormatPacket(@Compression compression: Int): ByteBuffer {
        val data = SetFormatData(compression)
        return createPacket(PacketType.SET_FORMAT, data)
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
