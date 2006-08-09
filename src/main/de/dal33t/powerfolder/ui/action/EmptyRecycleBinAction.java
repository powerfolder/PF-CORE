package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
@SuppressWarnings("serial")
public class EmptyRecycleBinAction extends BaseAction {

    public EmptyRecycleBinAction(Controller controller) {
        super("empty_recycle_bin", controller);
        RecycleBin recycleBin = controller.getRecycleBin();
        recycleBin.addRecycleBinListener(new MyRecycleBinListener());
        setEnabled(getController().getRecycleBin().getSize()>0);
    }

    public void actionPerformed(ActionEvent e) {
        
        int choice = JOptionPane.showConfirmDialog(getUIController()
            .getMainFrame().getUIComponent(), Translation
            .getTranslation("empty_recycle_bin_confimation.text"), Translation
            .getTranslation("empty_recycle_bin_confimation.title"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
            Icons.DELETE);
        
        if (choice == JOptionPane.OK_OPTION) {            
            //this could take long when big folder
            //maybe in diffrent thread?
            RecycleBin recycleBin = getController().getRecycleBin();
            recycleBin.emptyRecycleBin();            
        }
    }
    
    public class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
           setEnabled(true);            
        }

        public void fileRemoved(RecycleBinEvent e) {
            setEnabled(getController().getRecycleBin().getSize()>0);            
        }        
        
        public void fileUpdated(RecycleBinEvent e) {
        }
    }

}
