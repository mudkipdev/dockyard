package io.github.dockyardmc.events

import io.github.dockyardmc.annotations.EventDocumentation
import io.github.dockyardmc.player.Player

@EventDocumentation("when player opens vehicle's inventory while riding it", true)
//TODO: Make cancellable
class PlayerVehicleInventoryOpenEvent(player: Player): Event