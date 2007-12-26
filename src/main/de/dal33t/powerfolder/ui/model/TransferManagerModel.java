package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.transfer.DownloadsTableModel;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;

public class TransferManagerModel extends PFUIComponent {
    private DownloadsTableModel downloadsTableModel;
    private ValueModel downloadsAutoCleanupModel;
    private UploadsTableModel uploadsTableModel;

    public TransferManagerModel(TransferManager transferManager) {
        super(transferManager.getController());
        downloadsAutoCleanupModel = new ValueHolder();
        downloadsAutoCleanupModel
            .setValue(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                .getValueBoolean(getController()));
        downloadsTableModel = new DownloadsTableModel(this);

        uploadsTableModel = new UploadsTableModel(this, true);
    }

    // Exposing ***************************************************************

    public TransferManager getTransferManager() {
        return getController().getTransferManager();
    }

    public DownloadsTableModel getDownloadsTableModel() {
        return downloadsTableModel;
    }

    public ValueModel getDownloadsAutoCleanupModel() {
        return downloadsAutoCleanupModel;
    }

    public UploadsTableModel getUploadsTableModel() {
        return uploadsTableModel;
    }
}
