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
import de.dal33t.powerfolder.ui.action.PreviewFolderRemoveAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel displayed when wanting to remove a preview folder
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.00 $
 */
public class PreviewFolderRemovePanel extends BaseDialog {

    private final PreviewFolderRemoveAction action;
    private final Folder folder;

    private JButton okButton;
    private JButton cancelButton;
    private JLabel messageLabel;
    private JCheckBox cbDeleteSystemSubFolder;
    private JCheckBox removeFromServerBox;

    /**
     * Contructor when used on choosen folder
     * 
     * @param action
     * @param controller
     * @param foInfo
     */
    public PreviewFolderRemovePanel(PreviewFolderRemoveAction action,
        Controller controller, Folder folder)
    {
        super(controller, true);
        this.action = action;
        this.folder = folder;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        String folerLeaveText = Translation.getTranslation(
            "preview_folder_remove.dialog.text", folder.getInfo().name);
        messageLabel = new JLabel(folerLeaveText);

        cbDeleteSystemSubFolder = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preview_folder_remove.dialog.delete"));

        removeFromServerBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("folder_remove.dialog.remove_from_os"));
        removeFromServerBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getUIController().getOnlineStorageClientModel()
                    .checkAndSetupAccount();
            }
        });

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                action.confirmedFolderLeave(cbDeleteSystemSubFolder
                    .isSelected(), removeFromServerBox.isSelected());
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
        return Translation.getTranslation("preview_folder_remove.dialog.title",
            folder.getInfo().name);
    }

    protected Icon getIcon() {
        return Icons.REMOVE_FOLDER;
    }

    protected Component getContent() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow",
            "pref, 7dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.add(messageLabel, cc.xy(1, 1));

        builder.add(cbDeleteSystemSubFolder, cc.xy(1, 3));

        boolean showRemoveFromServer = !getController().isLanOnly()
            && getController().getOSClient().hasJoined(folder);
        if (showRemoveFromServer) {
            builder.add(removeFromServerBox, cc.xy(1, 5));
        }

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

}