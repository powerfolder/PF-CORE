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
package de.dal33t.powerfolder.ui.actionold;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action which will abort the selected transfer
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class AbortTransferAction extends SelectionBaseAction {

    public AbortTransferAction(Controller controller, SelectionModel selectionModel) {
        super("abort_transfer", controller, selectionModel);
        setEnabled(false);
    }

    
    public void selectionChanged(SelectionChangeEvent event) { 
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length != 0) {
            // check if all are files (cannot abort Dirs)
            setEnabled(true);
            for (int i = 0; i < selections.length; i++) {
                if (selections[i] instanceof FileInfo) {                    
                    FileInfo fileInfo = (FileInfo)selections[i];
                    DownloadManager dl = getController().getTransferManager().getActiveDownload(fileInfo);
                    if (dl == null) {
                        setEnabled(false);
                        break;
                    }
                } else {//dir so cannot abort dir
                    //TODO: or implement this later?
                    setEnabled(false);
                    break;
                }
            }
        }
    }
    
    


    public void actionPerformed(ActionEvent e) {
        Object[] selectedItems = getSelectionModel().getSelections();
        if (selectedItems.length <= 0) {
            return;
        }
        
        if (selectedItems[0] instanceof Transfer) {
            for (int i = 0; i < selectedItems.length; i++) {
                Transfer transfer = (Transfer) selectedItems[i];
                if (transfer instanceof Download) {
                    // Abort dl
//                    ((Download) transfer).abort();
                    ((Download) transfer).getDownloadManager().abort();
                }
            }
        }
        
        if (selectedItems[0] instanceof FileInfo) {
            TransferManager tm =  getController().getTransferManager();
            for (int i = 0; i < selectedItems.length; i++) {
                DownloadManager dl = tm.getActiveDownload((FileInfo) selectedItems[i]);
                if (dl != null) {
                    // Abort dl
                    dl.abort();
                }
            }
        }
    }
}