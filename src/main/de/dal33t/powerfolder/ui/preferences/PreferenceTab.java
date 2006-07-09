package de.dal33t.powerfolder.ui.preferences;

import javax.swing.JPanel;

public interface PreferenceTab {

    /** true if PowerFolder should restart for all changed to be in effect */
    public boolean needsRestart();

    /**
     * Tells the tab to save it's settings now. Afterwards
     * Controller.saveConfig() is called, so you don't need to do it.
     */
    public void save();

    /**
     * Shoud return false if some validation failed.
     */
    public boolean validate();

    /** returns a localized tabname for this tab */
    public String getTabName();

    /** the UI component that should appear in this tab */
    public JPanel getUIPanel();

    /**
     * undo those changes that where done immediately like theme change *
     */
    public void undoChanges();
}
