/* $Id: ChangeSyncProfileAction.java,v 1.7 2006/02/16 13:58:09 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;

/**
 * Action which changes the syncprofile of the folder if performed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class ChangeSyncProfileAction extends AbstractAction implements
    SelectionChangeListener
{
    private SyncProfile syncProfile;
    private PropertyChangeListener folderSyncProfileChangeListener;
    private SelectionModel selectionModel;

    /**
     * Constructs a new action which changes to syncprofile on a folder
     * 
     * @param folderModel
     *            the model of the folder
     * @param aSyncProfile
     * @param controller
     */
    public ChangeSyncProfileAction(SyncProfile aSyncProfile,
        SelectionModel selectionModel)
    {
        super(Translation.getTranslation(aSyncProfile.getTranslationId()));
        this.syncProfile = aSyncProfile;
        this.selectionModel = selectionModel;
        selectionModel.addSelectionChangeListener(this);
        folderSyncProfileChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() instanceof Folder) {
                    Folder folder = (Folder) evt.getSource();
                    markAsSelected(folder.getSyncProfile().equals(syncProfile));
                }
            }
        };
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();
        Object oldSelection = selectionModel.getOldSelection();

        if (oldSelection instanceof Folder) {
            // Remove listener from old folder
            Folder folder = (Folder) oldSelection;
            folder
                .removePropertyChangeListener(folderSyncProfileChangeListener);
        }
        if (selection instanceof Folder) {
            // Add listener to new folder
            Folder folder = (Folder) selection;
            folder.addPropertyChangeListener(Folder.PROPERTY_SYNC_PROFILE,
                folderSyncProfileChangeListener);
            markAsSelected(folder.getSyncProfile().equals(syncProfile));
        }

    }

    public void actionPerformed(ActionEvent e) {
        Object selection = selectionModel.getSelection();

        if (!(selection instanceof Folder)) {
            // No folder in model
            return;
        }
        Folder folder = (Folder) selection;
        if (SyncProfileSelectionBox.vetoableFolderSyncProfileChange(folder,
            syncProfile))
        {
            folder.setSyncProfile(syncProfile);
        }
    }

    /**
     * Marks the action as selected. Basically sets the icon
     * 
     * @param action
     */
    private void markAsSelected(boolean selected) {
        putValue(Action.SMALL_ICON, selected ? Icons.CHECKED : null);
    }
}