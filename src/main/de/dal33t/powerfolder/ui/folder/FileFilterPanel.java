package de.dal33t.powerfolder.ui.folder;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.Translation;

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
    private JCheckBox showNormalBox;
    private JLabel normalCount;
    private JCheckBox showDeletedBox;
    private JLabel deletedCount;
    private JCheckBox showExpectedBox;
    private JLabel expectedCount;
    private FilterTextField filterTextField;
    private FileFilterModel fileFilterModel;
    private boolean showCheckBoxes;

    /**
     * @param showCheckBoxes
     *            set to false to hide them
     */
    public FileFilterPanel(FileFilterModel fileFilterModel,
        boolean showCheckBoxes)
    {
        this.showCheckBoxes = showCheckBoxes;
        this.fileFilterModel = fileFilterModel;
        // listen to the filterModel to update the values if needed
        fileFilterModel
            .addFileFilterChangeListener(new FileFilterChangeListener() {
                public void filterChanged(FilterChangedEvent event) {
                    update();
                }

                public void countChanged(FilterChangedEvent event) {
                    update();
                }
            });

    }

    /** by default shows the checkboxes */
    public FileFilterPanel(FileFilterModel fileFilterModel) {
        this(fileFilterModel, true);
    }

    private void update() {
        int normal = fileFilterModel.getNormalCount();
        int deleted = fileFilterModel.getDeletedCount();
        int expected = fileFilterModel.getExpectedCount();
        String normalText = "";
        if (normal == -1) {
            normalText = "?";
        } else {
            normalText = normal + "";
        }
        String deletedText = "";
        if (deleted == -1) {
            deletedText = "?";
        } else {
            deletedText = deleted + "";
        }
        String expectedText = "";
        if (expected == -1) {
            expectedText = "?";
        } else {
            expectedText = expected + "";
        }
        final String normalTextFinal = normalText;
        final String deletedTextFinal = deletedText;
        final String expectedTextFinal = expectedText;
        Runnable runner = new Runnable() {
            public void run() {
                normalCount.setText(" (" + normalTextFinal + ")");
                deletedCount.setText(" (" + deletedTextFinal + ")");
                expectedCount.setText(" (" + expectedTextFinal + ")");
            }
        };
        EventQueue.invokeLater(runner);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    public void reset() {
        fileFilterModel.reset();
        showNormalBox.setSelected(fileFilterModel.isShowNormal());
        showDeletedBox.setSelected(fileFilterModel.isShowDeleted());
        showExpectedBox.setSelected(fileFilterModel.isShowExpected());
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        // First initalize filterbar components
        showNormalBox = new JCheckBox(new AbstractAction(Translation
            .getTranslation("filelist.filefilter.normal"))
        {
            public void actionPerformed(ActionEvent event) {
                fileFilterModel.setShowNormal(showNormalBox.isSelected());
            }
        });
        showNormalBox.setSelected(fileFilterModel.isShowNormal());

        normalCount = new JLabel(" (0)");

        showExpectedBox = new JCheckBox(new AbstractAction(Translation
            .getTranslation("filelist.filefilter.expected"), Icons.EXPECTED)
        {
            public void actionPerformed(ActionEvent event) {
                fileFilterModel.setShowExpected(showExpectedBox.isSelected());
            }
        });
        showExpectedBox.setForeground(Color.GRAY);
        showExpectedBox.setSelected(fileFilterModel.isShowExpected());

        expectedCount = new JLabel(" (0)");
        expectedCount.setForeground(Color.GRAY);

        showDeletedBox = new JCheckBox(new AbstractAction(Translation
            .getTranslation("filelist.filefilter.deleted"), Icons.DELETE)
        {
            public void actionPerformed(ActionEvent event) {
                fileFilterModel.setShowDeleted(showDeletedBox.isSelected());
            }
        });
        showDeletedBox.setForeground(Color.RED);
        showDeletedBox.setSelected(fileFilterModel.isShowDeleted());

        deletedCount = new JLabel(" (0)");
        deletedCount.setForeground(Color.RED);

        // Build search text field

        filterTextField = new FilterTextField(12); // 12 columns
        fileFilterModel.setSearchField(filterTextField.getValueModel());

        if (showCheckBoxes) {
            // Now build panel and align items
            FormLayout layout = new FormLayout(
                "105dlu:grow, 7dlu, pref, pref, 7dlu, pref, pref, 7dlu, pref, pref",
                "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(filterTextField.getUIComponent(), cc.xy(1, 1));

            builder.add(showNormalBox, cc.xy(3, 1));
            builder.add(normalCount, cc.xy(4, 1));
            builder.add(showExpectedBox, cc.xy(6, 1));
            builder.add(expectedCount, cc.xy(7, 1));
            builder.add(showDeletedBox, cc.xy(9, 1));
            builder.add(deletedCount, cc.xy(10, 1));

            panel = builder.getPanel();

        } else {// no checkboxes
            FormLayout layout = new FormLayout("105dlu:grow", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(filterTextField.getUIComponent(), cc.xy(1, 1));
            panel = builder.getPanel();
        }
    }
}
