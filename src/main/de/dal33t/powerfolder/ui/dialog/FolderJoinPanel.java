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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.PFListCellRenderer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.FolderCreateWorker;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;

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

    private JButton okButton;
    private JButton cancelButton;
    private SyncProfileSelectionBox profileBox;
    private JComponent baseDirSelectionField;
    private JCheckBox addToFriendBox;
    private JCheckBox cbCreateShortcut;

    private ValueModel baseDirModel;
    private String message;

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

        if (from != null) {
            this.message = Translation.getTranslation(
                "folderjoin.inivtationfrom", from.nick);
        } else {
            this.message = Translation.getTranslation("folderjoin.inivtation");
        }
    }

    // Application logic ******************************************************

    /**
     * Answers if user wants to add invitor to friends
     * 
     * @return
     */
    public boolean addInvitorToFriendsRequested() {
        return addToFriendBox.isSelected();
    }

    /**
     * Joins the folder, usually on OK
     * 
     * @param true
     *            if succeeded
     */
    private void startJoinFolder() {
        // Selected local base
        File localBase = new File((String) baseDirModel.getValue());
        // The sync profile
        SyncProfile syncProfile = profileBox.getSelectedSyncProfile();

        MyFolderJoinWorker folderJoinerWorker = new MyFolderJoinWorker(
            getController(), foInfo, localBase, syncProfile, false,
            cbCreateShortcut.isSelected());
        folderJoinerWorker.start();
    }

    /**
     * Analyses the information about a folder an recommends a synchronsiation
     * profile
     * 
     * @return
     */
    private SyncProfile getRecommendedSyncProfile() {
        Member source = getController().getFolderRepository().getSourceFor(
            foInfo, true);
        if (source == null) {
            return SyncProfile.AUTO_DOWNLOAD_FROM_ALL;
        }
        FileInfo[] filelist = source.getLastFileList(foInfo);
        if (filelist == null || filelist.length == 0) {
            return SyncProfile.AUTO_DOWNLOAD_FROM_ALL;
        }

        int friendFiles = 0;
        int foreignFiles = 0;
        int torrentFiles = 0;
        for (int i = 0; i < filelist.length; i++) {
            FileInfo file = filelist[i];
            if (file.isDeleted()) {
                continue;
            }
            if (file.isModifiedByFriend(getController())) {
                friendFiles++;
            } else {
                foreignFiles++;
            }
            if (file.getFilenameOnly().endsWith(".torrent")) {
                torrentFiles++;
            }
        }

        // If more than 75% are torrent files, recommend leecher profile
        if (torrentFiles > filelist.length * 0.75) {
            return SyncProfile.LEECHER;
        }

        // If there are more file modified by friend, recommend auto-dl from
        // friends
        if (friendFiles > foreignFiles) {
            return SyncProfile.AUTO_DOWNLOAD_FROM_FRIENDS;
        }

        return SyncProfile.AUTO_DOWNLOAD_FROM_ALL;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        profileBox = new SyncProfileSelectionBox();
        profileBox.setRenderer(new PFListCellRenderer());

        // Calculate recommended sync profile
        SyncProfile recommendedSyncProfile = getRecommendedSyncProfile();
        log().verbose(
            "Recommended sync profile for " + foInfo + ": "
                + recommendedSyncProfile);
        profileBox.setSelectedSyncProfile(recommendedSyncProfile);

        profileBox.getSyncProfileModel().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent arg0) {
                    if (!SyncProfileSelectionBox
                        .vetoableFolderSyncProfileChange(null,
                            (SyncProfile) arg0.getNewValue()))
                    {
                        profileBox.setSelectedItem(arg0.getOldValue());
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
            addToFriendBox.setText("("
                + Translation.getTranslation("general.user") + " " + from.nick
                + ")");
        }

        cbCreateShortcut = SimpleComponentFactory.createCheckBox();
        // Default to "create shortcut"
        cbCreateShortcut.setSelected(true);
        
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
            "pref, 7dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 5dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        // message ontop of dialog
        builder.addLabel(message, cc.xywh(1, 1, 3, 1));

        String type = foInfo.secret ? Translation
            .getTranslation("folderjoin.secret") : Translation
            .getTranslation("folderjoin.public");
        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(1,
            3));
        builder.addLabel(foInfo.name + " (" + type + ")", cc.xy(3, 3));

        builder.addLabel(Translation.getTranslation("general.estimatedsize"),
            cc.xy(1, 5));
        builder.addLabel(Format.formatBytes(foInfo.bytesTotal) + " ("
            + foInfo.filesCount + " "
            + Translation.getTranslation("general.files") + ")", cc.xy(3, 5));

        builder.addLabel(Translation.getTranslation("general.synchonisation"),
            cc.xy(1, 7));
        builder.add(Help.addHelpLabel(profileBox), cc.xy(3, 7));

        builder.addLabel(Translation.getTranslation("general.localcopyat"), cc
            .xy(1, 9));
        builder.add(baseDirSelectionField, cc.xy(3, 9));

        row = 11;
        
        if (from != null && !from.isFriend(getController())) {
            builder.addLabel(Translation
                .getTranslation("folderjoin.invitortofriend"), cc.xy(1, row));
            builder.add(addToFriendBox, cc.xy(3, row));
            
            row += 2;
        }
        
        builder.addLabel(Translation
            .getTranslation((String) getUIController().getFolderCreateShortcutAction()
                .getValue(Action.NAME)), cc.xy(1, row));
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

        public MyFolderJoinWorker(Controller theController,
            FolderInfo aFoInfo, File aLocalBase, SyncProfile aProfile,
            boolean storeInv, boolean createShortcut)
        {
            super(theController, aFoInfo, aLocalBase, aProfile, storeInv, createShortcut);
        }

        @Override
        public void finished()
        {
            if (getFolderException() != null) {
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