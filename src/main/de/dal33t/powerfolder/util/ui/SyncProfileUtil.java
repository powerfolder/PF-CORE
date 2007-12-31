package de.dal33t.powerfolder.util.ui;

import javax.swing.Icon;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Helper for rendering sync profiles.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SyncProfileUtil {
    private SyncProfileUtil() {
    }

    /**
     * @param syncPercentage
     * @return the rendered sync percentage.
     */
    public static final String renderSyncPercentage(double syncPercentage) {
        if (syncPercentage >= 0) {
            return Translation.getTranslation("percent.place.holder",
                    Format.NUMBER_FORMATS.format(syncPercentage));
        }
        return Translation.getTranslation("percent.place.holder", "?");
    }

    /**
     * Returns the icon for the sync status percentage.
     * 
     * @param syncPercentage
     *            in % between 0 and 100. Below 0 unkown sync status icons is
     *            returned
     * @return
     */
    public static Icon getSyncIcon(double syncPercentage) {
        if (syncPercentage < 0) {
            return Icons.FOLDER_SYNC_UNKNOWN;
        } else if (syncPercentage <= 20) {
            return Icons.FOLDER_SYNC_0;
        } else if (syncPercentage <= 50) {
            return Icons.FOLDER_SYNC_1;
        } else if (syncPercentage <= 80) {
            return Icons.FOLDER_SYNC_2;
        } else {
            return Icons.FOLDER_SYNC_3;
        }
    }
}
