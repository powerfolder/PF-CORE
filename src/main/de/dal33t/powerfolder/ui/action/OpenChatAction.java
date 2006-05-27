package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.InformationQuarter;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Open a Chat Frame on a Folder or Member
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.10 $
 */

public class OpenChatAction extends SelectionBaseAction {

    InformationQuarter informationQuarter;

    public OpenChatAction(Controller controller, SelectionModel selectionModel)
    {
        super("openchat", controller, selectionModel);
        informationQuarter = controller.getUIController()
            .getInformationQuarter();
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();        
        if (selection instanceof Member) {
            Member member = (Member) selection;
            setEnabled(member.isCompleteyConnected());
        } else if (selection instanceof Folder) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        Object selection = getSelectionModel().getSelection(); 
        if (selection instanceof Folder) {
            informationQuarter.displayChat((Folder) selection);
        } else if (selection instanceof Member) {
            informationQuarter.displayChat((Member) selection);
        }
    }
}