/* $Id: FolderJoinPanel.java,v 1.30 2006/02/28 16:44:33 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.ui.action.FolderLeaveAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel displayed when wanting to leave a folder
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.00 $
 */
public class FolderLeavePanel extends BaseDialog {

    private final FolderLeaveAction action;
    private final Folder folder;

    private JButton okButton;
    private JButton cancelButton;
    private JLabel messageLabel;
    private JCheckBox cbDeleteSystemSubFolder;

    /**
     * Contructor when used on choosen folder
     *
     * @param action
     * @param controller
     * @param foInfo
     */
    public FolderLeavePanel(FolderLeaveAction action, Controller controller, Folder folder) {
        super(controller, true);
        this.action = action;
        this.folder = folder;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        // Create folderleave dialog message
        boolean syncFlag = folder.isSynchronizing();
        String folerLeaveText;
        if (syncFlag) {
            folerLeaveText = Translation.getTranslation(
                    "folderleave.dialog.text", folder.getInfo().name)
                    + '\n'
                    + Translation
                    .getTranslation("folderleave.dialog.sync_warning");
        } else {
            folerLeaveText = Translation.getTranslation(
                    "folderleave.dialog.text", folder.getInfo().name);
        }
        messageLabel = new JLabel(folerLeaveText);

        cbDeleteSystemSubFolder = SimpleComponentFactory.createCheckBox();

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                action.confirmedFolderLeave(cbDeleteSystemSubFolder.isSelected());
                close();
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
        return Translation.getTranslation(
                    "folderleave.dialog.title", folder.getInfo().name);
    }

    protected Icon getIcon() {
        return Icons.LEAVE_FOLDER;
    }

    protected Component getContent() {
        initComponents();

        FormLayout layout = new FormLayout(
            "pref:grow, 7dlu, pref:grow",
            "pref, 7dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.add(messageLabel, cc.xyw(1, 1, 3));

        builder.addLabel(Translation.getTranslation(
                    "folderleave.dialog.delete"), cc.xy(1, 3));
        builder.add(cbDeleteSystemSubFolder, cc.xy(3, 3));

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

}