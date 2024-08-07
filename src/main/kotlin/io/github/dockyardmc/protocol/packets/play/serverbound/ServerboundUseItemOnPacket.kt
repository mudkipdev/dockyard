package io.github.dockyardmc.protocol.packets.play.serverbound

import io.github.dockyardmc.DockyardServer
import io.github.dockyardmc.annotations.ServerboundPacketInfo
import io.github.dockyardmc.annotations.WikiVGEntry
import io.github.dockyardmc.blocks.GeneralBlockPlacementRules
import io.github.dockyardmc.events.Events
import io.github.dockyardmc.events.PlayerBlockRightClickEvent
import io.github.dockyardmc.events.PlayerBlockPlaceEvent
import io.github.dockyardmc.events.PlayerRightClickWithItemEvent
import io.github.dockyardmc.extentions.broadcastMessage
import io.github.dockyardmc.extentions.readVarInt
import io.github.dockyardmc.extentions.readVarIntEnum
import io.github.dockyardmc.player.Direction
import io.github.dockyardmc.player.PlayerHand
import io.github.dockyardmc.protocol.PacketProcessor
import io.github.dockyardmc.protocol.packets.ProtocolState
import io.github.dockyardmc.protocol.packets.ServerboundPacket
import io.github.dockyardmc.registry.Blocks
import io.github.dockyardmc.registry.Items
import io.github.dockyardmc.utils.Vector3
import io.github.dockyardmc.utils.readBlockPosition
import io.github.dockyardmc.utils.toLocation
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

@WikiVGEntry("Use Item On")
@ServerboundPacketInfo(56, ProtocolState.PLAY)
class ServerboundUseItemOnPacket(
    var hand: PlayerHand,
    var pos: Vector3,
    var face: Direction,
    var cursorX: Float,
    var cursorY: Float,
    var cursorZ: Float,
    var insideBlock: Boolean,
    var sequence: Int
    ): ServerboundPacket {

    override fun handle(processor: PacketProcessor, connection: ChannelHandlerContext, size: Int, id: Int) {
        val player = processor.player
        val item = player.getHeldItem(hand)

        var cancelled = false

        val newPos = pos.copy()
        when(face) {
            Direction.UP -> newPos.y += 1
            Direction.DOWN -> newPos.y += -1
            Direction.WEST -> newPos.x += -1
            Direction.SOUTH -> newPos.z += 1
            Direction.EAST -> newPos.x += 1
            Direction.NORTH -> newPos.z += -1
        }

        val event = PlayerBlockRightClickEvent(player, item, player.world.getBlock(pos), face, pos.toLocation(player.world))
        if(event.cancelled) cancelled = true
        Events.dispatch(event)

        val rightClickEvent = PlayerRightClickWithItemEvent(player, item)
        Events.dispatch(event)
        if(rightClickEvent.cancelled) cancelled = true

        if(item.material.isBlock && item.material != Items.AIR) {
            val block = Blocks.getBlockById(item.material.blockId!!)

            if(!GeneralBlockPlacementRules.canBePlaced(pos.toLocation(player.world), newPos.toLocation(player.world), block, player)) cancelled = true

            val blockPlaceEvent = PlayerBlockPlaceEvent(player, block, newPos.toLocation(player.world))
            Events.dispatch(blockPlaceEvent)

            if(blockPlaceEvent.cancelled) cancelled = true

            if(cancelled) {
                player.world.getChunkAt(newPos.x, newPos.z)?.let { player.sendPacket(it.packet) }
                return
            }

            player.world.setBlock(blockPlaceEvent.location, blockPlaceEvent.block)
        }
    }

    companion object {
        fun read(buf: ByteBuf): ServerboundUseItemOnPacket {
            return ServerboundUseItemOnPacket(
                buf.readVarIntEnum<PlayerHand>(),
                buf.readBlockPosition(),
                buf.readVarIntEnum<Direction>(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readVarInt()
            )
        }
    }
}