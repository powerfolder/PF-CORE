package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;

/**
 * Debug panel shows buttons to shutdown/start the FileRquestor, TransferManager
 * and the NodeManager.
 *
 * @author <A HREF="mailto:sujith@powerfolder.com">Sujith Mahesh</A>
 * @version $Revision: 1 $
 */
public class OpenDebugInformationAction extends BaseAction{
	
	public OpenDebugInformationAction(Controller controller) {
        super("action_open_debug_information", controller);
    }

	public void actionPerformed(ActionEvent arg0) {
		getController().getUIController().openDebugInformation();
	}

}
