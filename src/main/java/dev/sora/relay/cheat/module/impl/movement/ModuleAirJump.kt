package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleAirJump : CheatModule("AirJump","踏空", CheatCategory.MOVEMENT) {

	private var speedMultiplierValue by floatValue("SpeedMultiplier", 1f, 0.5f..3f)
	private var modeValue by listValue("Mode", ModuleAirJump.Mode.values(), Mode.TELEPORT)
	private var jumpValue by floatValue("Jump",0.42f,0.1f..1f)

	private enum class Mode(override val choiceName: String) : NamedChoice {
		MOTION("Motion"),
		TELEPORT("Teleport")
	}

	private val onTick = handleOneTime<EventTick>({ it.session.thePlayer.inputData.contains(PlayerAuthInputData.JUMP_DOWN) }) {
		val player = it.session.thePlayer
		if(modeValue == Mode.MOTION) {
			if (!player.onGround) {
				it.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
					runtimeEntityId = player.runtimeEntityId
					motion = Vector3f.from(
						player.motionX * speedMultiplierValue,
						jumpValue,
						player.motionZ * speedMultiplierValue
					)
				})
			}
		}else{
			session.thePlayer.teleport(session.thePlayer.posX,session.thePlayer.posY + (jumpValue * 2),session.thePlayer.posZ)
		}
	}
}
