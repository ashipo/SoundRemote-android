package com.fake.soundremote.network

import com.fake.soundremote.network.PacketHeader.Companion.SIZE
import com.fake.soundremote.util.Net.getUByte
import java.nio.ByteBuffer

/**
 * Custom data for ACK response on a Connect request.
 */
data class AckConnectData(val protocol: UByte) {
    companion object {
        /*
        unsigned 8bit   protocol version
        */
        private const val SIZE = 1

        /**
         * Read ACK packet custom data from the source [ByteBuffer].
         * Increments [source] position by [SIZE] on successful read.
         * @param source [ByteBuffer] to read from
         * @return [AckData] instance or null if there is not enough data remaining in [source].
         */
        fun read(source: ByteBuffer): AckConnectData? {
            if (source.remaining() < SIZE) return null
            val protocol = source.getUByte()
            return AckConnectData(protocol)
        }
    }
}
