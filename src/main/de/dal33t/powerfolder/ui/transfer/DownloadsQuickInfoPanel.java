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

import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Show concentrated information about the downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.3 $
 */
public class DownloadsQuickInfoPanel extends QuickInfoPanel {

    private final ValueModel completedDownloadsCountVM;
    private final ValueModel activeDownloadsCountVM;

    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    protected DownloadsQuickInfoPanel(Controller controller) {
        super(controller);

        // Begin listening for changes to uploads from the transfer manager
        // model.
        completedDownloadsCountVM = getApplicationModel()
            .getTransferManagerModel().getCompletedDownloadsCountVM();
        completedDownloadsCountVM
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    // Ensure the parent table updates on changes.
                    updateText();
                }
            });

        activeDownloadsCountVM = getApplicationModel()
            .getTransferManagerModel().getActiveDownloadsCountVM();
        activeDownloadsCountVM
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    // Ensure the parent table updates on changes.
                    updateText();
                }
            });

    }

    /**
     * Initalizes the components
     * 
     * @return
     */
    @Override
    protected void initComponents() {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.download.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.DOWNLOAD_PICTO);

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
        if (picto == null) {
            initComponents();
        }
        Object value = completedDownloadsCountVM.getValue();
        int nCompletedDls = value == null ? 0 : (Integer) value;

        String text1 = Translation.getTranslation(
            "quickinfo.download.completed", nCompletedDls);
        infoText1.setText(text1);

        value = activeDownloadsCountVM.getValue();
        int nActiveDls = value == null ? 0 : (Integer) value;
        String text2 = Translation.getTranslation("quickinfo.download.active",
            nActiveDls);

        infoText2.setText(text2);
    }

    // Overridden stuff *******************************************************

    @Override
    protected JComponent getPicto() {
        return picto;
    }

    @Override
    protected JComponent getHeaderText() {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1() {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2() {
        return infoText2;
    }

    // Core listeners *********************************************************

    /**
     * Listens to transfer manager
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyTransferManagerListener extends TransferAdapter {

        public void downloadRequested(TransferManagerEvent event) {
            updateText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateText();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
