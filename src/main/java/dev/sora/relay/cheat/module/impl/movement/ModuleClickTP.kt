package dev.sora.relay.cheat.module.impl.movement;


import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket


class ModuleClickTP: CheatModule("ClickTP","点击传送",CheatCategory.MOVEMENT) {

	private val handlePacketOutbound = handle<EventPacketOutbound>{ event->
		val packet = event.packet

		if(packet is InventoryTransactionPacket){
			if(packet.transactionType == InventoryTransactionType.ITEM_USE && packet.actionType == 0){
				val teleportPosition = Vector3f.from(
					packet.blockPosition.x.toDouble() + 0.5,
					packet.blockPosition.y.toDouble() + 2.62,
					packet.blockPosition.z.toDouble() + 0.5
				)
				event.session.thePlayer.teleport(teleportPosition)
			}
		}
	}
}
