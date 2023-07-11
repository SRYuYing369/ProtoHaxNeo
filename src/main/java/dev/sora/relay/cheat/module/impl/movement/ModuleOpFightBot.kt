package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.combat.ModuleAntiBot.isBot
import dev.sora.relay.cheat.module.impl.combat.ModuleTeams.isTeammate
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ModuleOpFightBot : CheatModule("OPFightBot","自动战斗", CheatCategory.MOVEMENT) {

    private var modeValue by listValue("Mode", Mode.values(), Mode.STRAFE)
	private var horizontalSpeedValue by floatValue("HorizontalSpeed", 5f, 1f..7f)
	private var verticalSpeedValue by floatValue("VerticalSpeed", 4f, 1f..7f)
	private var hopSpeedValue by floatValue("Hop Speed",0.5f, 0.1f..2f)
	private var strafeSpeedValue by intValue("StrafeSpeed", 20, 10..90)
	private var jumpValue by floatValue("Jump", 0.32f, 0.1f..0.8f)
	private var offsetValue by floatValue("Offset",0f,-0.5f..0.5f)
    private var rangeValue by floatValue("Range", 1.5f, 1.5f..4f)
	private var passiveValue by boolValue("Passive", false)

	private val handleTick = handle<EventTick> { event ->
		val session = event.session
		val target = session.theWorld.entityMap.values.filter { it is EntityPlayer &&  !it.isBot() && !it.isTeammate()}
			.minByOrNull { it.distanceSq(session.thePlayer) } ?: return@handle
		if(target.distance(session.thePlayer) < 5) {
			val direction = Math.toRadians(when(modeValue) {
				Mode.RANDOM -> Math.random() * 360
				Mode.STRAFE -> ((session.thePlayer.tickExists * strafeSpeedValue) % 360).toDouble()
				Mode.BEHIND -> target.rotationYaw + 180.0
				Mode.HOPSTRAFE -> ((session.thePlayer.tickExists * strafeSpeedValue) % 360).toDouble()
			}).toFloat()
			if(modeValue != Mode.HOPSTRAFE) {
				session.thePlayer.teleport(
					target.posX - sin(direction) * rangeValue,
					target.posY + 0.5f,
					target.posZ + cos(direction) * rangeValue
				)
			}else{
				if(session.thePlayer.onGround){
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from((-sin(direction) * hopSpeedValue), jumpValue, (cos(direction) * hopSpeedValue))
					})
				}else {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = session.thePlayer.runtimeEntityId
						motion = Vector3f.from((-sin(direction) * hopSpeedValue),session.thePlayer.motionY - (offsetValue + 0.1265f),(cos(direction) * hopSpeedValue))
					})
				}
			}
		} else if (!passiveValue) {
			val direction = atan2(target.posZ - session.thePlayer.posZ, target.posX - session.thePlayer.posX) - Math.toRadians(90.0).toFloat()
			session.thePlayer.teleport(session.thePlayer.posX - sin(direction) * horizontalSpeedValue,
				target.posY.coerceIn(session.thePlayer.posY - verticalSpeedValue, session.thePlayer.posY + verticalSpeedValue),
				session.thePlayer.posZ + cos(direction) * horizontalSpeedValue)
		}
	}

	private enum class Mode(override val choiceName: String) : NamedChoice {
        RANDOM("Random"),
        STRAFE("Strafe"),
        BEHIND("Behind"),
		HOPSTRAFE("HopStrafe")
    }
}
