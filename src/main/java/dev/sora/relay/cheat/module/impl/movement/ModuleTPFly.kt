package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector2d
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ModuleTPFly : CheatModule("TPFly","传送飞行", CheatCategory.MOVEMENT) {
	private var speedValue by floatValue("Speed", 1.5f, 0.1f..5f)
	private var glideSpeedValue by floatValue("GlideSpeed",-0.02f,-0.2f..0.2f)
	private var glideValue by boolValue("Glide", false)
	private var blinkValue by boolValue("Blink",true)

	private var launchY = 0f
	private var decHeight = 0f
	private val packetList = mutableListOf<BedrockPacket>()

	override fun onEnable() {
		launchY = session.thePlayer.posY + 0.2f
		decHeight = 0f
	}

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		if(blinkValue) {
			packetList.add(event.packet)
			event.cancel()
		}
	}

	override fun onDisable() {
		if(blinkValue) {
			for (packet in packetList) {
				session.netSession.outboundPacket(packet)
			}
			packetList.clear()
		}
	}

	private val handleTick = handle<EventTick> { event ->
		val session = event.session

		var moveVec2d = Vector2d.from(session.thePlayer.moveForward , -session.thePlayer.moveStrafing)
		val calcYaw = (session.thePlayer.rotationYaw) * (PI / 180)
		val c = cos(calcYaw)
		val s = sin(calcYaw)
		moveVec2d = Vector2d.from(moveVec2d.x * c - moveVec2d.y * s, moveVec2d.x * s + moveVec2d.y * c)//delay no more

		if (session.thePlayer.inputData.contains(PlayerAuthInputData.WANT_UP)) {
			launchY += 0.3f
			decHeight = 0f
		} else if (session.thePlayer.inputData.contains(PlayerAuthInputData.WANT_DOWN)) {
			launchY -= 0.3f
			decHeight = 0f
		}

		if(glideValue) {
			decHeight -= glideSpeedValue
			if (session.thePlayer.isHorizontallyMove()) {
				session.thePlayer.teleport(
					(session.thePlayer.posX - moveVec2d.y * speedValue).toFloat(),
					launchY - decHeight,
					(session.thePlayer.posZ + moveVec2d.x * speedValue).toFloat()
				)
			} else {
				session.thePlayer.teleport(session.thePlayer.posX, launchY - decHeight, session.thePlayer.posZ)
			}
		}else{
			if (session.thePlayer.isHorizontallyMove()) {
				session.thePlayer.teleport(
					(session.thePlayer.posX - moveVec2d.y * speedValue).toFloat(),
					launchY,
					(session.thePlayer.posZ + moveVec2d.x * speedValue).toFloat()
				)
			} else {
				session.thePlayer.teleport(session.thePlayer.posX, launchY, session.thePlayer.posZ)
			}
		}
	}
}
