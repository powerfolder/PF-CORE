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
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;
import java.util.logging.Logger;

/**
 * Wizard for sending an invitation to a user for a selected folder.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.12 $
 */
public class SelectInvitationPanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(SelectInvitationPanel.class.getName());

    private List<FolderInfo> possibleFolders;
    private Member member;
    private JComboBox foldersCombo;
    private JTextField messageField;

    public SelectInvitationPanel(Controller controller,
                                 Member member,
                                 List<FolderInfo> possibleFolders) {
        super(controller);
        this.possibleFolders = possibleFolders;
        this.member = member;
    }

    public boolean hasNext() {
        return true;
    }

    public boolean validateNext(List list) {
        int index = foldersCombo.getSelectedIndex();
        FolderInfo folderInfo = possibleFolders.get(index);
        Invitation invitation = folderInfo.getFolder(getController())
                .createInvitation();
        invitation.setSuggestedLocalBase(getController(),
                folderInfo.getFolder(getController()).getLocalBase());
        invitation.setInvitationText(messageField.getText());
        InvitationUtil.invitationToNode(getController(), invitation, member);
        log.finer("Invited " + member.getNick() + " to folder "
                + folderInfo.name);
        return true;
    }

    public WizardPanel next() {
        // Show success panel
        return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
                "pref, max(pref;140dlu)",
                "pref, 5dlu, pref, 10dlu, pref, 5dlu, pref, 10dlu, pref, 5dlu, " +
                        "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation.getTranslation("select_invitation.text1"),
                cc.xyw(1, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation("select_invitation.text2"),
                cc.xyw(1, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation("select_invitation.message_text"),
                cc.xyw(1, row, 2));

        row += 2;
        builder.add(messageField, cc.xy(1, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("select_invitation.folder_text"),
                cc.xyw(1, row, 2));

        row += 2;
        builder.add(foldersCombo, cc.xy(1, row));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        // Clear folder attribute
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        foldersCombo = new JComboBox();
        for (FolderInfo possibleFolder : possibleFolders) {
            foldersCombo.addItem(possibleFolder.name);
        }

        messageField = new JTextField();
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return Translation
                .getTranslation("wizard.send_invitations.send_invitation");
    }
}