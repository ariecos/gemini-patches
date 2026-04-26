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
        val cweClass = method.definingClass
        val ytm = "app.morphe.android.apps.youtube.music"
        val yt  = "app.morphe.android.youtube"

        // Increase register count to make room for our new registers
        val implementation = method.implementation!!
        val newRegisterCount = implementation.registerCount + 2
        method.implementation = implementation.withRegisterCount(newRegisterCount)

        val r0 = newRegisterCount - 2
        val r1 = newRegisterCount - 1

        method.addInstructions(
            0,
            """
                iget-object v$r0, p0, $cweClass->a:Ljava/lang/Object;
                const-string v$r1, "$ytm"
                invoke-virtual {v$r0, v$r1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v$r1
                if-eqz v$r1, :check_yt
                const/4 v$r0, 0x1
                return v$r0
                :check_yt
                const-string v$r1, "$yt"
                invoke-virtual {v$r0, v$r1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v$r1
                if-eqz v$r1, :no_match
                const/4 v$r0, 0x1
                return v$r0
                :no_match
            """.trimIndent()
        )
    }
}
