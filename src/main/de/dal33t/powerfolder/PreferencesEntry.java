package de.dal33t.powerfolder;

import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.ValueModel;

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

    QUIT_ON_X("quitonx", false),
    
    WARN_ON_CLOSE("WarnOnClose", true),

    ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN(
        "AskForFriendshipOnPrivateFolderJoin", true),

    ASK_FOR_FRIENDSHIP_MESSAGE(
        "AskForFriendshipMessage", true),

    SHOW_PREVIEW_PANEL("ShowPreviewPanel", false),

    UI_COLOUR_THEME("UIColorTheme", null),

    SHOW_ADVANCED_SETTINGS("ShowAdvancedSettings", false),

    SMALL_TOOLBAR("SmallToolbar", false),

    FILE_NAME_CHECK("folder.check_filenames", true),

    CHECK_UPDATE("updatechecker.askfornewreleaseversion", true),

    START_PANEL("start.panel", StartPanel.OVERVIEW.name()),

    /**
     * the pref that holds a boolean value if the connection should be
     * tested and a warning displayed if limited connectivty is given.
     */
    TEST_CONNECTIVITY("test_for_connectivity", true),

    /**
     * The pref that holds a boolean value if PowerFolder should detect low memory (MemoryMonitor).
     */
    DETECT_LOW_MEMORY("detect_low_memory", true);

    /** String, Boolean, Integer */
    private Class type;

    private String preferencesKey;
    private Object defaultValue;

    // Methods/Constructors ***************************************************

    private PreferencesEntry(String aPreferencesKey, boolean theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Boolean.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, int theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Integer.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, String theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = String.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or
     */
    public String getValueString(Controller controller) {
        if (!type.isAssignableFrom(String.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot acces as String");
        }
        return controller.getPreferences().get(preferencesKey,
            (String) defaultValue);
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
        if (!type.isAssignableFrom(Integer.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Integer");
        }
        return controller.getPreferences().getInt(preferencesKey,
            (Integer) defaultValue);
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
        if (!type.isAssignableFrom(Boolean.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Boolean");
        }
        return controller.getPreferences().getBoolean(preferencesKey,
            (Boolean) defaultValue);
    }

    /**
     * Constructs a preferences adapter which is directly bound to the
     * preferences entry.
     * 
     * @param controller
     *            the controller
     * @return the model bound to the pref entry.
     */
    public ValueModel getModel(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return new PreferencesAdapter(controller.getPreferences(),
            preferencesKey, defaultValue);
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
        if (!type.isAssignableFrom(String.class)) {
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
        if (!type.isAssignableFrom(Boolean.class)) {
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
        if (!type.isAssignableFrom(Integer.class)) {
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
