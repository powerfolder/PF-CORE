package de.dal33t.powerfolder.ui.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.UIPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A simple panel to display a list of folder.
 * <p>
 * TODO: Add getter for model of selected items
 * <p>
 * TODO: Add sorting
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderListPanel implements UIPanel {
    private JPanel panel;
    private JScrollPane pane;
    private JList list;
    private SelectionInList listModel;

    /**
     * Constructs a new folder list panel.
     * 
     * @param aListModel
     *            the model containing the folders to be displayed
     */
    public FolderListPanel(SelectionInList aListModel) {
        Reject.ifNull(aListModel, "Listmodel is nulls");
        listModel = aListModel;
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "fill:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(pane, cc.xy(1, 1));
            panel = builder.getPanel();
            //panel.setOpaque(false);
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    private void initComponents() {
        list = BasicComponentFactory.createList(listModel);
        //list.setOpaque(false);
        list.setCellRenderer(new MyListCellRenderer());
        list.setBackground(Color.WHITE);
        
        pane = new JScrollPane(list);
        //pane.setOpaque(false);
       // pane.setBackground(Color.WHITE);
        UIUtil.removeBorder(pane);
        UIUtil.setZeroHeight(pane);
    }

    private static class MyListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList theList,
            Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component comp = super.getListCellRendererComponent(theList, value,
                index, isSelected, cellHasFocus);
            if (value instanceof Folder) {
                Folder folder = (Folder) value;
                setIcon(Icons.FOLDER);
                setText(folder.getName());
            }
            return comp;
        }
    }
}
