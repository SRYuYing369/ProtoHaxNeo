package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket

class ModuleAntiKick:CheatModule("AntiKick","防踢出",CheatCategory.MISC) {
	private var disconnectPacketValue by boolValue("DisconnectPacket",true)
	private var transferPacketValue by boolValue("TransferPacket",true)

	private var onPacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if(packet is DisconnectPacket && disconnectPacketValue){
			event.cancel()
			session.chat("disconnect: ${packet.kickMessage}")
		}

		if(packet is TransferPacket && transferPacketValue){
			event.cancel()
			session.chat("transfer cancel!")
		}
	}
}
