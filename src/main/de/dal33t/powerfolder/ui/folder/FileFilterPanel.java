package de.dal33t.powerfolder.ui.folder;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Holds a box with for entering keywords and 3 checkboxes (showNormal,
 * showDeleted and shoeExpexted). The checkboxes are optional displayed. The
 * checkboxes labels also hold a count of the number of files that matches the
 * criteria in the FileFilterModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class FileFilterPanel {

    private JPanel panel;
    private FileFilterModel fileFilterModel;
    private JComboBox filterSelectionComboBox;

    /**
     * @param showCheckBoxes
     *            set to false to hide them
     */
    public FileFilterPanel(FileFilterModel fileFilterModel)
    {
        this.fileFilterModel = fileFilterModel;
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    public void reset() {
        fileFilterModel.reset();
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        FilterTextField filterTextField = new FilterTextField(12);
        fileFilterModel.setSearchField(filterTextField.getValueModel());

        filterSelectionComboBox = new JComboBox();
        filterSelectionComboBox.addItem(Translation
                .getTranslation("file_filter_panel.local_and_incoming"));
        filterSelectionComboBox.addItem(Translation
                .getTranslation("file_filter_panel.local_files_only"));
        filterSelectionComboBox.addItem(Translation
                .getTranslation("file_filter_panel.incoming_files_only"));
        filterSelectionComboBox.addItem(Translation
                .getTranslation("file_filter_panel.new_files_only"));
        filterSelectionComboBox.addItem(Translation
                .getTranslation("file_filter_panel.deleted_and_previous_files"));
        filterSelectionComboBox.addActionListener(new MyActionListener());

        FormLayout layout = new FormLayout("105dlu:grow, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(filterTextField.getUIComponent(), cc.xy(1, 1));
        builder.add(filterSelectionComboBox, cc.xy(3, 1));
        panel = builder.getPanel();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(filterSelectionComboBox)) {
                fileFilterModel.setMode(filterSelectionComboBox.getSelectedIndex());
                fileFilterModel.filter();
            }
        }
    }
}
