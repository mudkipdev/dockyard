package io.github.dockyardmc.protocol

import LogType
import io.github.dockyardmc.TCP
import io.github.dockyardmc.events.Events
import io.github.dockyardmc.events.PacketReceivedEvent
import io.github.dockyardmc.extentions.readVarInt
import io.github.dockyardmc.player.Player
import io.github.dockyardmc.protocol.packets.ProtocolState
import io.github.dockyardmc.protocol.packets.login.LoginHandler
import io.github.dockyardmc.protocol.packets.status.StatusPacketHandler
import io.ktor.util.network.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import log

class PacketProcessor : ChannelInboundHandlerAdapter() {

    private var innerState = ProtocolState.HANDSHAKE
    var encrypted = false

    lateinit var player: Player

    var state: ProtocolState
        get() = innerState
        set(value) {
            innerState = value
            log("Protocol state changed to $value")
        }

    var statusHandler = StatusPacketHandler(this)
    var loginHandler = LoginHandler(this)

    var buffer: ByteBuf = Unpooled.buffer()

    override fun channelRead(connection: ChannelHandlerContext, msg: Any) {
        val buf = msg as ByteBuf
        buffer = buf

        try {
            while (buf.isReadable) {
                val size = buf.readVarInt()
                val id = buf.readVarInt()

                val data = buf.readBytes(size - 1)

                val packet = PacketParser.parsePacket(id, data, this)

                if(packet == null) {
                    log("Received unhandled packet with ID $id", LogType.ERROR)
                    continue
                }

                if(encrypted) {
                    PacketDecryptor.decrypt(packet, player.connectionEncryption)
                }

                log("Received ${packet::class.simpleName} (Size ${size})", LogType.NETWORK)

                Events.dispatch(PacketReceivedEvent(packet))
                packet.handle(this, connection)
            }
        } finally {
            connection.flush()
            ReferenceCountUtil.release(msg)
        }
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        log("TCP Handler Added <-> ${ctx.channel().remoteAddress().address}", TCP)
        super.handlerAdded(ctx)
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {

        log("TCP Handler Removed <-> ${ctx.channel().remoteAddress().address}", TCP)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}