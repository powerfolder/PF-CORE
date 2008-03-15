/* $Id$
 */
package de.dal33t.powerfolder.ui.friends;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Quick info panel for a user.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class NodeQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JLabel headerText;
    private JLabel infoText1;
    private JLabel infoText2;
    private Member user;

    public NodeQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents() {
        headerText = SimpleComponentFactory.createBiggerTextLabel("");
        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");
        picto = new JLabel(Icons.USER_PICTO);
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getUIController().getControlQuarter().getSelectionModel()
            .addSelectionChangeListener(new MySelectionChangeListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        if (user != null) {
            headerText.setText(user.getNick());
            if (user.isCompleteyConnected()) {
                if (user.isOnLAN()) {
                    infoText1.setText(Translation
                        .getTranslation("quickinfo.user.isconnectedlan"));
                } else {
                    infoText1.setText(Translation
                        .getTranslation("quickinfo.user.isconnectedinet"));
                }
                if (user.isSecure()) {
                    infoText2.setText(Translation
                        .getTranslation("quickinfo.user.secureconnected"));
                } else {
                    infoText2.setText("");
                }
                if (getController().isVerbose()) {
                    // FIXME WHAT A UGLY HACK
                    infoText2.setText(""
                        + ((Loggable) user.getPeer()).getLoggerName());
                }
            } else {
                infoText1.setText(Translation
                    .getTranslation("quickinfo.user.isdiconnected"));
                infoText2.setText("");
            }
        }
    }

    // Overridden stuff *******************************************************

    @Override
    protected JComponent getPicto() {
        return picto;
    }

    @Override
    protected JComponent getHeaderText() {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1() {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2() {
        return infoText2;
    }

    private void setUser(Member aUser) {
        if (aUser != null) {
            user = aUser;
            updateText();
        }
    }

    // UI listeners
    private class MySelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            SelectionModel selectionModel = (SelectionModel) event.getSource();
            Object selection = selectionModel.getSelection();

            if (selection instanceof Member) {
                setUser((Member) selection);
            }
        }

    }

    // Core listeners *********************************************************

    private class MyNodeManagerListener implements NodeManagerListener {

        private void updateTextIfRequired(NodeManagerEvent e) {
            if (e.getNode().equals(user)) {
                updateText();
            }
        }

        public void friendAdded(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateTextIfRequired(e);
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

}
