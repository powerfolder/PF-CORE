package de.dal33t.powerfolder.ui.webservice;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.webservice.WebServiceClient;

/**
 * Quickinfo for the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    public WebServiceQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.webservice.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.WEBSERVICE_QUICK_INFO_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerLister());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        WebServiceClient ws = getController().getWebServiceClient();
        boolean con = ws.isAWebServiceConnected();
        String text1 = con ? Translation
            .getTranslation("quickinfo.webservice.connected") : Translation
            .getTranslation("quickinfo.webservice.notconnected");
        String text2;
        if (con) {
            int nMirrored = ws.getMirroredFolders().size();
            int nFolders = getController().getFolderRepository()
                .getFoldersCount();
            text2 = Translation.getTranslation(
                "quickinfo.webservice.foldersmirrored", nMirrored, nFolders);
        } else {
            text2 = "";
        }
        infoText1.setText(text1);
        infoText2.setText(text2);
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

    // Inner logic ************************************************************

    private class MyNodeManagerLister implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateText();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateText();
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
