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
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * The Sync action panel. user can input his sync actions. e.g. scan. scan &
 * download. Now used only if syncprofile is Project work
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class SyncFolderPanel extends BaseDialog {

    private static final Object SEND_OPTION = new Object();
    private static final Object RECEIVE_OPTION = new Object();
    private static final Object SEND_RECEIVE_OPTION = new Object();
    private Folder folder;
    private JComponent sendChangesButton;
    private JComponent receiveChangesButton;
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
        log().warn("Performing sync");
        SwingWorker worker = new SyncFolderWorker();
        worker.start();
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("dialog.synchronization.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected Component getContent() {
        // Init
        initComponents();

        FormLayout layout = new FormLayout("pref",
            "pref, 10dlu, pref, 10dlu, pref, pref, pref, 7dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("dialog.synchronization.choose"), cc.xy(1, 1));
        // Add iconed label
        JLabel folderLabel = builder.addLabel(folder.getName(), cc.xy(1, 3));
        folderLabel
            .setIcon(Icons.getIconFor(getController(), folder.getInfo()));

        builder.add(sendChangesButton, cc.xy(1, 5));
        builder.add(receiveChangesButton, cc.xy(1, 6));
        builder.add(sendAndReceiveChangesButton, cc.xy(1, 7));

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
        optionModel = new ValueHolder(SEND_RECEIVE_OPTION);

        sendChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, SEND_OPTION, Translation
                .getTranslation("dialog.synchronization.send_own_changes"));

        receiveChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, RECEIVE_OPTION, Translation
                .getTranslation("dialog.synchronization.receive_changes"));

        sendAndReceiveChangesButton = BasicComponentFactory
            .createRadioButton(
                optionModel,
                SEND_RECEIVE_OPTION,
                Translation
                    .getTranslation("dialog.synchronization.send_and_receive_changes"));
    }

    // Working classes ********************************************************

    private final class SyncFolderWorker extends ActivityVisualizationWorker {
        private SyncFolderWorker() {
            super(getUIController());
        }

        @Override
        protected String getTitle()
        {
            return Translation
                .getTranslation("dialog.synchronization.sychronizing");
        }

        @Override
        protected String getWorkingText()
        {
            return Translation
                .getTranslation("dialog.synchronization.sychronizing");
        }

        @Override
        public Object construct()
        {
            // Force scan on folder (=send)
            if (optionModel.getValue() == SEND_OPTION
                || optionModel.getValue() == SEND_RECEIVE_OPTION)
            {
                log().info(folder + ": Performing send/scan");
                folder.forceScanOnNextMaintenance();
                folder.maintain();
            }

            if (optionModel.getValue() == RECEIVE_OPTION
                || optionModel.getValue() == SEND_RECEIVE_OPTION)
            {
                log().info(folder + ": Performing receive");
                // Perform remote deltions
                folder.handleRemoteDeletedFiles(true);
                // Request ALL files now modified by friends
                getController().getFolderRepository().getFileRequestor()
                    .requestMissingFiles(folder, true, false, false);
            }

            return null;
        }
    }

}