/* $Id: SyncProfileSelectionBox.java,v 1.9 2006/01/31 12:38:41 schaatser Exp $
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Component which lets the user choose the sync profile
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class SyncProfileSelectionBox extends JComboBox {
    private Logger LOG = Logger.getLogger(SyncProfileSelectionBox.class);

    // Entry for selecting custom sync profile
    public final static Object CUSTOM_SYNC_PROFILE_SELECTED = new Object();

    private final ValueModel selectionModel;
    private boolean settingNewValue;

	private ActionListener defaultActionListener;
    
    // Some swing classes like "JTable" fire several events on changes.
    // Since we don't want multiple warning boxes for one selection change,
    // the vetoableFolderSyncProfileChange methods uses this flag to remember that
    // it's already running.
    private static boolean	selectedItem;

    /**
     * Initalizes a new profile selection box.
     */
    private SyncProfileSelectionBox(ValueModel aSelectionModel,
        SyncProfile[] availableProfile)
    {
        super();
        this.selectionModel = aSelectionModel;

        // Build dropdown
        if (availableProfile != null && availableProfile.length != 0) {
            Object selectedProfile = selectionModel.getValue();
            for (int i = 0; i < availableProfile.length; i++) {
                addItem(availableProfile[i]);
                if (availableProfile[i].equals(selectedProfile)) {
                    setSelectedItem(availableProfile[i]);
                }
            }
        }

        // Add custom profile as selectable option
        //addItem(CUSTOM_SYNC_PROFILE_SELECTED);
        LOG.verbose("NOT ADDING \"Custom...\" entry to sync selection box");

        // Add behavior to box
        addBehavior();
    }

    /**
     * Initalizes a new profile selection box. Defaults will be added
     */
    public SyncProfileSelectionBox(ValueModel aSelectionModel) {
        this(aSelectionModel, SyncProfile.DEFAULT_SYNC_PROFILES);
    }

    /**
     * Easy to use contructor with a given sync profile
     * 
     * @param syncProfile
     */
    public SyncProfileSelectionBox(SyncProfile syncProfile) {
        this(new ValueHolder(syncProfile));
    }

    /**
     * Creates a new sync profile selection box with default sync profiles in
     * list
     */
    public SyncProfileSelectionBox() {
        this((SyncProfile) null);
    }

    // Getter/Setter **********************************************************

    /**
     * Answers the selection model for the box
     * 
     * @return
     */
    public ValueModel getSyncProfileModel() {
        return selectionModel;
    }

    /**
     * Sets the sync profile as selected
     * 
     * @param profile
     */
    public void setSelectedSyncProfile(SyncProfile profile) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).equals(profile)) {
                setSelectedIndex(i);
                return;
            }
        }

        // Not found set custom entry
        setSelectedItem(CUSTOM_SYNC_PROFILE_SELECTED);
    }

    /**
     * Answers the currently selected sync profile
     * 
     * @return
     */
    public SyncProfile getSelectedSyncProfile() {
        Object selection = selectionModel.getValue();
        if (selection instanceof SyncProfile) {
            return (SyncProfile) selection;
        } else if (selection == null) {
            return null;
        }

        throw new IllegalStateException("Not a valid profile selected: "
            + selection);
    }

    // Internal ***************************************************************

    /**
     * Shows a warning if the syncprofile will sync deletions. 
     * @param syncProfile
     *            the syncprofile selected
     * @return true only if the profile doesn't sync deletion or the user approved it
     */
    public static boolean showDeltionSyncProfileWarning(JComponent parent, SyncProfile syncProfile)
    {
        // Show warning if user wants to switch to a mode
        if (!(syncProfile.isSyncDeletionWithFriends() || syncProfile
            .isSyncDeletionWithOthers()))
        {
            return true;
        }

        String profName = Translation.getTranslation(syncProfile
            .getTranslationId());
        return JOptionPane.showConfirmDialog(parent, 
            Translation
                .getTranslation("synchronisation.warning.automatic_deletions_notice"),
            Translation.getTranslation("synchronisation.notice.title",
                profName), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) ==
                	JOptionPane.OK_OPTION;

    }
    

    /**
     * Called to allow the user to cancel changes.
     * 
     * @param folder The folder the user wants to change the SyncProfile. Folder may be null
     * for not yet created folders.
     * @param profile The new profile to apply
     * @return true if the changes were applied (Calling objects may consider to undo
     * their selection if this returns false).
     */
    public static boolean vetoableFolderSyncProfileChange(Folder folder, SyncProfile profile) {
    	if (folder != null && folder.getSyncProfile() == profile)
    		return true;
    	if (selectedItem)
    		return false;
    	selectedItem = true;
    	if (showDeltionSyncProfileWarning(null, profile)) {
    		if (folder != null)
    			folder.setSyncProfile(profile);
    		selectedItem = false;
    		return true;
    	}
		selectedItem = false;
    	return false;
    }
    
    /**
     * Adds a default ActionListener which will perform the necessary changes
     * to the given folder. 
     * @param folder The folder on which to change profils on
     */
    public void addDefaultActionListener(final Folder folder) {
    	if (defaultActionListener != null)
    		removeActionListener(defaultActionListener);
    	addActionListener(defaultActionListener = new ActionListener() {
            // called if in syncProfileChooser a selection is made
            public void actionPerformed(ActionEvent e) {
                if (!SyncProfileSelectionBox.vetoableFolderSyncProfileChange(folder, (SyncProfile)getSelectedItem()))
                	setSelectedItem(folder.getSyncProfile());
            }
    	});
    }
    
    /**
     * Adds behavior to box
     */
    private void addBehavior() {
        settingNewValue = false;

        // Listen on value changes on selection model
        selectionModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!settingNewValue) {
                    LOG.verbose("Property change on selection model: "
                        + evt.getNewValue());
                    // Set selected item
                    setSelectedItem(evt.getNewValue());
                }
            }
        });

        // Listen on changes on dropdown box
        addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }

                // TODO: Popup custom sync profile editor if selected custom
                
                if (e.getItem() == CUSTOM_SYNC_PROFILE_SELECTED) {
                    LOG.error("UNIMPLEMENTED !!!");
                    return;
                }

                LOG.verbose("Sync profile selected: " + e.getItem());
                settingNewValue = true;
                selectionModel.setValue(e.getItem());
                settingNewValue = false;
            }
        });
    }
}