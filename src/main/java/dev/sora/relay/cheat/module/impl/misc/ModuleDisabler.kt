package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class ModuleDisabler : CheatModule("Disabler","反作弊禁用器", CheatCategory.MISC) {

    private var modeValue by choiceValue("Mode", arrayOf(TheHive, Cubecraft, LifeBoat), TheHive)

	private object TheHive : Choice("TheHive") {
		private val handleTick = handle<EventTick> { event ->
			event.session.thePlayer.attackEntity(event.session.thePlayer, EntityPlayerSP.SwingMode.NONE)
		}
	}

	private object Cubecraft : Choice("Cubecraft") {

		private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet

			if (packet is PlayerAuthInputPacket) {
				packet.motion = Vector2f.from(0.01f, 0.01f)

				for (i in 0 until 9) {
					event.session.netSession.outboundPacket(packet)
				}
			} else if (packet is NetworkStackLatencyPacket) {
				event.cancel()
			}
		}

		private val handleTick = handle<EventTick> { event ->
			event.session.sendPacket(MovePlayerPacket().apply {
				val thePlayer = event.session.thePlayer
				runtimeEntityId = thePlayer.runtimeEntityId
				position = thePlayer.vec3Position
				rotation = thePlayer.vec3Rotation
				isOnGround = true
			})
		}
	}

	private object LifeBoat : Choice("LifeBoat") {

		private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet

			if (packet is PlayerAuthInputPacket) {
				event.session.sendPacket(MovePlayerPacket().apply {
					position = packet.position.add(0f, 0.1f, 0f)
					rotation = packet.rotation
					mode = MovePlayerPacket.Mode.NORMAL
					isOnGround = false
				})
			}
		}
	}
}
