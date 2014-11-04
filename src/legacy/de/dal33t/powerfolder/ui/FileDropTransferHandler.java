/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: FileDropTransferHandler.java 11762 2010-03-18 00:32:52Z tot $
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.InvitationUtil;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handler to accept folder drops, opening folder wizard.
 */
public class FileDropTransferHandler extends TransferHandler {

    private static final Logger log = Logger.getLogger(FileDropTransferHandler.class.getName());
    private Controller controller;

    public FileDropTransferHandler(Controller controller) {
        this.controller = controller;
    }

    /**
     * Whether this drop can be imported; must be file list flavor.
     *
     * @param support
     * @return
     */
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    /**
     * Import the file. Only import if it is a single directory.
     *
     * @param support
     * @return
     */
    public boolean importData(TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }

        final File file = getFileList(support);
        if (file == null) {
            return false;
        }

        // Run later, so do not tie up OS drag and drop process.
        Runnable runner = new Runnable() {
            public void run() {
                if (file.isDirectory()) {
                    PFWizard.openExistingDirectoryWizard(controller,
                            file);
                } else if (file.getName().endsWith(".invitation")) {
                    Invitation invitation = InvitationUtil.load(file);
                    PFWizard.openInvitationReceivedWizard(controller,
                            invitation);
                }
            }
        };
        SwingUtilities.invokeLater(runner);

        return true;
    }

    /**
     * Get the directory to import. The transfer is a list of files; need to
     * check the list has one directory, else return null.
     *
     * @param support
     * @return
     */
    private static File getFileList(TransferSupport support) {
        Transferable t = support.getTransferable();
        try {
            List list = (List) t
                    .getTransferData(DataFlavor.javaFileListFlavor);
            if (list.size() == 1) {
                for (Object o : list) {
                    if (o instanceof File) {
                        File file = (File) o;
                        if (file.isDirectory()) {
                            return file;
                        } else if (file.getName().endsWith(".invitation")) {
                            return file;
                        }
                    }
                }
            }
        } catch (UnsupportedFlavorException e) {
            log.log(Level.SEVERE, "UnsupportedFlavorException", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException", e);
        }
        return null;
    }
}
