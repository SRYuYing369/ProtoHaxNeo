package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

class ModuleTextSpoof:CheatModule("TextSpoof","文字伪造",CheatCategory.VISUAL) {
	private var oldTextValue by stringValue("OldText","steve")
	private var newTextValue by stringValue("NewText","ProtoHax User")

	private var onPacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet
		if(packet is TextPacket){
			if(packet.message.contains(oldTextValue)){
				packet.message = packet.message.replace(oldTextValue,newTextValue)
			}
		}
	}
}
