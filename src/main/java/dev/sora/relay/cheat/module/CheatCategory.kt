package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.value.NamedChoice

enum class CheatCategory(override val choiceName: String, val chinese: String) : NamedChoice {
	COMBAT("Combat","战斗"),
	MOVEMENT("Movement","移动"),
	VISUAL("Visual","视觉"),
	MISC("Misc","其它")
}
