/* $Id: FolderJoinPanel.java,v 1.30 2006/02/28 16:44:33 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.FolderCreateWorker;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

/**
 * Panel displayed when wanting to join a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class FolderJoinPanel extends BaseDialog {
    // the folder
    private FolderInfo foInfo;
    private MemberInfo from;
    private SyncProfile suggestedSyncProfile;

    private JButton okButton;
    private JButton cancelButton;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JComponent baseDirSelectionField;
    private JCheckBox addToFriendBox;
    private JCheckBox cbCreateShortcut;
    private JTextField invitationField;

    private ValueModel baseDirModel;
    private String message;
    private String invitationText;

    /**
     * Contructor when used on choosen folder
     * 
     * @param controller
     * @param foInfo
     */
    public FolderJoinPanel(Controller controller, FolderInfo foInfo) {
        super(controller, true);
        this.foInfo = foInfo;
        this.message = Translation.getTranslation("folderjoin.dialog.title",
            foInfo.name);
    }

    /**
     * Used when a invitation should be processed
     * 
     * @param controller
     * @param invitation
     *            the invitation
     * @param from
     *            the source of the invitation
     */
    public FolderJoinPanel(Controller controller, Invitation invitation,
        MemberInfo from)
    {
        super(controller, true);
        this.foInfo = invitation.folder;
        this.from = from;
        this.suggestedSyncProfile = invitation.suggestedProfile;
        this.invitationText = invitation.invitationText;
        if (from != null) {
            this.message = Translation.getTranslation(
                "folderjoin.inivtationfrom", from.nick);
        } else {
            this.message = Translation.getTranslation("folderjoin.inivtation");
        }
    }

    // Application logic ******************************************************

    /**
     * @return if user wants to add invitor to friends
     */
    public boolean addInvitorToFriendsRequested() {
        return addToFriendBox.isSelected();
    }

    /**
     * Joins the folder, usually on OK
     */
    private void startJoinFolder() {
        // Selected local base
        File localBase = new File((String) baseDirModel.getValue());
        // The sync profile
        SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();

        // Default to the general propery for recycle bin use.
        boolean useRecycleBin = ConfigurationEntry.USE_RECYCLE_BIN
            .getValueBoolean(getController());

        MyFolderJoinWorker folderJoinerWorker = new MyFolderJoinWorker(
            getController(), foInfo, localBase, syncProfile, false,
            cbCreateShortcut.isSelected(), useRecycleBin);
        setVisible(false);
        folderJoinerWorker.start();
    }

    /**
     * Analyses the information about a folder an recommends a synchronsiation
     * profile
     * 
     * @return an recommends a synchronsiation profile
     */
    private SyncProfile getRecommendedSyncProfile() {
        // New versions of PowerFolder will support suggested profiles.
        if (suggestedSyncProfile != null) {
            return suggestedSyncProfile;
        }
        return SyncProfile.AUTO_DOWNLOAD_FROM_ALL;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        // Calculate recommended sync profile
        SyncProfile recommendedSyncProfile = getRecommendedSyncProfile();
        log().verbose(
            "Recommended sync profile for " + foInfo + ": "
                + recommendedSyncProfile);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(
            getController(), recommendedSyncProfile);

        syncProfileSelectorPanel
            .addModelValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!SyncProfileSelectorPanel
                        .vetoableFolderSyncProfileChange(null,
                            (SyncProfile) evt.getNewValue()))
                    {
                        syncProfileSelectorPanel.setSyncProfile(
                            (SyncProfile) evt.getOldValue(), false);
                    }
                }
            });

        // Base dir selection
        baseDirModel = new ValueHolder();
        baseDirSelectionField = ComplexComponentFactory
            .createFolderBaseDirSelectionField(new ValueHolder(foInfo.name),
                baseDirModel, getController());

        // Invitor add to friends checkbox
        addToFriendBox = SimpleComponentFactory.createCheckBox();
        if (from != null) {
            addToFriendBox.setText('('
                + Translation.getTranslation("general.user") + ' ' + from.nick
                + ')');
        }

        invitationField = new JTextField();
        if (invitationText != null) {
            invitationField.setText(invitationText);
        }
        invitationField.setEditable(false);

        cbCreateShortcut = SimpleComponentFactory.createCheckBox();
        cbCreateShortcut.setEnabled(getUIController()
            .getFolderCreateShortcutAction().getValue(
                CreateShortcutAction.SUPPORTED) == Boolean.TRUE);
        // Default to "create shortcut" if not disabled
        cbCreateShortcut.setSelected(cbCreateShortcut.isEnabled());

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                startJoinFolder();
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("folderjoin.dialog.title",
            foInfo.name);
    }

    protected Icon getIcon() {
        return Icons.JOIN_FOLDER;
    }

    protected Component getContent() {
        int row;
        initComponents();

        FormLayout layout = new FormLayout(
            "right:pref, 7dlu, max(120dlu;pref):grow",
            "pref, 7dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 5dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        // message ontop of dialog
        builder.addLabel(message, cc.xywh(1, 1, 3, 1));

        row = 3;

        builder.addLabel(Translation.getTranslation(
            "folderjoin.invitationtext", from != null ? from.nick : ""), cc.xy(
            1, row));
        builder.add(invitationField, cc.xy(3, row));

        row += 2;

        String type = foInfo.secret ? Translation
            .getTranslation("folderjoin.secret") : Translation
            .getTranslation("folderjoin.public");
        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(1,
            row));
        builder.addLabel(foInfo.name + " (" + type + ")", cc.xy(3, row));

        row += 2;

        builder.addLabel(Translation.getTranslation("general.estimatedsize"),
            cc.xy(1, row));
        builder.addLabel(Format.formatBytes(foInfo.bytesTotal) + " ("
            + foInfo.filesCount + " "
            + Translation.getTranslation("general.files") + ")", cc.xy(3, row));

        row += 2;

        builder.addLabel(Translation.getTranslation("general.synchonisation"),
            cc.xy(1, row));
        builder.add(syncProfileSelectorPanel.getUIComponent(), cc.xy(3, row));

        row += 2;

        builder.addLabel(Translation.getTranslation("general.localcopyat"), cc
            .xy(1, row));
        builder.add(baseDirSelectionField, cc.xy(3, row));

        row += 2;

        if (from != null && !from.isFriend(getController())) {
            builder.addLabel(Translation
                .getTranslation("folderjoin.invitortofriend"), cc.xy(1, row));
            builder.add(addToFriendBox, cc.xy(3, row));

            row += 2;
        }

        builder.addLabel((String) getUIController()
            .getFolderCreateShortcutAction().getValue(Action.NAME), cc.xy(1,
            row));
        builder.add(cbCreateShortcut, cc.xy(3, row));

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    // Creation worker ********************************************************

    /**
     * Worker to create the folder in the background and shows activity
     * visualization. It is highly recommended to read the javadocs from
     * <code>FolderCreateWorker</code>.
     * 
     * @see FolderCreateWorker
     */
    private class MyFolderJoinWorker extends FolderCreateWorker {

        public MyFolderJoinWorker(Controller theController, FolderInfo aFoInfo,
            File aLocalBase, SyncProfile aProfile, boolean storeInv,
            boolean createShortcut, boolean useRecycleBin)
        {
            super(theController, aFoInfo, aLocalBase, aProfile, storeInv,
                createShortcut, useRecycleBin);
        }

        @Override
        public void finished() {
            if (getFolderException() != null) {
                setVisible(true);
                // Show error
                getFolderException().show(getController());
                okButton.setEnabled(true);
            } else {
                close();

                // Display joined folder
                getUIController().getControlQuarter().setSelected(getFolder());
            }
        }
    }
}