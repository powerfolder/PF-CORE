/* $Id: SyncFolderPanel.java,v 1.3 2006/01/23 00:36:25 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

/**
 * The Sync action panel. user can input his sync actions. e.g. scan. scan &
 * download
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class SyncFolderPanel extends BaseDialog {
    private static final Object SEND_OPTION = new Object();
    private static final Object SEND_RECEIVE_OPTION = new Object();
    private Folder folder;
    private JComponent sendChangesButton;
    private JComponent sendAndReceiveChangesButton;
    private ValueModel optionModel;

    public SyncFolderPanel(Controller controller, Folder folder) {
        // Modal dialog
        super(controller, true);
        this.folder = folder;
    }

    // Application logic ******************************************************

    /**
     * Performs the choosen sync options
     */
    private void performSync() {
        log().warn("Performing sync send");

        // Force scan on folder (=send)
        folder.forceNextScan();

        if (optionModel.getValue() == SEND_RECEIVE_OPTION) {
            log().warn("Performing receive");
            // Perform remote deltions
            folder.handleRemoteDeletedFiles(true);
            // Request ALL files now modified by friends
            // FIXME: This method requests all files from the first member !
            // Better strategy needed!!
            folder.requestMissingFiles(true, false, true);
        }
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("dialog.synchronization.title");
    }

    protected Icon getIcon() {
        return Icons.FILELIST;
    }

    protected Component getContent() {
        // Init
        initComponents();

        FormLayout layout = new FormLayout("pref",
            "pref, 10dlu, pref, 10dlu, pref, pref, 7dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        
        builder.addLabel(Translation.getTranslation("dialog.synchronization.choose"), cc.xy(1, 1));
        // Add iconed label
        JLabel folderLabel = builder.addLabel(folder.getName(), cc.xy(1, 3));
        folderLabel
            .setIcon(Icons.getIconFor(getController(), folder.getInfo()));

        builder.add(sendChangesButton, cc.xy(1, 5));
        builder.add(sendAndReceiveChangesButton, cc.xy(1, 6));

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        JButton okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performSync();
                // remove window
                close();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        // Default: Send
        optionModel = new ValueHolder(SEND_OPTION);

        sendChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, SEND_OPTION, Translation.getTranslation("dialog.synchronization.send_own_changes"));

        sendAndReceiveChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, SEND_RECEIVE_OPTION, Translation.getTranslation("dialog.synchronization.send_and_receive_changes"));
    }
}