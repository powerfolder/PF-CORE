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
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.util.StreamCallback;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog opened, when an programm update is detected and downloading
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class DownloadUpdateDialog extends PFUIComponent {

    private JDialog uiComponent;
    private JProgressBar processBar;

    // The callback of the copy process
    private StreamCallback streamCallback;

    private boolean canceled;

    private final String version;

    /**
     * Initialize
     *
     * @param controller
     */
    public DownloadUpdateDialog(Controller controller, String version) {
        super(controller);
        canceled = false;
        this.version = version;
        streamCallback = new MyStreamCallback();
    }

    /**
     * Initalizes / builds all ui elements
     */
    public void initComponents() {
        // General dialog initalization
        uiComponent = new JDialog(getUIController().getMainFrame()
            .getUIComponent(), Translation
            .getTranslation("dialog.update.updating"), false);

        uiComponent.setResizable(false);
        uiComponent.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                canceled = true;
            }
        });

        // Cancel buttons
        JButton cancelButton = new JButton(Translation
            .getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation("general.cancel")
            .charAt(0));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Callback call to cancel
                cancelPressed();
            }
        });
        JComponent buttonBar = ButtonBarFactory.buildCenteredBar(cancelButton);

        // Progress bar
        processBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);

        // Layout
        FormLayout layout = new FormLayout(
            "20dlu, max(70dlu;pref):grow, max(70dlu;pref):grow, 20dlu",
            "pref, 14dlu, pref, 14dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.DLU14_BORDER);
        CellConstraints cc = new CellConstraints();

        // Add components
        builder.addLabel(Translation.getTranslation(
                "dialog.update.updating.text", version), cc.xywh(1, 1, 4, 1));
        builder.add(processBar, cc.xywh(2, 3, 2, 1));
        builder.add(buttonBar, cc.xywh(2, 5, 2, 1));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.pack();

        // Orientation
        Component parent = uiComponent.getOwner();
        if (parent != null) {
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;
            uiComponent.setLocation(x, y);
        }
    }

    /**
     * Returns the ui component (dialog)
     *
     * @return
     */
    private synchronized JDialog getUIComponent() {
        if (uiComponent == null) {
            initComponents();
        }
        return uiComponent;
    }

    /**
     * Opens the dialog
     */
    public void openInEDT() {
        logFine("Opening download dialog");
        UIUtil.invokeLaterInEDT(new Runnable() {
            public void run() {
                getUIComponent().setVisible(true);
            }
        });
    }

    /**
     * Closes the dialog
     */
    public void close() {
        if (uiComponent != null) {
            uiComponent.dispose();
        }
    }

    /**
     * Sets the completion state of this download
     *
     * @param compState
     */
    public void setCompletionPercentage(int compState) {
        if (processBar != null) {
            processBar.setValue(compState);
        }
    }

    /**
     * @return The callback for the download process
     */
    public StreamCallback getStreamCallback() {
        return streamCallback;
    }

    /**
     * Answers if was canceled
     *
     * @return
     */
    public boolean isCancelled() {
        return canceled;
    }

    /**
     * Callback method, called on cancel button
     */
    private void cancelPressed() {
        // Cancel pressed
        canceled = true;
        uiComponent.dispose();
    }

    private final class MyStreamCallback implements StreamCallback {
        public boolean streamPositionReached(final long position,
            final long totalAvailable)
        {
            Runnable runner = new Runnable() {
                public void run() {
                    // Open the display of this dialog if nessesary
                    // displayIfNessesary();

                    // Set completion percentage
                    int completePercentage = (int) (position * 100 / totalAvailable);
                    setCompletionPercentage(completePercentage);

                    // Close
                    if (position >= totalAvailable) {
                        // Close dialog when download is ready
                        close();
                        canceled = false;
                    }
                }
            };
            UIUtil.invokeLaterInEDT(runner);

            // Break stream if canceled
            return canceled;
        }
    }
}