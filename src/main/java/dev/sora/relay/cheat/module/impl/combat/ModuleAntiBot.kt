package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag

object ModuleAntiBot : CheatModule("AntiBot","防假人",CheatCategory.COMBAT) {

    fun EntityPlayer.isBot(): Boolean {
        if (this is EntityPlayerSP || !state)
            return false

        if(this.username.isBlank())
            return true

        if(this.metadata.flags.contains(EntityFlag.CAN_SHOW_NAME) != session.thePlayer.metadata.flags.contains(EntityFlag.CAN_SHOW_NAME))
            return true

        if(session.thePlayer.metadata[EntityDataTypes.NAME].toString().isBlank() != this.metadata[EntityDataTypes.NAME].toString().isBlank())
            return true

        if(session.thePlayer.metadata[EntityDataTypes.NAME].toString().contains("\n") != this.metadata[EntityDataTypes.NAME].toString().contains("\n"))
            return true

        return false
    }
}
