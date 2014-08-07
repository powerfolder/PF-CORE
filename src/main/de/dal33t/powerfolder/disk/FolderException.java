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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General Exception for folder
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class FolderException extends Exception implements Serializable {

    private static final Logger log = Logger.getLogger(FolderException.class.getName());
    private static final long serialVersionUID = 100L;

    public FolderInfo fInfo;

    /**
     *
     */
    public FolderException() {
        super();
    }

    /**
     * @param message
     */
    public FolderException(FolderInfo folder, String message) {
        super(message);
        this.fInfo = folder;
    }

    /**
     * @param cause
     */
    public FolderException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public FolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMessage() {
        String prefix = "";
        if (fInfo != null) {
            prefix = "Folder '" + fInfo.getLocalizedName() + "': ";
        }
        return prefix + super.getMessage();
    }

    /**
     * Shows this error to the user if ui open
     *
     * @param controller
     */
    public void show(Controller controller) {
        show(controller, null);
    }

    /**
     * Shows this error to the user if ui open
     *
     * @param controller
     * @param additonalText
     *            the additional text which is displayed
     */
    public void show(final Controller controller, final String additonalText) {
        if (controller.isUIEnabled()) {
            Runnable runner = new Runnable() {
                public void run() {
                    JFrame parent = null;
                    if (controller.isUIOpen()) {
                    }
                    String addText = additonalText != null ? '\n'
                            + additonalText : "";
                    DialogFactory.genericDialog(controller,
                            Translation.getTranslation("folder_exception.dialog.title",
                                    fInfo == null ? "null" : fInfo.getLocalizedName()),
                            Translation.getTranslation("folder_exception.dialog.text",
                                    fInfo == null ? "null" : fInfo.getLocalizedName(),
                                    FolderException.super.getMessage()) + addText,
                            controller.isVerbose(), FolderException.this);
                }
            };

            if (EventQueue.isDispatchThread()) {
                // We are in event disp thread
                runner.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(runner);
                } catch (InterruptedException e) {
                    log.log(Level.SEVERE, "InterruptedException", e);
                } catch (InvocationTargetException e) {
                    log.log(Level.SEVERE, "InvocationTargetException", e);
                }
            }
        }
    }
}