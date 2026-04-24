package ariecos.patches.gemini

import app.morphe.patcher.patch.resourcePatch

@Suppress("unused")
val geminiGmsCorePatch = resourcePatch(
    name = "GmsCore MicroG support",
    description = "Injects the MicroG signature-spoof permission into Gemini's AndroidManifest.",
    default = true,
) {
    compatibleWith(
        "com.google.android.apps.bard",
        "com.google.android.googlequicksearchbox",
    )

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val manifestNode = manifest.getElementsByTagName("manifest").item(0)

            val existingPerms = manifest.getElementsByTagName("uses-permission")
            val alreadyPresent = (0 until existingPerms.length).any { i ->
                existingPerms.item(i).attributes
                    ?.getNamedItem("android:name")
                    ?.nodeValue == "org.microg.gms.permission.FAKE_PACKAGE_SIGNATURE"
            }

            if (!alreadyPresent) {
                val permNode = manifest.createElement("uses-permission")
                permNode.setAttribute(
                    "android:name",
                    "org.microg.gms.permission.FAKE_PACKAGE_SIGNATURE"
                )
                manifestNode.appendChild(permNode)
            }
        }
    }
}
