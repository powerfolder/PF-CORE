package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import static de.dal33t.powerfolder.disk.Folder.THUMBS_DB;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import jwf.Wizard;
import jwf.WizardPanel;

import javax.swing.*;
import java.io.File;

/**
 * A panel that actually starts the creation process of a folder on display.
 * Automatically switches to the next panel when succeeded otherwise prints
 * error.
 * <p>
 * Extracts the settings for the folder from the
 * <code>WizardContextAttributes</code>.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderCreatePanel extends PFWizardPanel {

    private FolderInfo foInfo;
    private boolean backupByOS;
    private boolean sendInvitations;
    private boolean createShortcut;
    private FolderSettings folderSettings;

    private Folder folder;
    private FolderException exception;

    private JLabel statusLabel;
    private JTextArea errorArea;
    private JComponent errorPane;
    private JProgressBar bar;

    public FolderCreatePanel(Controller controller) {
        super(controller);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("pref, 5dlu, pref",
            "pref, 5dlu, pref, 5dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;

        statusLabel = builder.addLabel(Translation
            .getTranslation("wizard.create_folder.working"), cc.xy(1, row));

        row += 2;
        bar = new JProgressBar();
        bar.setIndeterminate(true);
        builder.add(bar, cc.xy(1, row));

        errorArea = new JTextArea();
        errorArea.setRows(5);
        errorArea.setWrapStyleWord(true);
        errorPane = new JScrollPane(errorArea);
        builder.add(errorPane, cc.xy(1, row));
        return builder.getPanel();
    }

    @Override
    protected void afterDisplay() {

        // Mandatory
        File localBase = (File) getWizardContext().getAttribute(
            WizardContextAttributes.FOLDER_LOCAL_BASE);
        SyncProfile syncProfile = (SyncProfile) getWizardContext()
            .getAttribute(WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE);
        Reject.ifNull(localBase, "Local base for folder is null/not set");
        Reject.ifNull(syncProfile, "Sync profile for folder is null/not set");

        // Optional
        foInfo = (FolderInfo) getWizardContext().getAttribute(
            WizardContextAttributes.FOLDERINFO_ATTRIBUTE);
        if (foInfo == null) {
            // Create new folder info
            String name = getController().getMySelf().getNick() + '-'
                + localBase.getName();

            String folderId = '[' + IdGenerator.makeId() + ']';
            foInfo = new FolderInfo(name, folderId);
            getWizardContext().setAttribute(
                WizardContextAttributes.FOLDERINFO_ATTRIBUTE, foInfo);
        }
        Boolean prevAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE);
        boolean previewFolder = prevAtt != null && prevAtt;
        boolean useRecycleBin = ConfigurationEntry.USE_RECYCLE_BIN
            .getValueBoolean(getController());
        createShortcut = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.CREATE_DESKTOP_SHORTCUT);
        Boolean osAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE);
        backupByOS = osAtt != null && osAtt;
        // Send invitation after by default.
        Boolean sendInvsAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE);
        sendInvitations = sendInvsAtt == null || sendInvsAtt;

        folderSettings = new FolderSettings(localBase, syncProfile,
            useRecycleBin, true, previewFolder, false);

        // Reset
        folder = null;
        exception = null;

        SwingWorker worker = new MyFolderCreateWorker();
        bar.setVisible(true);
        errorPane.setVisible(false);
        worker.start();

        updateButtons();
    }

    protected void initComponents() {
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.create_folder.title");
    }

    @Override
    public boolean hasNext() {
        return folder != null;
    }

    @Override
    public WizardPanel next() {
        WizardPanel next;
        if (sendInvitations) {
            next = new SendInvitationsPanel(getController(), true);
        } else {
            next = (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        }
        return next;
    }

    private class MyFolderCreateWorker extends SwingWorker {

        @Override
        public Object construct() {
            try {
                folder = getController().getFolderRepository().createFolder(
                    foInfo, folderSettings);
                if (createShortcut) {
                    folder.setDesktopShortcut(true);
                }
                if (OSUtil.isWindowsSystem()) {
                    // Add thumbs to ignore pattern on windows systems
                    // Don't duplicate thumbs (like when moving a preview
                    // folder)
                    if (!folder.getBlacklist().getPatterns()
                        .contains(THUMBS_DB))
                    {
                        folder.getBlacklist().addPattern(THUMBS_DB);
                    }

                    // Add desktop.ini to ignore pattern on windows systems
                    if (!OSUtil.isWindowsVistaSystem()
                        && ConfigurationEntry.USE_PF_ICON
                            .getValueBoolean(getController()))
                    {
                        folder.getBlacklist().addPattern(
                            FileUtils.DESKTOP_INI_FILENAME);
                    }
                }
                if (backupByOS && getController().getOSClient().isLastLoginOK())
                {
                    try {
                        getController().getOSClient().getFolderService()
                            .createFolder(folder.getInfo(),
                                SyncProfile.BACKUP_TARGET);
                    } catch (FolderException e) {
                        errorArea
                            .setText(Translation
                                .getTranslation("foldercreate.dialog.backuperror.text"));
                        errorPane.setVisible(true);
                        log().error(
                            "Unable to backup folder to online storage", e);
                    }
                }
            } catch (FolderException ex) {
                exception = ex;
                log().error("Unable to create new folder " + foInfo, ex);
            }
            return null;
        }

        @Override
        public void finished() {
            bar.setVisible(false);

            if (folder != null) {
                if (SyncProfile.PROJECT_WORK.equals(folder.getSyncProfile())) {
                    // Show sync folder panel after created a project folder
                    new SyncFolderPanel(getController(), folder).open();
                }

                Wizard wiz = (Wizard) getWizardContext().getAttribute(
                    Wizard.WIZARD_ATTRIBUTE);
                wiz.next();
            } else {
                updateButtons();

                statusLabel.setText(Translation
                    .getTranslation("wizard.create_folder.failed"));
                String details = exception != null
                    ? exception.getMessage()
                    : "";
                errorArea.setText(details);
                errorPane.setVisible(true);
            }
        }
    }

}
