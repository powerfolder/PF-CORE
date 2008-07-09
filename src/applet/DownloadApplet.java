import javax.swing.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

public class DownloadApplet extends JApplet {

    private static final int MIN_MAJOR = 1;
    private static final int MIN_MINOR = 6;
    private static final int MIN_BUILD_A = 0;
    private static final int MIN_BUILD_B = 6;

    public void start() {

        String os = System.getProperty("os.name");
        String javaVersion = System.getProperty("java.version");
        // Default
        String javascript = "WebStart";
        try {
            if (os != null && os.startsWith("Windows")) {
                // Windows (Java)
                javascript = "Win32withJava";

                // Parse x.y.z_vv version for minimum java version.
                StringTokenizer st = new StringTokenizer(javaVersion, ".");
                if (st.countTokens() == 3) {
                    int major = Integer.valueOf(st.nextToken()).intValue();
                    int minor = Integer.valueOf(st.nextToken()).intValue();
                    int buildA = 0;
                    int buildB = 0;

                    // Try to get build information
                    String build = st.nextToken();
                    StringTokenizer st2 = new StringTokenizer(build, "_");
                    if (st2.countTokens() == 2) {
                        buildA = Integer.valueOf(st2.nextToken()).intValue();
                        buildB = Integer.valueOf(st2.nextToken()).intValue();
                    }
                    if (goodVersion(MIN_MAJOR, MIN_MINOR, MIN_BUILD_A,
                        MIN_BUILD_B, major, minor, buildA, buildB))
                    {
                        // Windows (with Java)
                        javascript = "Win32withoutJava";
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            getAppletContext().showDocument(
                new URL("javascript:selectDownload(\"" + javascript + "\")"));
        } catch (MalformedURLException me) {
            // Hmmmmm.
        }
    }

    /**
     * See if the local java version is good enough.
     * 
     * @param minMajor
     * @param minMinor
     * @param minBuildA
     * @param minBuildB
     * @param major
     * @param minor
     * @param buildA
     * @param buildB
     * @return
     */
    private static boolean goodVersion(int minMajor, int minMinor,
        int minBuildA, int minBuildB, int major, int minor, int buildA,
        int buildB)
    {
        if (major > minMajor) {
            return true;
        } else if (major == minMajor) {
            if (minor > minMinor) {
                return true;
            } else if (minor == minMinor) {
                if (buildA > minBuildA) {
                    return true;
                } else if (buildA == minBuildA) {
                    return buildB >= minBuildB;
                }

            }
        }
        return false;
    }
}
