package ariecos.patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fingerprint

val allowlistFingerprint = fingerprint {
    returns("Z")
    parameters()
    strings("com.google.android.apps.youtube.music")
}

@Suppress("unused")
val geminiRoutingPatch = bytecodePatch(
    name = "Route intents to patched apps",
    description = "Extends Gemini's package allowlist to include Morphe-patched YouTube and YouTube Music.",
    default = true,
) {
    compatibleWith(
        "com.google.android.apps.bard",
        "com.google.android.googlequicksearchbox",
    )

    execute {
        val method = allowlistFingerprint.method
        val ytm = "app.morphe.android.apps.youtube.music"
        val yt  = "app.morphe.android.youtube"

        val instructions = method.implementation!!.instructions.toList()

        var lastReturnIdx = -1
        for (i in instructions.indices.reversed()) {
            if (instructions[i].opcode.name == "RETURN") {
                lastReturnIdx = i
                break
            }
        }

        method.addInstructions(
            lastReturnIdx,
            """
                const-string v0, "$ytm"
                invoke-virtual {v1, v0}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-nez v0, :patched_true
                const-string v0, "$yt"
                invoke-virtual {v1, v0}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :patched_end
                :patched_true
                const/4 v0, 0x1
                return v0
                :patched_end
            """.trimIndent()
        )
    }
}
