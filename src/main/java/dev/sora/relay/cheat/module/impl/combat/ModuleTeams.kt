package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.combat.ModuleAntiBot.isBot
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import java.util.concurrent.LinkedBlockingQueue

object ModuleTeams : CheatModule("Teams","智能队友", CheatCategory.COMBAT,false,false) {

	private var modeValue by listValue("Mode", ModuleTeams.Mode.values(), Mode.ROUND)
    private val rangeValue by floatValue("Range", 150f, 20f..300f)

	private var selfName = ""

	private enum class Mode(override val choiceName: String) : NamedChoice {
		ROUND("Round"),
		NAME("Name"),
		ARROW("Arrow")
	}

    var list:List<Entity>?= null
    var nameList: LinkedBlockingQueue<Entity>? = null
    fun EntityPlayer.isTeammate(): Boolean {
        if (modeValue == Mode.ROUND) {
            if(list.isNullOrEmpty()) return false
            return list!!.filterIsInstance<EntityPlayer>().any { it.username.equals(this.username, true) }
        }else if (modeValue == Mode.NAME) {
            if(nameList.isNullOrEmpty()) return false
            return nameList!!.filterIsInstance<EntityPlayer>().any { it.username.equals(this.username, true) }
        }
        return false
    }

    override fun onEnable() {
        super.onEnable()
        if(!nameList.isNullOrEmpty()) nameList!!.clear()
		if (session.thePlayer.metadata[EntityDataTypes.NAME].toString().contains("\n")) selfName = session.thePlayer.metadata[EntityDataTypes.NAME].toString().replace("\n", " ") else selfName = session.thePlayer.metadata[EntityDataTypes.NAME].toString()
        session.chat("Your Name: $selfName")
        when (modeValue) {
            Mode.ROUND -> {
                list=session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < rangeValue && !it.isBot() }
                for (entity in list!!) {
                    val a=entity as EntityPlayer
                    session.chat("Added to teams "+a.username)
                }
                state=false
            }
            Mode.NAME -> {
                nameList=LinkedBlockingQueue<Entity>()
                nameList!!.clear()
                for (entity in session.theWorld.entityMap.values.filter { it is EntityPlayer && !it.isBot() }) {
                    val a=entity as EntityPlayer
                    if(session.thePlayer.username.contains(a.username.substring(0,4))) {
                        session.chat("Added to teams " + a.username)
                        nameList!!.add(entity)
                    }
                }
            }
            else -> {
                /// TODO
            }
        }
        state=false
    }
}
