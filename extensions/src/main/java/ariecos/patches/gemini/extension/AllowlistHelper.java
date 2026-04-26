package ariecos.patches.gemini.extension;

public class AllowlistHelper {
    public static boolean isPatchedPackage(String packageName) {
        if (packageName == null) return false;
        return packageName.equals("app.morphe.android.apps.youtube.music")
            || packageName.equals("app.morphe.android.youtube");
    }
}
