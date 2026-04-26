package ariecos.patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fingerprint

private const val EXTENSION_CLASS = "Lariecos/patches/gemini/extension/AllowlistHelper;"

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

    extendWith("extensions/extension.mpe")

    execute {
        val method = allowlistFingerprint.method
        val cweClass = method.definingClass

        // Prepend a call to our helper which checks the patched package names.
        // The helper receives this.a (the package name string) and returns
        // true if it matches a patched package — if so we return true immediately.
        method.addInstructions(
            0,
            """
                iget-object v0, p0, $cweClass->a:Ljava/lang/Object;
                invoke-static {v0}, $EXTENSION_CLASS->isPatchedPackage(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :not_patched
                return v0
                :not_patched
            """.trimIndent()
        )
    }
}
