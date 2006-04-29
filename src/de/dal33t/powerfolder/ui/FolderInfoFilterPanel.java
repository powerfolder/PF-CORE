package de.dal33t.powerfolder.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EditableHistoryComboBox;

/**
 * Holds a box with for entering keywords and 3 checkboxes (showNormal,
 * showDeleted and showExpected). The checkboxes are optional displayed. The
 * checkboxes labels also hold a count of the number of files that matches the
 * criteria in the FileFilterModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class FolderInfoFilterPanel {
    private JPanel panel;
    private Controller controller;
    private JCheckBox showEmptyBox;
    private JComponent textFilterField;
    private FolderInfoFilterModel folderInfoFilterModel;

    /**
     * @param showCheckBoxes
     *            set to false to hide them
     */
    public FolderInfoFilterPanel(Controller controller,
        FolderInfoFilterModel folderInfoFilterModel)
    {
        this.controller = controller;
        this.folderInfoFilterModel = folderInfoFilterModel;
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        // First initalize filterbar components
        showEmptyBox = new JCheckBox(new AbstractAction(Translation
            .getTranslation("publicfolders.filter.show_empty_folders"))
        {
            public void actionPerformed(ActionEvent event) {
                folderInfoFilterModel.setShowEmpty(showEmptyBox.isSelected());
            }
        });

        // Build search text field
        ValueModel searchFieldModel = folderInfoFilterModel.getSearchField();
        String infoText = Translation
            .getTranslation("filelist.filefilter.searchfieldinfo");
        textFilterField = new EditableHistoryComboBox(searchFieldModel, 30,
            "folderInfoTextSearch", controller.getPreferences(), infoText);

        // Now build panel and align items
        FormLayout layout = new FormLayout("150dlu, 7dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(textFilterField, cc.xy(1, 1));

        builder.add(showEmptyBox, cc.xy(3, 1));

        panel = builder.getPanel();

        panel.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
    }
}
