package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventPacketOutbound
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket

class ModuleGodMode:CheatModule("GodMode","上帝模式", CheatCategory.COMBAT) {
	private var modeValue by listValue("Mode",ModuleGodMode.Mode.values(),Mode.POS)

	private enum class Mode(override val choiceName: String) : NamedChoice {
		POS("AddPos"),
		ATTACK("Attack")
	}

	private var onPacketOutbound = handle<EventPacketOutbound> { event ->
		if(modeValue.choiceName == "AddPos"){
			event.session.netSession.outboundPacket(MovePlayerPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				position = session.thePlayer.vec3Position.add(1000.0,1000.0,1000.0)
				rotation = session.thePlayer.vec3Rotation
				mode = MovePlayerPacket.Mode.NORMAL
				isOnGround = false
				tick = event.session.thePlayer.tickExists
			})
		}else if(modeValue.choiceName == "Attack"){
			session.thePlayer.attackEntity(session.thePlayer, EntityPlayerSP.SwingMode.NONE,false,false)
		}
	}
}
