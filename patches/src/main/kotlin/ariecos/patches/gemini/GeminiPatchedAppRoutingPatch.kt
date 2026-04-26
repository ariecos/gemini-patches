package ariecos.patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
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
        val ytm = "com.google.android.apps.youtube.music.morphe"
        val yt  = "com.google.android.youtube.morphe"

        method.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, $cweClass->a:Ljava/lang/Object;
                const-string v1, "$ytm"
                invoke-virtual {v0, v1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-nez v1, :patched_match
                const-string v1, "$yt"
                invoke-virtual {v0, v1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :not_matched
                :patched_match
                const/4 v1, 0x1
                return v1
                :not_matched
            """.trimIndent()
        )
    }
}
