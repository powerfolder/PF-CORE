/* $Id: UploadsQuickInfoPanel.java,v 1.2 2006/04/14 14:52:32 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about the uploads
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class UploadsQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    public UploadsQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     * 
     * @return
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory
            .createBiggerTextLabel(Translation.getTranslation("quickinfo.upload.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.TRANSFERS_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        int nUploads = getController().getTransferManager().countUploads();
        infoText1.setText(Translation.getTranslation("quickinfo.upload.active",
            nUploads));
    }

    // Overridden stuff *******************************************************

    @Override
    protected JComponent getPicto()
    {
        return picto;
    }

    @Override
    protected JComponent getHeaderText()
    {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1()
    {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2()
    {
        return infoText2;
    }

    // Core listeners *********************************************************

    /**
     * Listens to transfer manager
     * 
     * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
     */
    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
        }

        public void downloadQueued(TransferManagerEvent event) {
        }

        public void downloadStarted(TransferManagerEvent event) {
        }

        public void downloadAborted(TransferManagerEvent event) {
        }

        public void downloadBroken(TransferManagerEvent event) {
        }

        public void downloadCompleted(TransferManagerEvent event) {
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateText();
        }
    }
}
