package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket

class ModuleSprint:CheatModule("Sprint","疾跑",CheatCategory.MOVEMENT) {

	override fun onDisable() {
		super.onDisable()
		session.netSession.inboundPacket(PlayerActionPacket().apply {
			runtimeEntityId = session.thePlayer.runtimeEntityId
			action = PlayerActionType.STOP_SPRINT
			blockPosition = Vector3i.ZERO
			resultPosition = Vector3i.ZERO
			face = 0
		})
	}

	private val handleTick = handle<EventTick> { event ->
		if(session.thePlayer.isHorizontallyMove()){
			session.netSession.inboundPacket(PlayerActionPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				action = PlayerActionType.START_SPRINT
				blockPosition = Vector3i.ZERO
				resultPosition = Vector3i.ZERO
				face = 0
			})
		}else{
			session.netSession.inboundPacket(PlayerActionPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				action = PlayerActionType.STOP_SPRINT
				blockPosition = Vector3i.ZERO
				resultPosition = Vector3i.ZERO
				face = 0
			})
		}
	}
}
