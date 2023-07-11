package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.constants.Attribute
import org.cloudburstmc.math.vector.Vector2d
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.AttributeData
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ModuleSpeed : CheatModule("Speed","移动加速", CheatCategory.MOVEMENT) {

	private var modeValue by choiceValue("Mode", arrayOf(Vanilla(), Hop(), Motion(), Simple(), Strafe()), "Hop")
	private var vanillaSpeedValue by floatValue("Vanilla Speed",0.2f,0.1f..5f)
	private var jumpValue by floatValue("Jump", 0.32f, 0.1f..0.8f)
	private var offsetValue by floatValue("Offset",0f,-0.5f..0.5f)
	private var speedValue by floatValue("Speed", 0.5f, 0.1f..2f)
	private var sprintToMoveValue by boolValue("SprintToMove", false)
	private var noSlowValue by boolValue("NoSlow",false)

	private val canMove: Boolean
		get() = !sprintToMoveValue || session.thePlayer.inputData.contains(PlayerAuthInputData.SPRINT_DOWN)

	private fun getMoveDirectionAngle(player: EntityPlayerSP): Float {
		return Math.toRadians(player.rotationYaw.toDouble()).toFloat()
	}

	private inner class Vanilla : Choice("Vanilla"){
		private val onTick = handle<EventTick> { event ->
			session.netSession.inboundPacket(UpdateAttributesPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				attributes.add(AttributeData(Attribute.MOVEMENT_SPEED,0f, Float.MAX_VALUE,vanillaSpeedValue,0.1f))
			})
		}
	}

	private inner class Hop : Choice("Hop") {
		private val onTick = handle<EventTick> { event ->
			val player = event.session.thePlayer

			if(!event.session.thePlayer.isHorizontallyMove() || ModuleFly().state || ModuleTPFly().state || session.thePlayer.isSwimming)
				return@handle

			if(noSlowValue) {
				val direction = Math.toRadians(player.rotationYaw.toDouble()).toFloat() + atan2(
					-player.moveStrafing,
					session.thePlayer.moveForward
				)

				if (session.thePlayer.onGround) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from(
							(-sin(direction) * speedValue),
							jumpValue,
							(cos(direction) * speedValue)
						)
					})
				} else {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from(
							(-sin(direction) * speedValue).toFloat(),
							player.motionY - (offsetValue + 0.1265f),
							(cos(direction) * speedValue).toFloat()
						)
					})
				}
			}else {
				val direction = player.newDirection

				if (session.thePlayer.onGround) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from(
							(direction.x * speedValue).toFloat(),
							jumpValue,
							(direction.y * speedValue).toFloat()
						)
					})
				} else {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from(
							(direction.x * speedValue).toFloat(),
							player.motionY - (offsetValue + 0.1265f),
							(direction.y * speedValue).toFloat()
						)
					})
				}
			}
		}
	}

	private inner class Motion : Choice("Motion") {
		private val onTick = handle<EventTick> { event ->
			val player = event.session.thePlayer

			if(!event.session.thePlayer.isHorizontallyMove() || ModuleFly().state || ModuleTPFly().state || session.thePlayer.isSwimming)
				return@handle

			val direction = event.session.thePlayer.newDirection

			event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				motion = Vector3f.from((direction.x * speedValue).toFloat(),player.motionY - (offsetValue + 0.1265f),(direction.y * speedValue).toFloat())
			})
		}
	}

	private inner class Simple : Choice("Simple") {

		private val onTick = handle<EventTick> { event ->
			val player = event.session.thePlayer

			if (canMove) {
				if (player.onGround || (player.motionY > player.prevMotionY && player.motionY < 0)) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						val angle = getMoveDirectionAngle(player)
						motion = Vector3f.from(-sin(angle) * speedValue, jumpValue, cos(angle) * speedValue)
					})
				}
			}
		}
	}

	private inner class Strafe : Choice("Strafe") {

		private val onTick = handle<EventTick> { event ->
			val player = event.session.thePlayer

			if (canMove) {
				val angle = getMoveDirectionAngle(player)
				val motionX = -sin(angle) * speedValue
				val motionZ = cos(angle) * speedValue

				if (player.onGround || (player.motionY > player.prevMotionY && player.motionY < 0 && player.posY % 0.125f == 0f)) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, jumpValue, motionZ)
					})
				} else {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, (player.motionY - 0.1f) * 0.95f, motionZ)
					})
				}
			}
		}
	}
}
