package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket

class ModuleNoFire : CheatModule("NoFire","无火焰", CheatCategory.VISUAL) {

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if(packet is SetEntityDataPacket){
			if(packet.runtimeEntityId == event.session.thePlayer.runtimeEntityId){
				if(packet.metadata.flags.contains(EntityFlag.ON_FIRE)){
					packet.metadata.flags.remove(EntityFlag.ON_FIRE)
				}
			}
		}
	}
}
