package de.dal33t.powerfolder.ui.home;

import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.tree.TreeNode;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.DoubleClickAction;

/**
 * Holds the root items in a table.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class RootPanel extends PFUIComponent {
    private JPanel panel;

    private RootQuickInfoPanel quickInfo;
    private JScrollPane tableScroller;
    private RootTable rootTable;
    //private JPanel toolbar;

    public RootPanel(Controller controller) {
        super(controller);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, pref, fill:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(quickInfo.getUIComponent(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.add(tableScroller, cc.xy(1, 3));
            // TODO: Toolbar removed
            // builder.addSeparator(null, cc.xy(1, 4));
            // builder.add(toolbar, cc.xy(1, 5));
            panel = builder.getPanel();
        }
        return panel;
    }

    // TODO what title is usefull here?
    public String getTitle() {
        return null;
    }

    private void initComponents() {
        quickInfo = new RootQuickInfoPanel(getController());

        final RootTableModel rootTableModel = new RootTableModel(
            getController());
        rootTable = new RootTable(rootTableModel, getController());
        tableScroller = new JScrollPane(rootTable);
        Util.whiteStripTable(rootTable);
        Util.removeBorder(tableScroller);
        Util.setZeroHeight(tableScroller);
        rootTable.addMouseListener(new DoubleClickAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = rootTable.getSelectedRow();

                TreeNode node = (TreeNode) rootTableModel.getValueAt(row, 0);
                getUIController().getControlQuarter().setSelected(node);
            }
        }));

        //toolbar = createToolBar();
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
   /* private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        bar.addGridded(new JButton(new EmptyRecycleBinAction(getController())));
        bar.setBorder(Borders.DLU4_BORDER);

        return bar.getPanel();
    }*/
}
