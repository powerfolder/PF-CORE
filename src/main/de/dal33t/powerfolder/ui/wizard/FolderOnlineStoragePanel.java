package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.FolderComboBox;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class FolderOnlineStoragePanel extends PFWizardPanel {

    private SelectionInList<Folder> foldersModel;
    private FolderComboBox folderList;

    public FolderOnlineStoragePanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public boolean hasNext() {
        return foldersModel.getSelection() != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:pref, 5dlu, pref, pref:grow",
            "pref, 10dlu, pref, 10dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.mirrorchoosefolder"), cc.xyw(1,
            1, 4));

        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(1,
            3));
        builder.add(folderList.getUIComponent(), cc.xy(3, 3));

        LinkLabel link = new LinkLabel(Translation
            .getTranslation("wizard.webservice.learnmore"),
            "http://www.powerfolder.com/node/webservice");
        // FIXME This is a hack because of "Fusch!"
        link.setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
        builder.add(link, cc.xyw(1, 5, 3));
        return builder.getPanel();
    }

    public WizardPanel next() {
        Folder folder = foldersModel.getSelection();

        // Actually setup mirror
        try {
            getController().getOSClient().getFolderService().createFolder(
                folder.getInfo(), SyncProfile.BACKUP_TARGET);

            // Choose location...
            return new TextPanelPanel(getController(),
                "Online Storage Setup Successful",
                "You successfully setup the Online Storage\nto mirror folder "
                    + folder.getName() + ".\n \n"
                    + "Please keep in mind that the inital backup\n"
                    + "may take some time on big folders.");
        } catch (FolderException e) {
            log().error(e);
            return new TextPanelPanel(getController(),
                "Online Storage Setup Error",
                "PowerFolder was unable\nto setup folder " + folder.getName()
                    + ".\n \n" + "Cause:\n" + e.getMessage());
        }

    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        ServerClient ws = getController().getOSClient();
        List<Folder> folders = new ArrayList<Folder>(getController()
            .getFolderRepository().getFoldersAsCollection());
        folders.removeAll(ws.getJoinedFolders());
        foldersModel = new SelectionInList<Folder>(folders);
        folderList = new FolderComboBox(foldersModel);
        foldersModel.getSelectionHolder().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateButtons();
                }
            });
        updateButtons();

        setPicto(Icons.WEBSERVICE_PICTO);
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.webservice.mirrorsetup");
    }
}
