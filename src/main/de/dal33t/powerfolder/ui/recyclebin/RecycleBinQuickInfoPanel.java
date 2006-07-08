/* $Id: RecycleBinQuickInfoPanel.java,v 1.3 2006/04/14 14:52:33 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.recyclebin;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about the recycle bin
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.3 $
 */
public class RecycleBinQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    protected RecycleBinQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     * 
     * @return
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.recylcebin.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.RECYCLE_BIN_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getRecycleBin().addRecycleBinListener(
            new MyRecycleBinListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        int nFiles = getController().getRecycleBin().countAllRecycledFiles();
        infoText1.setText(Translation.getTranslation(
            "quickinfo.recylcebin.files", Integer.valueOf(nFiles)));
    }

    // Overridden stuff *******************************************************

    @Override
    protected JComponent getPicto()
    {
        return picto;
    }

    @Override
    protected JComponent getHeaderText()
    {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1()
    {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2()
    {
        return infoText2;
    }

    // Core listeners *********************************************************

    /**
     * Listener to recycle bin
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyRecycleBinListener implements RecycleBinListener {
        public void fileAdded(RecycleBinEvent e) {
            updateText();
        }

        public void fileRemoved(RecycleBinEvent e) {
            updateText();
        }
    }
}
