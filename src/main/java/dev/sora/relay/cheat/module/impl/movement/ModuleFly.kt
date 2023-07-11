package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.data.Effect
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ModuleFly : CheatModule("Fly","飞行", CheatCategory.MOVEMENT) {

    private var modeValue by choiceValue("Mode", arrayOf(Vanilla("Vanilla"), Motion(), Jetpack(), Glide(), YPort()), "Motion")
    private var speedValue by floatValue("Speed", 0.6f, 0.1f..5f)
	private var verticalSpeedValue by floatValue("VerticalSpeed",0.2f,0.1f..1f)
	private var addValue by floatValue("Add", 0.01f, 0f..0.2f)
	private var pressJumpValue by boolValue("PressJump", true)

    private var launchY = 0f
	private val canFly: Boolean
		get() = !pressJumpValue || session.thePlayer.inputData.contains(PlayerAuthInputData.JUMP_DOWN)

    private val abilityPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.values())
            abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    override fun onEnable() {
        launchY = session.thePlayer.posY
    }

	private open inner class Vanilla(choiceName: String) : Choice(choiceName) {

		private val handleTick = handle<EventTick> { event ->
			if (event.session.thePlayer.tickExists % 10 == 0L) {
				event.session.netSession.inboundPacket(abilityPacket.apply {
					uniqueEntityId = event.session.thePlayer.uniqueEntityId
				})
			}
		}

		private val handlePacketInbound = handle<EventPacketInbound> { event ->
			val packet = event.packet
			if (packet is UpdateAbilitiesPacket) {
				event.cancel()
				event.session.netSession.inboundPacket(abilityPacket.apply {
					uniqueEntityId = event.session.thePlayer.uniqueEntityId
				})
			} else if (packet is StartGamePacket) {
				event.session.netSession.inboundPacket(abilityPacket.apply {
					uniqueEntityId = event.session.thePlayer.uniqueEntityId
				})
			}
		}

		private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet
			if (packet is RequestAbilityPacket && packet.ability == Ability.FLYING) {
				event.cancel()
			}
		}
	}

	private inner class Motion : Vanilla("Motion") {
		private val handleTick = handle<EventTick> { event ->
			val session = event.session

			var motionX = 0f
			var motionY = addValue
			var motionZ = 0f

			if (session.thePlayer.inputData.contains(PlayerAuthInputData.WANT_UP)) {
				motionY = verticalSpeedValue
			} else if (session.thePlayer.inputData.contains(PlayerAuthInputData.WANT_DOWN)) {
				motionY = -verticalSpeedValue
			}

			if(session.thePlayer.isHorizontallyMove()) {
				val direction = session.thePlayer.newDirection
				motionX = (direction.x * speedValue).toFloat()
				motionZ = (direction.y * speedValue).toFloat()
			}

			event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				motion = Vector3f.from(motionX, motionY,motionZ)
			})
		}
	}

	private inner class Jetpack : Choice("Jetpack") {

		private val handleTick = handle<EventTick> { event ->
			val session = event.session

			if (!canFly) {
				return@handle
			}

			session.netSession.inboundPacket(SetEntityMotionPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId

				val calcYaw: Double = (session.thePlayer.rotationYawHead + 90) * (PI / 180)
				val calcPitch: Double = (session.thePlayer.rotationPitch) * -(PI / 180)

				motion = Vector3f.from(
					cos(calcYaw) * cos(calcPitch) * speedValue,
					sin(calcPitch) * speedValue,
					sin(calcYaw) * cos(calcPitch) * speedValue
				)
			})
		}
	}

	private inner class Glide : Choice("Glide") {

		override fun onDisable() {
			if (session.netSessionInitialized) {
				session.netSession.inboundPacket(MobEffectPacket().apply {
					event = MobEffectPacket.Event.REMOVE
					runtimeEntityId = session.thePlayer.runtimeEntityId
					effectId = Effect.SLOW_FALLING
				})
			}
		}

		private val handleTick = handle<EventTick> { event ->
			val session = event.session
			if (session.thePlayer.tickExists % 20 != 0L) return@handle
			session.netSession.inboundPacket(MobEffectPacket().apply {
				runtimeEntityId = session.thePlayer.runtimeEntityId
				setEvent(MobEffectPacket.Event.ADD)
				effectId = Effect.SLOW_FALLING
				amplifier = 0
				isParticles = false
				duration = 360000
			})
		}
	}


	private inner class YPort : Choice("YPort") {

		private var flag = true

		override fun onDisable() {
			flag = true
		}

		private val onTick = handle<EventTick> { event ->
			val player = event.session.thePlayer

			if (canFly) {
				val angle = Math.toRadians(player.rotationYaw.toDouble()).toFloat()

				event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
					runtimeEntityId = player.runtimeEntityId
					motion = Vector3f.from(-sin(angle) * speedValue, if (flag) 0.42f else -0.42f, cos(angle) * speedValue)
				})
				flag = !flag
			}
		}
	}
}
