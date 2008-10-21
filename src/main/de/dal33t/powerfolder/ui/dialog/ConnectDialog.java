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
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The dialog opened when connecting
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class ConnectDialog extends PFUIComponent {

    private JDialog uiComponent;
    private JLabel infoText;
    private JProgressBar bar;
    private boolean open;
    private boolean canceled;

    public ConnectDialog(Controller controller) {
        super(controller);
        open = false;
        canceled = false;
    }

    private void initComponents() {
        // General dialog initalization
        uiComponent = new JDialog(getUIController().getMainFrame()
            .getUIComponent(), Translation
            .getTranslation("dialog.connect.connecting"), false);

        // Cancel connect
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                canceled = true;
                close();
            }
        });

        uiComponent.setResizable(false);
        
        bar = new JProgressBar();
        bar.setIndeterminate(true);
        infoText = new JLabel(Translation
            .getTranslation("dialog.connect.connecting"));

        // Cancel buttons
        JButton cancelButton = new JButton(Translation
            .getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation("general.cancel.key").charAt(0));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                close();
            }
        });
        JComponent buttonBar = ButtonBarFactory.buildCenteredBar(cancelButton);

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
    private JDialog getUIComponent() {
        if (uiComponent == null) {
            initComponents();
        }
        return uiComponent;
    }

    /**
     * Sets the infotext
     * 
     * @param text
     */
    public void setInfoText(String text) {
        infoText.setText(text);
    }

    public void open(String connectStr) {
        canceled = false;
        getUIComponent().setVisible(true);
        infoText.setText(Translation.getTranslation(
            "dialog.connect.connecting_to", connectStr));

        // Run bar updater
        startBarUpdater();

        open = true;
    }

    /**
     * Closes the connection dialog
     */
    public void close() {
        open = false;
        getUIComponent().setVisible(false);
        getUIComponent().dispose();
    }

    /**
     * Starts the updater thread on the progress bar
     */
    private void startBarUpdater() {
        // Run bar updater
        Runnable barUpdater = new Runnable() {
            public void run() {
                int i = 0;
                while (open) {
                    i++;
                    bar.setValue(i % 101);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                logFiner("Connectionbar runner stopped");
            }
        };
        new Thread(barUpdater, "Connectionbar runner").start();
    }

    public boolean isCanceled() {
        return canceled;
    }
}