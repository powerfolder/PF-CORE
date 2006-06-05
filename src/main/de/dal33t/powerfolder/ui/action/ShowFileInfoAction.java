/* $Id: ShowFileInfoAction.java,v 1.4 2006/01/23 00:37:08 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import com.jgoodies.forms.factories.ButtonBarFactory;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action to show the currently selected file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ShowFileInfoAction extends BaseAction {
    private BaseDialog dialog;
    private FileDetailsPanel panel;

    /**
     * Initalizes a new showfileinfo action. displays a dialog with information
     * about the selected file
     * 
     * @param controller
     * @param selectionModel
     */
    public ShowFileInfoAction(Controller controller, SelectionModel selectionModel)
    {
        super("showfileinfo", controller);
        panel = new FileDetailsPanel(controller, selectionModel);
    }

    public void actionPerformed(ActionEvent e) {
        // Show dialog
        show();
    }

    /**
     * Opens the dialog
     */
    private void show() {
        if (dialog == null) {
            initDialog();
        }
        dialog.open();
    }

    /**
     * Initalizes the dialog around the file info panel
     */
    private void initDialog() {
        dialog = new BaseDialog(getController(), false) {

            public String getTitle() {
                // TODO Auto-generated method stub
                return "TODO";
            }

            protected Icon getIcon() {
                // TODO Auto-generated method stub
                return null;
            }

            protected Component getContent() {
                return panel.getPanel();
            }

            protected Component getButtonBar() {
                JButton okButton = createOKButton(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(false);
                    }
                });
                return ButtonBarFactory.buildCenteredBar(okButton);
            }
        };
    }
}