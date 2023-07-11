package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketOutbound
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.LevelEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket
import kotlin.random.Random

class ModuleHitEffect : CheatModule("HitEffect","打击效果", CheatCategory.VISUAL) {

	private var effectValue by listValue("Effect", Effect.values(), Effect.CRITICAL)

	private enum class Effect(override val choiceName: String) : NamedChoice {
		CRITICAL("Critical"),
		LIGHTNING("Lightning"),
		BLOOD("Blood")
	}

	private val onPacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if(packet is InventoryTransactionPacket) {
			if (packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY) { //Attack
				if (packet.runtimeEntityId != event.session.thePlayer.runtimeEntityId) {
					if(effectValue == Effect.CRITICAL) {
						event.session.netSession.inboundPacket(AnimatePacket().apply {
							runtimeEntityId = packet.runtimeEntityId
							action = AnimatePacket.Action.CRITICAL_HIT
						})
					}else if(effectValue == Effect.LIGHTNING){
						val entity = session.theWorld.entityMap[packet.runtimeEntityId] ?: return@handle

						val entityId = Random.nextLong()

						event.session.netSession.inboundPacket(AddEntityPacket().apply {
							uniqueEntityId = entityId
							runtimeEntityId = entityId
							identifier = "minecraft:lightning_bolt"
							entityType = 0
							position = Vector3f.from(
								entity.vec3Position.x,
								entity.vec3Position.y - 1.62f,
								entity.vec3Position.z
							)
							motion = Vector3f.ZERO
							rotation = Vector2f.ZERO
							bodyRotation = 0f
						})
					}else if(effectValue == Effect.BLOOD){
						val entity = session.theWorld.entityMap[packet.runtimeEntityId] ?: return@handle

						event.session.netSession.inboundPacket(LevelEventPacket().apply {
							type = LevelEvent.PARTICLE_DESTROY_BLOCK
							position = Vector3f.from(
								entity.vec3Position.x,
								entity.vec3Position.y - 1.62f,
								entity.vec3Position.z
							)
							data = 5169
						})
					}
				}
			}
		}
	}
}
