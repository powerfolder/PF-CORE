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
package de.dal33t.powerfolder.ui.widget;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.util.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

/**
 * Basically a SwingWorker, which shows some activity visualisation after some
 * working time.
 * <p>
 * TODO Add cancel button.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class ActivityVisualizationWorker extends de.dal33t.powerfolder.ui.util.SwingWorker {
    private JDialog dialog;
    private JLabel infoText;
    private JProgressBar bar;
    private ProgressListener progressListener;

    private Thread dialogThread;
    private long startTime;
    private boolean stopped;
    private Semaphore lock;

    /**
     * Activity visualization worker constructor for indeterminate progress.
     *
     * @param uiController
     *            the UI Controller
     */
    protected ActivityVisualizationWorker(UIController uiController) {
        this(uiController, true);
    }

    /**
     * Activity visualization worker constructor with progress listener
     *
     * @param uiController
     *            the UI Controller
     * @param indeterminate
     */
    protected ActivityVisualizationWorker(UIController uiController,
        boolean indeterminate)
    {
        this(uiController.getActiveFrame());
        if (!indeterminate) {
            progressListener = new MyProgressListener();
        }
    }

    protected ActivityVisualizationWorker(Window theParent) {
        this();
        dialog = new JDialog(theParent);
    }

    private ActivityVisualizationWorker() {
        lock = new Semaphore(1);
        stopped = false;
        dialogThread = new Thread(new DialogRunnable());
    }

    // API ********************************************************************

    /**
     * @return the title of the activity dialog, when it appears
     */
    protected abstract String getTitle();

    public void setInfoText(JLabel infoText) {
        this.infoText = infoText;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * @return the text displayed in the acitivy dialog over the progress bar
     */
    protected abstract String getWorkingText();

    // UI building ************************************************************

    private void initComponents() {
        if (dialog == null) {
            dialog = new JDialog();
        }
        dialog.setModal(false);
        dialog.setResizable(false);
        dialog.setTitle(getTitle());

        bar = new JProgressBar();
        bar.setIndeterminate(progressListener == null);
        infoText = new JLabel(getWorkingText());

        // Layout
        FormLayout layout = new FormLayout(
            "20dlu, max(70dlu;pref):grow, max(70dlu;pref):grow, 20dlu",
            "pref, 14dlu, pref, 14dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.DLU14_BORDER);

        CellConstraints cc = new CellConstraints();

        // Add components
        builder.add(infoText, cc.xywh(1, 1, 4, 1));
        builder.add(bar, cc.xywh(2, 3, 2, 1));

        dialog.getContentPane().add(builder.getPanel());
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.pack();

        Component parent = dialog.getParent();
        // Orientation
        if (parent != null) {
            int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - dialog.getHeight())
                / 2;
            dialog.setLocation(x, y);
        }
    }

    private long activityTookedMS() {
        return System.currentTimeMillis() - startTime;
    }

    // Override hooks *********************************************************

    @Override
    protected void beforeConstruct() {
        startTime = System.currentTimeMillis();
        dialogThread.start();
    }

    @Override
    protected void afterConstruct() {
        stopped = true;
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (dialog != null) {
            dialog.dispose();
        }
        lock.release();
    }

    // The dialog updater/runner **********************************************

    private final class MyProgressListener implements ProgressListener {
        public void progressReached(final double percentageReached) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (bar == null) {
                        return;
                    }
                    bar.setValue((int) percentageReached);
                }
            });
        }
    }

    /**
     * Enables the dialog after some time and displays some activity on it.
     */
    private class DialogRunnable implements Runnable {
        public void run() {
            if (stopped) {
                return;
            }
            // Step 1) Wait few seconds
            while (!stopped && activityTookedMS() < 500) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (stopped) {
                return;
            }

            // Step 2) Show dialog
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lock.tryAcquire();
                    initComponents();
                    lock.release();
                    if (!stopped) {
                        dialog.setVisible(true);
                    }
                }
            });
        }
    }
}
