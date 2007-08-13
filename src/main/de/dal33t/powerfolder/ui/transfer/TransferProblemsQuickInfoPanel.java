package de.dal33t.powerfolder.ui.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.*;

/**
 * Show information about the transfer problems
 *
 * @author <a href="mailto:harryglasgow@gmail.com">Harry Glasgow</a>
 * @version $Revision: 2.0 $
 */
public class TransferProblemsQuickInfoPanel extends QuickInfoPanel {

    /** The left side image. */
    private JComponent picto;

    /** The heading */
    private JComponent headerText;

    /** The detail text */
    private JLabel infoText1;

    /**
     * Constructor
     * Builds a TransferProblemsQuickInfoPanel
     *
     * @param controller
     */
    protected TransferProblemsQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(new MyTransferManagerListener());
    }

    /**
     * Updates the info fields
     * Uses the transfer manager count of transfer problems.
     */
    private void updateText() {
        int problemCount = getController().getTransferManager().countTransferProblems();
        String text1 = Translation.getTranslation(
                "quickinfo.transfer.problems.detected", problemCount);
        infoText1.setText(text1);

    }

    // QuickInfoPanel implementation / override ***********

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents() {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
                .getTranslation("quickinfo.transfer.problems.title"));
        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        picto = new JLabel(Icons.TRANSFER_PROBLEM_PICTO);
        updateText();
        registerListeners();
    }

    protected JComponent getPicto() {
        return picto;
    }

    protected JComponent getHeaderText() {
        return headerText;
    }

    protected JComponent getInfoText1() {
        return infoText1;
    }

    protected JComponent getInfoText2() {
        return new JLabel();
    }

    // Core listeners *********************************************************

    /**
     * Listens to transfer manager
     */
    private class MyTransferManagerListener extends TransferAdapter {

        @Override
        public void downloadBroken(TransferManagerEvent event) {
            updateText();
        }

        @Override
        public void clearTransferProblems() {
            updateText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
