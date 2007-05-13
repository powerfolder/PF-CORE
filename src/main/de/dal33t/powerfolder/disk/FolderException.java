/* $Id: FolderException.java,v 1.5 2005/05/03 03:38:06 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.awt.EventQueue;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * General Exception for folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class FolderException extends Exception implements Serializable {
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
            prefix = "Folder '" + fInfo.name + "': ";
        }
        return prefix + super.getMessage();
    }

    /**
     * Shows this error to the user if ui open
     * 
     * @param controller
     */
    public void show(final Controller controller) {
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
                        parent = controller.getUIController().getMainFrame()
                            .getUIComponent();
                    }
                    String addText = additonalText != null ? "\n"
                        + additonalText : "";
                    JOptionPane.showMessageDialog(parent, Translation
                        .getTranslation("folderexception.dialog.text",
                            fInfo.name, FolderException.super.getMessage())
                        + addText, Translation.getTranslation(
                        "folderexception.dialog.title", fInfo.name),
                        JOptionPane.ERROR_MESSAGE);
                }
            };
            if (!EventQueue.isDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(runner);
                } catch (InterruptedException e) {
                    Logger.getLogger(FolderException.class).error(e);
                } catch (InvocationTargetException e) {
                    Logger.getLogger(FolderException.class).error(e);
                }
            } else {
                // We are in event disp thread
                runner.run();
            }
        }
    }
}