package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Reject;

/**
 * Refelects an entry setting in the preferences. Provides basic method for
 * accessing and setting the prefs. Preferences are stored (on windows) in the
 * registry.
 */
public enum PreferencesEntry {
    /**
     * Hide offline friends
     */
    NODEMANAGERMODEL_HIDEOFFLINEFRIENDS("NodeManagerModel_HideOfflineFriends",
        false),

    /** find offline users */
    FRIENDSEARCH_HIDEOFFLINE("FriendsSearch_HideOfflineUsers", false),

    WARN_ON_CLOSE("WarnOnClose", true),

    ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN(
        "AskForFriendshipOnPrivateFolderJoin", true),

    SHOW_PREVIEW_PANEL("ShowPreviewPanel", false),

    UI_COLOUR_THEME("UIColorTheme", null),

    SHOW_ADVANCED_SETTINGS("ShowAdvancedSettings", false),

    FILE_NAME_CHECK("folder.check_filenames", true),

    CHECK_UPDATE("updatechecker.askfornewreleaseversion", true),

    /**
     * the pref that holds a boolean value if the connection should be
     * tested and a warning displayed if limited connectivty is given.
     */
    TEST_CONNECTIVITY("test_for_connectivity", true);

    /** String, Boolean, Integer */
    private Class type;

    private String preferencesKey;
    private String defaultValueString;
    private boolean defaultValueBoolean;
    private int defaultValueInteger;

    // Methods/Constructors ***************************************************

    private PreferencesEntry(String aPreferencesKey, boolean theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Boolean.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValueBoolean = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, int theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Integer.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValueInteger = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, String theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = String.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValueString = theDefaultValue;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or
     */
    public String getValueString(Controller controller) {
        if (type != String.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot acces as String");
        }
        return controller.getPreferences().get(preferencesKey,
            defaultValueString);
    }

    /**
     * the preferences entry if its a Integer.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the preferences for this entry. or the
     *         default value if value not set.
     */
    public Integer getValueInt(Controller controller) {
        if (type != Integer.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Integer");
        }
        return controller.getPreferences().getInt(preferencesKey,
            defaultValueInteger);
    }

    /**
     * Parses the configuration entry into a Boolen.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Boolean getValueBoolean(Controller controller) {
        if (type != Boolean.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Boolean");
        }
        return controller.getPreferences().getBoolean(preferencesKey,
            defaultValueBoolean);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, String value) {
        Reject.ifNull(controller, "Controller is null");
        if (type != String.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as String");
        }
        controller.getPreferences().put(preferencesKey, value);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, boolean value) {
        Reject.ifNull(controller, "Controller is null");
        if (type != Boolean.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as Boolean");
        }
        controller.getPreferences().putBoolean(preferencesKey, value);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, int value) {
        Reject.ifNull(controller, "Controller is null");
        if (type != Integer.class) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as Integer");
        }
        controller.getPreferences().putInt(preferencesKey, value);
    }

    /**
     * Removes the entry from the preferences.
     * 
     * @param controller
     *            the controller to use
     */
    public void removeValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        controller.getPreferences().remove(preferencesKey);
    }
}
