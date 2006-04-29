/* $Id: DownloadUpdateDialog.java,v 1.4 2005/06/13 12:49:36 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.StreamCallback;

/**
 * Dialog opened, when an programm update is detected and downloading
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class DownloadUpdateDialog extends PFUIComponent {
    private JDialog uiComponent;
    private JButton cancelButton;
    private JProgressBar processBar;

    // The callback of the copy process
    private StreamCallback streamCallback;

    private boolean canceled;

    /**
     * Initialize
     * 
     * @param controller
     */
    public DownloadUpdateDialog(Controller controller) {
        super(controller);
        canceled = false;
    }

    /**
     * Initalizes / builds all ui elements
     */
    public void initComponents() {
        // General dialog initalization
        uiComponent = new JDialog(getUIController().getMainFrame()
            .getUIComponent(), "Updating", false);

        uiComponent.setResizable(false);
        uiComponent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                canceled = true;
            }
        });

        // Cancel buttons
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
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
        builder.addLabel("Downloading update", cc.xywh(1, 1, 4, 1));
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
    private JDialog getUIComponent() {
        if (uiComponent == null) {
            initComponents();
        }
        return uiComponent;
    }

    /**
     * Opens the dialog
     * 
     * @return not used
     */
    public boolean open() {
        log().warn("Opening download dialog");
        getUIComponent().setVisible(true);
        return true;
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
     * Displays (opens) the dialog if nessesary.
     */
    private void displayIfNessesary() {
        if (!canceled && !getUIComponent().isVisible()) {
            // Open dialog
            open();
        }
    }

    /**
     * The callback for the download process
     * 
     * @return
     */
    public StreamCallback getStreamCallback() {
        if (streamCallback == null) {
            streamCallback = new StreamCallback() {
                public boolean streamPositionReached(int position,
                    int totalAvailable)
                {
                    // Open the display of this dialog if nessesary
                    displayIfNessesary();

                    // Set completion percentage
                    int completePercentage = position * 100 / totalAvailable;
                    setCompletionPercentage(completePercentage);

                    // Close
                    if (position >= totalAvailable) {
                        // Close dialog when download is ready
                        close();
                        canceled = false;
                    }

                    // Break stream if canceled
                    return canceled;
                }
            };
        }

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
}