package de.dal33t.powerfolder.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.FolderComboBox;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.webservice.WebServiceClient;
import de.dal33t.powerfolder.webservice.WebServiceException;

public class MirrorFolderPanel extends PFWizardPanel {
    private boolean initalized = false;

    private SelectionInList foldersModel;
    private FolderComboBox folderList;

    public MirrorFolderPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return foldersModel.getSelection() != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    public WizardPanel next() {
        Folder folder = (Folder) foldersModel.getSelection();

        // Actually setup mirror
        try {
            getController().getWebServiceClient().setupFolder(folder);

            // Choose location...
            return new TextPanelPanel(getController(),
                "WebService Setup Successful",
                "You successfully setup the WebService\nto mirror folder "
                    + folder.getName() + ".\n \n"
                    + "Please keep in mind that the inital backup\n"
                    + "may take some time on big folders.");
        } catch (WebServiceException e) {
            log().error(e);
            return new TextPanelPanel(getController(),
                "WebService Setup Error",
                "PowerFolder was unable\nto setup folder " + folder.getName()
                    + ".\n \n" + "Cause:\n" + e.getMessage());
        }

    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        setBorder(Borders.EMPTY_BORDER);
        FormLayout layout = new FormLayout(
            "pref, 15dlu, pref, 3dlu, fill:100dlu, 0:grow",
            "pref, 15dlu, pref, 7dlu, pref, 7dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout, this);
        builder.setBorder(Borders.createEmptyBorder("5dlu, 20dlu, 0, 0"));
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Icons.WEBSERVICE_PICTO), cc.xywh(1, 3, 1, 3,
            CellConstraints.DEFAULT, CellConstraints.TOP));
        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.webservice.mirrorsetup")), cc.xyw(3, 1, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.mirrorchoosefolder"), cc.xyw(3,
            3, 4));

        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(3,
            5));
        builder.add(folderList.getUIComponent(), cc.xy(5, 5));

        LinkLabel link = new LinkLabel(Translation
            .getTranslation("wizard.webservice.learnmore"),
            "http://www.powerfolder.com/node/webservice");
        // FIXME This is a hack because of "Fusch!"
        link.setBorder(Borders.createEmptyBorder("0, 1dlu, 0, 0"));
        builder.add(link, cc.xyw(3, 7, 4));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        WebServiceClient ws = getController().getWebServiceClient();
        List<Folder> folders = new ArrayList<Folder>(getController()
            .getFolderRepository().getFoldersAsCollection());
        folders.removeAll(ws.getMirroredFolders());
        foldersModel = new SelectionInList(folders);
        folderList = new FolderComboBox(foldersModel);
        foldersModel.getSelectionHolder().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateButtons();
                }
            });
        updateButtons();
    }
}
