package ariecos.patches.gemini

import app.morphe.patcher.patch.annotation.Patch
import app.morphe.patcher.patch.annotation.Package
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.options.PatchOption.Companion.stringPatchOption
import app.morphe.patcher.fingerprint

val ytMusicPackageOption by stringPatchOption(
    key         = "ytMusicPackage",
    default     = "com.google.android.apps.youtube.music.morphe",
    title       = "YouTube Music patched package name",
    description = "Package name of your patched YouTube Music install.",
    required    = false,
)

val youtubePackageOption by stringPatchOption(
    key         = "youtubePackage",
    default     = "com.google.android.youtube.morphe",
    title       = "YouTube patched package name",
    description = "Package name of your patched YouTube install.",
    required    = false,
)

val allowlistFingerprint = fingerprint {
    returns("Z")
    parameters()
    strings("com.google.android.apps.youtube.music")
}

@Patch(
    name        = "Route intents to patched apps",
    description = "Extends Gemini's internal package allowlist to include " +
                  "Morphe-patched versions of YouTube and YouTube Music, so " +
                  "voice/text commands correctly open the patched apps.",
    use         = true,
    compatiblePackages = [
        Package("com.google.android.apps.bard"),
        Package("com.google.android.googlequicksearchbox"),
    ],
)
val geminiRoutingPatch = bytecodePatch(
    name = "Route intents to patched apps",
) {
    val match by allowlistFingerprint()

    execute {
        val method = match.mutableMethod
        val ytm = ytMusicPackageOption ?: "com.google.android.apps.youtube.music.morphe"
        val yt  = youtubePackageOption ?: "com.google.android.youtube.morphe"
        val cweClass = method.definingClass

        val instructions = method.implementation!!.instructions.toList()
		val lastReturnIndex = instructions.indices.last { i ->
			instructions[i].opcode.name.let { it == "RETURN" || it == "RETURN_VOID" || it == "RETURN_OBJECT" || it == "RETURN_WIDE" }
}

        method.addInstructionsWithLabels(
            lastReturnIndex,
            """
                iget-object v0, p0, ${cweClass}->a:Ljava/lang/Object;
                const-string v1, "$ytm"
                invoke-virtual {v0, v1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-nez v1, :patched_match
                const-string v1, "$yt"
                invoke-virtual {v0, v1}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-nez v1, :patched_match
                goto :original_false
                :patched_match
                const/4 v1, 0x1
                return v1
                :original_false
            """.trimIndent()
        )
    }
}
