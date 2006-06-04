/* $Id: FriendsQuickInfoPanel.java,v 1.3 2006/04/14 14:52:32 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.friends;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about friends and friendssearch
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.3 $
 */
public class FriendsQuickInfoPanel extends QuickInfoPanel {
    private String headerText;
    private JComponent picto;
    private JComponent headerTextLabel;
    private JLabel infoText1;
    private JLabel infoText2;

    protected FriendsQuickInfoPanel(Controller controller, String aHeaderText) {
        super(controller);
        Reject.ifNull(aHeaderText, "Header text is null");
        headerText = aHeaderText;
    }

    /**
     * Initalizes the components
     * 
     * @return
     */
    @Override
    protected void initComponents()
    {
        headerTextLabel = SimpleComponentFactory
            .createBiggerTextLabel(headerText);

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.FRIENDS_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        int nFriends = getController().getNodeManager().countFriends();
        int nOnlineFriends = getController().getNodeManager()
            .countOnlineFriends();
        String text1 = Translation.getTranslation("quickinfo.friends.online",
            nOnlineFriends, nFriends);
        infoText1.setText(text1);
    }

    /**
     * Sets the number of found users
     * 
     * @param nUsers
     */
    void setUsersFound(int nUsers) {
        infoText2.setText(Translation.getTranslation(
            "quickinfo.friends.usersfound", nUsers));
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
        return headerTextLabel;
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
     * Listener to nodemanager
     * 
     * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            updateText();
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateText();
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateText();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateText();
        }

        public void friendAdded(NodeManagerEvent e) {
            updateText();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateText();
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateText();
        }
    }
}
