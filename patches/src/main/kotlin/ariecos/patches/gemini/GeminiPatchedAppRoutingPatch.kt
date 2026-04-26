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

        // From the smali we know the structure:
        // Instructions 0-45: load strings into v1-v35, build first eig.q() array
        // Instruction 46: move-result-object v42 (result of filled-new-array)
        // Instructions 47-52: load remaining strings v36-v41
        // Instruction 53: invoke-static/range eig.q() 
        // Instruction 54: move-result-object v0
        // Instruction 55: move-object/from16 v1, p0
        // Instruction 56: iget-object v1, v1, Lcwe;->a
        // Instruction 57: invoke-virtual contains()
        // Instruction 58: move-result v0
        // Instruction 59: if-eqz v0, :cond_0
        // Instruction 60: const/4 v0, 0x1
        // Instruction 61: return v0
        // :cond_0 starts at instruction 62
        // At this point v1 = this.a (Object), v0 = false result
        // We inject here before the second eig.q() call

        val instructions = method.implementation!!.instructions.toList()
        
        // Find :cond_0 by finding the if-eqz instruction and injecting right after it
        // The if-eqz skips the return true, so cond_0 is after instruction 61
        // We inject at instruction 62 (right after the early return true block)
        // At that point v1 already holds this.a as a verified Object reference

        // Find the index of the first "if-eqz" instruction
        val ifEqzIdx = instructions.indexOfFirst { it.opcode.name == "IF_EQZ" }
        // Inject 3 instructions after if-eqz (skip past: const/4, return, landing at cond_0)
        val injectIdx = ifEqzIdx + 3

        method.addInstructions(
            injectIdx,
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
