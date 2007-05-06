package de.dal33t.powerfolder.ui.model;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;

public class TransferManagerModel  extends PFUIComponent {
    
    private ClearCompletedAction clearCompletedAction;

    public TransferManagerModel(Controller controller) {
        super(controller);
        // TODO Auto-generated constructor stub
    }
    
    // Actions ****************************************************************
    
    /**
     * Aborts the selected downloads
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    public class ClearCompletedAction extends BaseAction {
        public ClearCompletedAction(Controller controller) {
            super("cleardompleteddownloads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getTransferManager().clearCompletedDownloads();
        }
    }
    
    public ClearCompletedAction getClearCompletedAction(Controller controller) {
        if (clearCompletedAction == null) {
            clearCompletedAction = new ClearCompletedAction(controller);
        }
        return clearCompletedAction;
    }

    
    

}
