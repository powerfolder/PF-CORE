/* $Id$
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.util.concurrent.Semaphore;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * Basically a SwingWorker, which shows some activity visualisation after some
 * working time.
 * <p>
 * TODO Add cancel button.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class ActivityVisualizationWorker extends SwingWorker {
    private JDialog dialog;
    private JLabel infoText;
    private JProgressBar bar;

    private Thread dialogThread;
    private long startTime;
    private boolean stopped;
    private Semaphore lock;

    public ActivityVisualizationWorker(UIController uiController) {
        this(uiController.getMainFrame().getUIComponent());
    }

    public ActivityVisualizationWorker(Frame theParent) {
        this();
        dialog = new JDialog(theParent);
    }

    private ActivityVisualizationWorker() {
        super();
        lock = new Semaphore(1);
        stopped = false;
        dialogThread = new Thread(new DialogRunnable());
    }

    // API ********************************************************************

    /**
     * @return the title of the activity dialog, when it appears
     */
    protected abstract String getTitle();

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
        bar.setIndeterminate(true);
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
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
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
                    try {
                        lock.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
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
