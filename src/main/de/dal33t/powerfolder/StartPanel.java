package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Enumeration of preferred preselected navigation panel.
 */
public enum StartPanel {

    OVERVIEW("preferences.dialog.startPanel.overview"), MY_FOLDERS(
        "preferences.dialog.startPanel.myFolders"), DOWNLOADS(
        "preferences.dialog.startPanel.downloads");

    private String description;

    StartPanel(String descriptionKey) {
        description = Translation.getTranslation(descriptionKey);
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return description;
    }

    public static StartPanel valueForLegacyName(String startPanelName) {
        // Migration
        try {

            if (startPanelName != null) {
                startPanelName = startPanelName.toUpperCase();
                if (startPanelName.equalsIgnoreCase("myFolders")) {
                    startPanelName = StartPanel.MY_FOLDERS.name();
                }
            }
            return valueOf(startPanelName);
        } catch (Exception e) {
            Logger.getLogger(StartPanel.class).error(e);
            return OVERVIEW;
        }
    }
}
