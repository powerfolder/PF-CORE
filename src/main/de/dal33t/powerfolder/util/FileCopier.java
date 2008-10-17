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
package de.dal33t.powerfolder.util;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.ui.FileCopierProgressBar;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileCopier to copy files in a different Thread, shows a progress bar after
 * 0.5 seconds. Calls the Directory after copy to notify that a file is added.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.11 $
 */
public class FileCopier extends PFComponent {

    private static final Logger log = Logger.getLogger(FileCopier.class.getName());
    private static final String TEMP_FILENAME_SUFFIX = "(copy_temp_powerfolder)";

    private JDialog dialog;

    private boolean abort = false;
    private boolean finished = false;
    private boolean started = false;
    private Queue<FromTo> filesToCopy = new LinkedList<FromTo>();
    private FileCopierProgressBar progressBar;
    private JLabel filenameLabel = new JLabel();
    private JButton abortButton;
    private FromTo currentFromTo;

    public FileCopier(Controller controller) {
        super(controller);
    }

    /**
     * add a file to copy. the source (from), destination (to) and the callback
     * on completion (directory)
     */
    public void add(File from, File to, Directory directory) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot copy onto itself");
        }
        FromTo fromTo = new FromTo(from, to, directory);
        add(fromTo);
    }

    /** during copy process the old file (that if exists will be overwritten) will be 
     * renamed to a temp file to enable restore if copy preocess is aborted. 
     * This check returns true if the current temp file equals this one */
    public static boolean isTempBackup(File file) {
        return file.getAbsolutePath().endsWith(TEMP_FILENAME_SUFFIX);                
    }
    
    /**
     * add a file to copy. FromTo has the source (from), destination (to) and
     * the callback on completion (directory)
     */
    private void add(FromTo fromTo) {
        finished = false;
        filesToCopy.add(fromTo);
    }

    public boolean isStarted() {
        return started;
    }

    /** call in a Runnable Thread */
    public void start() {        
        if (started) {
            return;
        }
        finished = false;
        started = true;
        abort = false;

        // on each start a new progress bar to make sure we got the correct
        // numbers
        Runnable runner = new Runnable() {
            public void run() {
                progressBar = new FileCopierProgressBar(
                    JProgressBar.HORIZONTAL, FileCopier.this);
                filenameLabel.setPreferredSize(new Dimension(350, 20));

                abortButton = new JButton(new BaseAction("abort_copy",
                    getController())
                {
                    public void actionPerformed(ActionEvent e) {
                        abort = true;
                    }
                });
            }
        };
        if (EventQueue.isDispatchThread()) {
            runner.run();
        } else {
            try {
                EventQueue.invokeAndWait(runner);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception", e);
            }
        }
        
        TimerTask task = new TimerTask() {
            public void run() {
                showProgress();
            }
        };
        // schedule to show progressBar after 0.5 seconds
        getController().schedule(task, 500);

        while (!filesToCopy.isEmpty()) {
            if (abort) {
                filesToCopy.clear();
                break;
            }
            currentFromTo = filesToCopy.poll();
            try {
                filenameLabel.setText(currentFromTo.from.getName());
                copyFile(currentFromTo.from, currentFromTo.to, progressBar);
                // take the source modification date
                currentFromTo.to.setLastModified(currentFromTo.from
                    .lastModified());
                // call the directory to notify we have a new file
                currentFromTo.directory.add(currentFromTo.to);
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "IOException", ioe);
            }

        }
        started = false;
        finished = true;
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            if (progressBar != null) {
                progressBar.reset();
            }
        }
    }

    private void showProgress() {
        if (!finished) {
            Runnable runner = new Runnable() {
                public void run() {
                    if (dialog == null) {
                        dialog = new JDialog(getController().getUIController()
                            .getMainFrame().getUIComponent(), Translation
                            .getTranslation("file_copier.progress_dialog.title"),
                            false);
                        dialog
                            .setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                        // progressBar.setStringPainted(true);
                        FormLayout layout = new FormLayout("pref",
                            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
                        PanelBuilder builder = new PanelBuilder(layout);

                        CellConstraints cc = new CellConstraints();
                        builder.add(new JLabel(Translation
                            .getTranslation("file_copier.progress_dialog.text")),
                            cc.xy(1, 1));
                        builder.add(progressBar, cc.xy(1, 3));
                        builder.add(filenameLabel, cc.xy(1, 5));
                        builder.add(createAbortButtonBar(), cc.xy(1, 7));

                        builder.setBorder(Borders
                            .createEmptyBorder("4dlu, 4dlu, 4dlu, 4dlu"));

                        dialog.add(builder.getPanel());
                        dialog.pack();
                        // center:
                        Component parent = dialog.getOwner();
                        int x = parent.getX()
                            + (parent.getWidth() - dialog.getWidth()) / 2;
                        int y = parent.getY()
                            + (parent.getHeight() - dialog.getHeight()) / 2;
                        dialog.setLocation(x, y);
                    }
                    dialog.setVisible(true);
                }
            };
            if (EventQueue.isDispatchThread()) {
                runner.run();
            } else {
                EventQueue.invokeLater(runner);
            }
        }
    }

    private JPanel createAbortButtonBar() {
        return ButtonBarFactory.buildCenteredBar(abortButton);
    }

    /** calculates the sum of all file sizes */
    public int calculateSize() {
        int size = 0;
        FromTo[] fromTos = new FromTo[filesToCopy.size()];
        fromTos = filesToCopy.toArray(fromTos);
        for (FromTo fromTo : fromTos) {
            size += fromTo.from.length();
        }
        // add the current file
        if (currentFromTo != null) {
            return size + (int) currentFromTo.from.length();
        }
        return size;
    }

    /**
     * Copies a file to disk from another file . Overwrites the target file if
     * exists. The process may be observed with a FileCopierProgressBar
     * 
     * @param in
     *            the source file
     * @param to
     *            the target file
     * @param progressBar
     *            to report the progress to
     * @throws IOException
     *             any io exception
     */
    public void copyFile(File from, File to, FileCopierProgressBar progressBar)
        throws IOException
    {
        if (from == null) {
            throw new NullPointerException("From file is null");
        }
        if (!from.exists()) {
            throw new IOException("From file does not exists "
                + from.getAbsolutePath());
        }
        if (to == null) {
            throw new NullPointerException("To file is null");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot copy onto itself");
        }
        log.finer("coping file start: "+ from + " to: " + to);
        File backup = new File(to.getAbsoluteFile()+ TEMP_FILENAME_SUFFIX);
        if (to.exists()) {
            //try create backup (will be restored on abort)
            if (to.renameTo(backup)) {
                backup.deleteOnExit();
                log.finer("backup created: " +backup);
            } else {
                log.finer("backup failed: " +backup);
                //backup failed
                //delete old one
                if (!to.delete()) {
                    throw new IOException("Unable to delete old file "
                        + to.getAbsolutePath());
                }
            }
        }

        if (!to.createNewFile()) {
            throw new IOException("Unable to create file "
                + to.getAbsolutePath());
        }

        if (!to.canWrite()) {
            throw new IOException("Unable to write to " + to.getAbsolutePath());
        }

        FileInputStream in = new FileInputStream(from);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(to));

        byte[] buffer = new byte[1024];
        int read;
        int position = 0;
        try {
            do {
                if (abort) {
                    out.close();
                    to.delete();
                    //restore backup if its there
                    if (backup.exists()) {
                        log.fine("backup restore? :" +backup);
                        if (backup.renameTo(to)) {
                            log.finer("backup restore succes :" +backup);
                        } else {
                            log.finer("backup restore failed :" +backup);
                        }
                    }
                    break;
                }
                read = in.read(buffer);
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                position += read;
                if (progressBar != null) {
                    progressBar.bytesWritten(read);
                }
            } while (read >= 0);
        } finally {
            // Close streams
            in.close();
            out.close();
            //remove the backup
            if (backup.exists()) {
                backup.delete();
                log.finer("backup removed:" +backup);
            }                
        }
        log.finer("coping file end: "+ from + " to: " + to);
    }

    /**
     * FromTo has the source (from), destination (to) and the callback on
     * completion (directory)
     */
    public class FromTo {
        File from;
        File to;
        Directory directory;

        public FromTo(File from, File to, Directory directory) {
            this.from = from;
            this.to = to;
            this.directory = directory;
        }
    }
}
