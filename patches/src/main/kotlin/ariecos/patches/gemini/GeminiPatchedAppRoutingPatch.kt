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

        method.addInstructions(
            0,
            """
                iget-object p1, p0, $cweClass->a:Ljava/lang/Object;
                const-string p2, "$ytm"
                invoke-virtual {p1, p2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result p2
                if-eqz p2, :check_yt
                const/4 p1, 0x1
                return p1
                :check_yt
                const-string p2, "$yt"
                invoke-virtual {p1, p2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result p2
                if-eqz p2, :no_match
                const/4 p1, 0x1
                return p1
                :no_match
            """.trimIndent()
        )
    }
}
