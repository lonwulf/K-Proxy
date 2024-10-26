package com.lonwulf.iproxyclone

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class TunnelManager {
    private val packetSize = 32767
    private val buffer = ByteBuffer.allocate(packetSize)


    fun startTunnelling(tunnel: DatagramChannel, vpnInterface: ParcelFileDescriptor?) {
        while (true) {
            val length = FileInputStream(vpnInterface?.fileDescriptor).read(buffer.array())
            if (length > 0) {
                buffer.limit(length)
                tunnel.write(buffer)
                buffer.clear()
            }

            buffer.limit(packetSize)
            val bytesRead = tunnel.read(buffer)
            if (bytesRead > 0) {
                buffer.flip()
                FileOutputStream(vpnInterface?.fileDescriptor).write(buffer.array(), 0, bytesRead)
                buffer.clear()
            }
        }
    }
}