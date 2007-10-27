package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Translation;

/**
 * Enumeration of preferred preselected navigation panel.
 */
public enum StartPanel {

    OVERVIEW("overview", "preferences.dialog.startPanel.overview"),
    MY_FOLDERS("myFolders", "preferences.dialog.startPanel.myFolders"),
    DOWNLOADS("downloads", "preferences.dialog.startPanel.downloads");

    private String name;
    private String description;

    StartPanel(String name, String descriptionKey) {
        this.name = name;
        description = Translation.getTranslation(descriptionKey);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }



    public static StartPanel decode(String nameArg) {
        if (OVERVIEW.name.equals(nameArg)) {
            return OVERVIEW;
        } else if (MY_FOLDERS.name.equals(nameArg)) {
            return MY_FOLDERS;
        } else if (DOWNLOADS.name.equals(nameArg)) {
            return DOWNLOADS;
        } else {
            throw new IllegalArgumentException("Bad StartPanel name: " + nameArg);
        }
    }

    public String toString() {
        return description;
    }
}
