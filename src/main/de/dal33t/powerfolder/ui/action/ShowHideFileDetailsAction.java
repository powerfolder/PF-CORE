/* $Id: ShowHideFileDetailsAction.java,v 1.3 2005/12/01 00:08:45 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;

/**
 * Action for toggeling a visibility of a file details panel. Makes it
 * visible/non-visible on actionPerformed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("serial")
public class ShowHideFileDetailsAction extends BaseAction {

    private HasDetailsPanel hasDetailsPanel;

    public ShowHideFileDetailsAction(HasDetailsPanel hasDetailsPanel,
        Controller controller) {
        super("showhidefiledetails", controller);
        if (hasDetailsPanel == null) {
            throw new NullPointerException("File details panel is null");
        }
        this.hasDetailsPanel = hasDetailsPanel;
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle visibility
        hasDetailsPanel.toggeDetails();
    }
}