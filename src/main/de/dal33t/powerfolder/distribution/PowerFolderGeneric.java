package de.dal33t.powerfolder.distribution;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.skin.Origin;
import de.dal33t.powerfolder.ui.LookAndFeelSupport;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.util.ConfigurationLoader;

public class PowerFolderGeneric extends AbstractDistribution {
    public static final String DEFAULT_WEB_DOWNLOAD_CLIENT_URL = "http://download.powerfolder.com/pro/win/PowerFolder_Generic_Latest_Installer.exe";
    public static final String WEB_MAC_CLIENT_URL = "http://download.powerfolder.com/pro/mac/PowerFolder_Generic_Latest_Mac.dmg";
    public static final String WEB_LINUX_CLIENT_URL = "http://download.powerfolder.com/pro/linux/PowerFolder_Generic_Latest_Linux.tar.gz";
    public static final String WEB_LINUX_CLIENT_DEB_32_URL = "http://download.powerfolder.com/pro/linux/PowerFolder_Generic_Latest_i386.deb";
    public static final String WEB_LINUX_CLIENT_DEB_64_URL = "http://download.powerfolder.com/pro/linux/PowerFolder_Generic_Latest_amd64.deb";
    public static final String WEB_LINUX_CLIENT_RPM_32_URL = "http://download.powerfolder.com/pro/linux/PowerFolder_Generic_Latest.i386.rpm";
    public static final String WEB_LINUX_CLIENT_RPM_64_URL = "http://download.powerfolder.com/pro/linux/PowerFolder_Generic_Latest.x86_64.rpm";
    public static final String WEB_IOS_CLIENT_URL = "https://itunes.apple.com/app/powerfolder/id536214931";
    public static final String WEB_ANDROID_CLIENT_URL = "https://market.android.com/details?id=de.goddchen.android.powerfolder.A";
    public static final String WEB_START_CLIENT_URL = "http://download.powerfolder.com/pro/webstart_generic/PowerFolder.jnlp";
    public static final String CHECK_VERSION_URL = "http://checkversion.powerfolder.com/PowerFolderGeneric_LatestVersion.txt";

    public String getName() {
        return "PowerFolder Pro Generic";
    }

    public String getBinaryName() {
        return "PowerFolder";
    }

    public void init(Controller controller) {
        super.init(controller);

        loadPreConfigFromClasspath(controller, null);

        // #2467: Get server URL from the installer
        ConfigurationLoader.loadAndMergeFromInstaller(controller);

        if (!ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM.hasValue(controller)) {
            // Other default for generic. Don't overwrite if set
            ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM.setValue(
                controller, true);
        }

        boolean prompt = ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM
            .getValueBoolean(getController());
        if (prompt && ServerClient.isPowerFolderCloud(getController())
            && controller.isUIEnabled())
        {
            try {
                LookAndFeelSupport
                    .setLookAndFeel(new Origin().getLookAndFeel());
            } catch (Exception e) {
                logSevere("Failed to set look and feel", e);
            }
            // Configuration required
            new ConfigurationLoaderDialog(controller).openAndWait();
        }
    }

    public boolean allowSkinChange() {
        return true;
    }
}
