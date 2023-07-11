package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.module.impl.combat.*
import dev.sora.relay.cheat.module.impl.misc.*
import dev.sora.relay.cheat.module.impl.movement.*
import dev.sora.relay.cheat.module.impl.visual.*
import dev.sora.relay.game.GameSession

class ModuleManager(private val session: GameSession) {

    val modules = mutableListOf<CheatModule>()

    fun registerModule(module: CheatModule) {
		module.session = session
		module.moduleManager = this
        modules.add(module)
        module.register(session.eventManager)
    }

    fun init() {
		registerModule(ModuleClickTP())
		registerModule(ModuleAntiKick())
		registerModule(ModuleTextSpoof())
		registerModule(ModuleTeams)
		registerModule(ModuleAntiBot)
		registerModule(ModuleTPFly())
		registerModule(ModuleGodMode())
		registerModule(ModuleCriticalHit())
		registerModule(ModuleNoFire())
		registerModule(ModuleSprint())
		registerModule(ModuleHUD())
        registerModule(ModuleFly())
        registerModule(ModuleVelocity())
        registerModule(ModuleKillAura())
        registerModule(ModuleSpammer())
        registerModule(ModuleBGM())
        registerModule(ModuleDisabler())
        registerModule(ModuleOpFightBot())
        registerModule(ModuleNoSkin())
        registerModule(ModuleDeviceSpoof())
        registerModule(ModuleResourcePackSpoof())
        registerModule(ModuleNoFall())
        registerModule(ModuleAntiBlind())
        registerModule(ModuleFastBreak())
        registerModule(ModuleBlink())
        registerModule(ModuleScaffold())
        registerModule(ModuleInventoryHelper())
		registerModule(ModuleAirJump())
		registerModule(ModuleClip())
		registerModule(ModuleNoHurtCam())
		registerModule(ModuleSurround())
		registerModule(ModuleCrystalAura())
		registerModule(ModuleHitEffect())
		registerModule(ModuleMiner())
		registerModule(ModuleSpeed())
    }

	inline fun <reified T : CheatModule> getModule(t: Class<T>): T {
		return modules.filterIsInstance<T>().first()
	}
}
