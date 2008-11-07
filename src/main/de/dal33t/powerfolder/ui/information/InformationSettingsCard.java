package de.dal33t.powerfolder.ui.information;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;

import javax.swing.*;
import java.awt.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

public class InformationSettingsCard extends InformationCard {

    private Folder folder;
    private JPanel uiComponent;

    public InformationSettingsCard(Controller controller, FolderInfo folderInfo) {
        super(controller);
        folder = controller.getFolderRepository().getFolder(folderInfo);
    }

    public Image getCardImage() {
        return Icons.SETTINGS_IMAGE;
    }

    public String getCardTitle() {
        return Translation.getTranslation("information.settings.title",
                folder.getName());
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    private void buildUIComponent() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        uiComponent = builder.getPanel();
    }

    private void initialize() {
    }
}
