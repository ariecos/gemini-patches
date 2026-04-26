package ariecos.patches.gemini

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fingerprint
import app.morphe.patcher.StringFilter
import app.morphe.patcher.StringComparisonType

val allowlistFingerprint = fingerprint {
    returns("Z")
    parameters()
    strings("com.google.android.apps.youtube.music")
}

@Suppress("unused")
val geminiRoutingPatch = bytecodePatch(
    name = "Route intents to patched apps",
    description = "Replaces unused internal package names in Gemini's allowlist " +
                  "with Morphe-patched YouTube and YouTube Music package names.",
    default = true,
) {
    compatibleWith(
        "com.google.android.apps.bard",
        "com.google.android.googlequicksearchbox",
    )

    execute {
        val method = allowlistFingerprint.method

        // Replace the two obscure ambient music test packages (which will never
        // be installed on a real device) with our patched package names.
        // These are loaded into v34 and v35 in the original smali.
        // No register manipulation needed — just swap the string constants.

        method.implementation!!.instructions.toList().forEach { instruction ->
            val refInstr = instruction as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
                ?: return@forEach
            val ref = refInstr.reference.toString()
            val newStr = when (ref) {
                "com.google.intelligence.sense.ambientmusic.functional.emulator" ->
                    "app.morphe.android.apps.youtube.music"
                "com.google.intelligence.sense.ambientmusic.history.functional" ->
                    "app.morphe.android.youtube"
                else -> return@forEach
            }
            val idx = method.implementation!!.instructions.indexOf(instruction)
            method.replaceInstruction(
                idx,
                "const-string v${(instruction as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA}, \"$newStr\""
            )
        }
    }
}
