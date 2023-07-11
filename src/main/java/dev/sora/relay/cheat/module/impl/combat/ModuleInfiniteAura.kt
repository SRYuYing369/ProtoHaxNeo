package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.combat.ModuleAntiBot.isBot
import dev.sora.relay.cheat.module.impl.combat.ModuleTeams.isTeammate
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import kotlin.math.pow

class ModuleInfiniteAura:CheatModule("InfiniteAura","百米大刀",CheatCategory.COMBAT) {
	private var rangeValue by floatValue("Range", 15f, 7f..80f)
	private var reversePriorityValue by boolValue("ReversePriority", false)

	val POS_Y_MAX = 2.0e9
	val MAX_RANGE = 500
	val ATTACK_COUNT = 3
	val AUTO_INTERVAL = 15
	val COLLISION_X_MIN = 1.3
	val COLLISION_Y_MIN = 1.3

	fun attack() {
		val localPlayerPos = session.thePlayer.vec3Position
		val range = rangeValue.pow(2)

		val entityList = session.theWorld.entityMap.values.filter {
			it is EntityPlayer
				&& it.distanceSq(session.thePlayer) < range
				&& !it.isBot()
				&& !it.isTeammate()
		}

		if (entityList.isEmpty()) return

		val aimTarget = selectEntity(session, entityList)

		val colision = aimTarget.metadata.size

		val pos = aimTarget.vec3Position
	}

	private fun selectEntity(session: GameSession, entityList: List<Entity>): Entity {
		return entityList.sortedBy { it.distanceSq(session.thePlayer) }.let { if (!reversePriorityValue) it.first() else it.last() }
	}
}
