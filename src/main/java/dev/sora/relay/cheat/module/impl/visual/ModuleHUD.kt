package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.utils.timing.TheTimer
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.math.sqrt

class ModuleHUD : CheatModule("HUD","抬头显示",CheatCategory.VISUAL) {
	private var delayValue by intValue("Delay", 250, 0..500)
	private var debugModeValue by boolValue("DebugMode",false)
	private var targetValue by boolValue("Target", true)
	private var speedValue by boolValue("Speed",true)
	private var apsValue by boolValue("APS",true)

	private val displayTimer = TheTimer()
	private val apsTimer = TheTimer()
	private var clickCount = 0

	private var bps = 0.0
	private var aps  = 0

	override fun onEnable() {
		super.onEnable()
		apsTimer.reset()
		bps = 0.0
		aps = 0
	}

	private val handleTick = handle<EventTick> { event ->
		val session = event.session
		if(speedValue){
			val thePlayer = event.session.thePlayer
			bps = (sqrt(thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ) * 20).toDouble()
		}

		if(apsValue){
			if(apsTimer.hasTimePassed(1000)){
				aps = clickCount
				clickCount = 0
				apsTimer.reset()
			}
		}
	}

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet
		if (apsValue) {
			if (packet is InventoryTransactionPacket) {
				if (packet.transactionType == InventoryTransactionType.ITEM_USE || packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY) {
					++clickCount
				}
			}
		}
	}

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if(displayTimer.hasTimePassed(delayValue)) {
			if (!debugModeValue) {
				if (session.targetName.contains("\n")) session.targetName = session.targetName.replace("\n", " ")
				if (session.targetName.contains("§l")) session.targetName = session.targetName.replace("§l", "")
				val targetText = if(session.chineseMode)" | §e正在攻击§r: ${session.targetName}§r" else " | §eAttacking§r: ${session.targetName}§r"
				val bpsText = if(session.chineseMode)" | §e速度§r: ${String.format("%.2f", bps)} 格每秒" else " | §eSpeed§r: ${String.format("%.2f", bps)} bps"
				val cpsText = " | §eAPS§r: $aps"

				event.session.netSession.inboundPacket(TextPacket().apply {
					type = TextPacket.Type.TIP
					isNeedsTranslation = false
					sourceName = null
					xuid = session.thePlayer.xuid
					message = ""
					message += "§bProtoHaxNeo§r"
					if (session.targetName != "None" && targetValue) {
						message += targetText
						message += ""
					}
					if (speedValue) {
						message += bpsText
						message += ""
					}
					if (session.targetName != "None") {
						message += cpsText
					}
					message += "§我"
				})
			}else{
				val onGroundText = " | §eonGround§r: ${session.thePlayer.onGround}§r"
				val movingText = " | §eMoving§r: ${session.thePlayer.isHorizontallyMove()}"
				val directionText = " | §eDirection§r: ${session.thePlayer.direction}"
				val moveForwardText = " | §eMoveForward§r: ${session.thePlayer.moveForward}§r"
				val moveStrafingText = " | §eMoveStrafing§r: ${session.thePlayer.moveStrafing}§r"

				event.session.netSession.inboundPacket(TextPacket().apply {
					type = TextPacket.Type.TIP
					isNeedsTranslation = false
					sourceName = null
					xuid = session.thePlayer.xuid

					message = ""
					message += "§bProtoHax§r"

					message += moveForwardText
					message += ""

					message += moveStrafingText
					message += ""

					message += directionText

					message += "§我"
				})
			}
			displayTimer.reset()
		}
	}
}
