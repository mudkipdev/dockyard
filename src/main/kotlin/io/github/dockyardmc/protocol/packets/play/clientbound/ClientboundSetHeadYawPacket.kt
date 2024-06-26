package io.github.dockyardmc.protocol.packets.play.clientbound

import io.github.dockyardmc.annotations.ClientboundPacketInfo
import io.github.dockyardmc.annotations.WikiVGEntry
import io.github.dockyardmc.entities.Entity
import io.github.dockyardmc.extentions.writeVarInt
import io.github.dockyardmc.protocol.packets.ClientboundPacket
import io.github.dockyardmc.protocol.packets.ProtocolState

@WikiVGEntry("Set Head Rotation")
@ClientboundPacketInfo(0x48, ProtocolState.PLAY)
class ClientboundSetHeadYawPacket(entity: Entity): ClientboundPacket() {

    init {
        data.writeVarInt(entity.entityId)
        data.writeByte((entity.location.yaw * 256 / 360).toInt())
    }

}