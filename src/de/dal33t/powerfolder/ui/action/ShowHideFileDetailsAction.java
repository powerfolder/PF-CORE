/* $Id: ShowHideFileDetailsAction.java,v 1.3 2005/12/01 00:08:45 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import de.dal33t.powerfolder.Controller;

/**
 * Action for toggeling a visibility of a file details panel. Makes it
 * visible/non-visible on actionPerformed
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("serial")
public class ShowHideFileDetailsAction extends BaseAction {
    private JComponent panel;

    public ShowHideFileDetailsAction(JComponent fileDetailsPanel,
        Controller controller)
    {
        super("showhidefiledetails", controller);
        if (fileDetailsPanel == null) {
            throw new NullPointerException("File details panel is null");
        }
        panel = fileDetailsPanel;
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle visibility
        panel.setVisible(!panel.isVisible());
    }
}