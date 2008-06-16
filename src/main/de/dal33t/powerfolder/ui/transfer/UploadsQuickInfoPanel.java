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
package de.dal33t.powerfolder.ui.transfer;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about the uploads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
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
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.upload.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.UPLOAD_PICTO);

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
        int nCompletedUls = getController().getTransferManager()
                .countAllUploads();

        String text1 = Translation.getTranslation("quickinfo.upload.completed",
                nCompletedUls);
        infoText1.setText(text1);

        int nActiveUls = getController().getTransferManager()
                .countLiveUploads();
        String text2 = Translation.getTranslation("quickinfo.upload.active",
            nActiveUls);

        infoText2.setText(text2);
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
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyTransferManagerListener extends TransferAdapter {

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

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
