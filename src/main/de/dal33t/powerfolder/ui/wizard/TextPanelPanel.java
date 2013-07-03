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
 * $Id: TextPanelPanel.java 21202 2013-03-18 01:37:47Z sprajc $
 */
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_IS_INVITE;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProUtil;

/**
 * A general text panel, displays the given text and offers to finish wizard
 * process. No next panel
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class TextPanelPanel extends PFWizardPanel {

    private boolean autoFadeOut;
    private String title;
    private String text;

    public TextPanelPanel(Controller controller, String title, String text) {
        this(controller, title, text, false);
    }

    public TextPanelPanel(Controller controller, String title, String text,
                          boolean autoFadeOut) {
        super(controller);
        this.title = title;
        this.text = text;
        this.autoFadeOut = autoFadeOut;
    }

    public boolean hasNext() {
        return false;
    }

    @Override
    protected void afterDisplay() {
        if (PFWizard.hideFolderJoinWizard(getController())) {
            JDialog diag = getWizardDialog();
            diag.setVisible(false);
            diag.dispose();
        }
        if (autoFadeOut) {
            new FadeOutWorker().execute();
        }

        // If it's an invite, try to display it in the UI.
        Object o = getWizardContext().getAttribute(FOLDER_IS_INVITE);
        if (o != null && o instanceof Boolean && (Boolean) o) {
            Object p = getWizardContext().getAttribute(FOLDERINFO_ATTRIBUTE);
            if (p != null && p instanceof FolderInfo) {
                getController().getUIController().displayInviteFolderContents(
                        (FolderInfo) p);
            }
        }
    }

    public WizardPanel next() {
        return null;
    }

    public boolean canFinish() {
        return true;
    }

    public boolean canCancel() {
        return false;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        // Add text as labels
        StringTokenizer nizer = new StringTokenizer(text, "\n");
        int y = 1;
        boolean firstAddition = true;
        while (nizer.hasMoreTokens()) {
            String line = nizer.nextToken();
            if (firstAddition) {
                // Nothing to add. We already have the first line.
                firstAddition = false;
            } else {
                builder.appendRow("pref");
                y++;
            }

            builder.addLabel(line, cc.xy(1, y));
        }

        // If it is a locally synced folder,
        // show link to open the folder in explorer.
        if (!autoFadeOut) {

            FolderInfo folderInfo = (FolderInfo) getWizardContext()
                    .getAttribute(FOLDERINFO_ATTRIBUTE);
            if (folderInfo != null) {
                Folder folder = getController().getFolderRepository()
                        .getFolder(folderInfo);
                if (folder != null) {
                    builder.appendRow("3dlu");
                    y++;
                    builder.appendRow("pref");
                    y++;
                    Action action = new OpenFolderAction(getController(), folderInfo);
                    builder.add(new ActionLabel(getController(), action)
                            .getUIComponent(), cc.xy(1, y));
                    builder.appendRow("3dlu");
                    y++;
                    builder.appendRow("pref");
                    y++;
                    action = new SendInviteAction(getController(), folderInfo);
                    builder.add(new ActionLabel(getController(), action)
                            .getUIComponent(), cc.xy(1, y));
                }
            }
        }

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
    }

    protected String getTitle() {
        return title;
    }

    private JDialog getWizardDialog() {
        return (JDialog) getWizardContext().getAttribute(
                WizardContextAttributes.DIALOG_ATTRIBUTE);
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class FadeOutWorker extends SwingWorker<Void, Integer> {

        @Override
        protected void process(List<Integer> chunks) {
            if (!getWizardDialog().isVisible()) {
                return;
            }
            // Translucency is 1 - opacity.
            float opacity = 1.0f - chunks.get(0) / 100.0f;
            UIUtil.applyTranslucency(getWizardDialog(), opacity);
        }

        @Override
        protected Void doInBackground() throws Exception {
            Thread.sleep(1000);
            if (!Constants.OPACITY_SUPPORTED) {
                Thread.sleep(1000);
                return null;
            }
            for (int i = 0; i < 100; i++) {
                publish(i);
                Thread.sleep(10);
            }
            publish(100);
            return null;
        }

        @Override
        protected void done() {
            JDialog diag = getWizardDialog();
            diag.setVisible(false);
            diag.dispose();
        }

    }

    private static class OpenFolderAction extends BaseAction {

        private final FolderInfo folderInfo;

        private OpenFolderAction(Controller controller, FolderInfo folderInfo) {
            super("action_open_folder", controller);
            this.folderInfo = folderInfo;
        }

        public void actionPerformed(ActionEvent e) {
            Folder folder = folderInfo.getFolder(getController());
            PathUtils.openFile(folder.getLocalBase());
        }
    }

    private class SendInviteAction extends BaseAction {

        private final FolderInfo folderInfo;

        private SendInviteAction(Controller controller, FolderInfo folderInfo) {
            super("action_invite_friend", controller);
            this.folderInfo = folderInfo;
        }

        public void actionPerformed(ActionEvent e) {

            // Close this dialog.
            JDialog diag = getWizardDialog();
            diag.setVisible(false);
            diag.dispose();

            // Open the wizard to send an invitation.
            PFWizard.openSendInvitationWizard(getController(), folderInfo);
        }
    }

}