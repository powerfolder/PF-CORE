/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder;

import java.util.logging.Level;

import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.skin.Origin;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.util.Reject;

/**
 * Refelects an entry setting in the preferences. Provides basic method for
 * accessing and setting the prefs. Preferences are stored (on windows) in the
 * registry.
 */
public enum PreferencesEntry {

    /**
     * #2427: The last node ID this preferences ran on.
     */
    LAST_NODE_ID("last_nodeID_obf", ""),

    /**
     * Show offline members
     */
    NODE_MANAGER_MODEL_SHOW_OFFLINE("node_manager_model_show_offline", true),
    /** find offline users */
    FRIEND_SEARCH_HIDE_OFFLINE("FriendsSearch_HideOfflineUsers", false),

    QUIT_ON_X("quitonx", false),

    ASK_FOR_QUIT_ON_X("AskForQuitOnX", true),

    WARN_ON_CLOSE("WarnOnClose", true),

    EXPERT_MODE("ExpertMode", false),

    BEGINNER_MODE("BeginnerMode", true),
    
    SHOW_DEVICES("show.devices", false),

    VIEW_ACHIVE("view.archive", true),

    UNDERLINE_LINKS("UnderlineLinks", false),

    FILE_NAME_CHECK("folder.check_filenames", false),

    CHECK_UPDATE("updatechecker.askfornewreleaseversion", true),

    /**
     * Whether to show system notifications when minimized.
     */
    SHOW_SYSTEM_NOTIFICATIONS("show.system.notifications", true) {
        protected Object getDefaultValue(Controller controller) {
            return PreferencesEntry.EXPERT_MODE.getValueBoolean(controller);
        }
    },

    /**
     * the pref that holds a boolean value a warning is displayed if no direct connectivity is given.
     */
    WARN_ON_NO_DIRECT_CONNECTIVITY("warn_on_no_direct_connectivity", false),

    /** Warn user if cloud space is getting full (90%+). */
    WARN_FULL_CLOUD("warn.poor.quality", true),

    SETUP_DEFAULT_FOLDER("setup_default_folder", false),

    /**
     * If the last password of login should be reminded.
     */
    SERVER_REMEMBER_PASSWORD("server_remind_password", true),

    DOCUMENT_LOGGING("document.logging", Level.WARNING.getName()),

    AUTO_EXPAND("auto.expand", false),

    /** Whether the user uses OS. If not, don't show OS stuff. */
    USE_ONLINE_STORAGE("use.os", true),

    /** How many seconds the notification should display. */
    NOTIFICATION_DISPLAY("notification.display", 10),

    /** How translucent the notification should display, as percentage. */
    NOTIFICATION_TRANSLUCENT("notification.translucent", 0),

    /** Skin name. */
    SKIN_NAME("skin.name", Origin.NAME),

    /** The 'Show offline' checkbox on the ComputersTab. */
    SHOW_OFFLINE("show.offline", true),

    SHOW_ASK_FOR_PAUSE("show.ask.for.pause", true),

    MAIN_FRAME_MAXIMIZED("mainframe.maximized", false),

    FILE_SEARCH_MODE("file.search.mode",
        DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME),

    SHOW_TYPICAL_FOLDERS("show.typical.folders", false),

    /**
     * Show PowerFolder base dir short cut on the desk top.
     */
    CREATE_BASEDIR_DESKTOP_SHORTCUT("display.powerfolders.shortcut", true),

    /**
     * Whether to set PowerFolders as a Favorite Link in Windows Explorer.
     */
    CREATE_FAVORITES_SHORTCUT("use.pf.link", true),

    PAUSED("paused", false),

    INCLUDE_DELETED_FILES("include.deleted.files", false),

    /**
     * Show hidden files in the file browser.
     */
    SHOW_HIDDEN_FILES("show.hidden.files", false),

    /**
     * Enable the UI-Mode selector.
     * PFC-2385
     */
    MODE_SELECT("mode.select.enabled", true),

    /**
     * Show the "Browse" Button / Link in the main window and the tray icon's
     * context menu. PFC-2624
     */
    SHOW_BROWSE("show.browse", true),

    /**
     * PFC-2395
     */
    ENABLE_CONTEXT_MENU("context_menu.enabled", true);

    /** String, Boolean, Integer */
    private Class<?> type;

    private String preferencesKey;
    private Object defaultValue;

    // Methods/Constructors ***************************************************

    PreferencesEntry(String aPreferencesKey, boolean theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        type = Boolean.class;
        preferencesKey = aPreferencesKey;
        defaultValue = theDefaultValue;
    }

    PreferencesEntry(String aPreferencesKey, int theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        type = Integer.class;
        preferencesKey = aPreferencesKey;
        defaultValue = theDefaultValue;
    }

    PreferencesEntry(String aPreferencesKey, String theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        type = String.class;
        preferencesKey = aPreferencesKey;
        defaultValue = theDefaultValue;
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
            (String) getDefaultValue(controller));
    }

    public Integer getDefaultValueInt() {
        return (Integer) defaultValue;
    }

    protected Object getDefaultValue(Controller controller) {
        return defaultValue;
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
            (Integer) getDefaultValue(controller));
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
            (Boolean) getDefaultValue(controller));
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
            preferencesKey, getDefaultValue(controller));
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
