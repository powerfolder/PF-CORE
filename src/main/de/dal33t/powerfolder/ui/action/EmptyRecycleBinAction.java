package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
@SuppressWarnings("serial")
public class EmptyRecycleBinAction extends BaseAction
{

    public EmptyRecycleBinAction(Controller controller) {
        super("empty_recycle_bin", controller);
        RecycleBin recycleBin = controller.getRecycleBin();
        recycleBin.addRecycleBinListener(new MyRecycleBinListener());
        setEnabled(getController().getRecycleBin().countAllRecycledFiles() > 0);
    }

    public void actionPerformed(ActionEvent e) {

        int choice = DialogFactory.genericDialog(getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation("empty_recycle_bin_confimation.title"),
                Translation.getTranslation("empty_recycle_bin_confimation.text"),
                new String[] {Translation.getTranslation("empty_recycle_bin_confimation.empty"),
                Translation.getTranslation("empty_recycle_bin_confimation.dont")},
                1, GenericDialogType.INFO); // Default = 1 = Dont Empty

        if (choice == 0) { // Empty bin
            setEnabled(false);
            ActivityVisualizationWorker worker = new ActivityVisualizationWorker(
                getUIController())
            {

                @Override
                public Object construct()
                {
                    getController().getRecycleBin().emptyRecycleBin();
                    setEnabled(true);
                    return null;
                }

                @Override
                protected String getTitle()
                {
                    return Translation
                        .getTranslation("empty_recycle_bin.working.title");
                }

                @Override
                protected String getWorkingText()
                {
                    return Translation
                        .getTranslation("empty_recycle_bin.working.description");
                }
            };
            worker.start();
        }
    }

    public class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
            setEnabled(true);
        }

        public void fileRemoved(RecycleBinEvent e) {
            setEnabled(getController().getRecycleBin().countAllRecycledFiles() > 0);
        }

        public void fileUpdated(RecycleBinEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

}
