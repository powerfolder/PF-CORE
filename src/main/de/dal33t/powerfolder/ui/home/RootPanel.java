package de.dal33t.powerfolder.ui.home;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.util.ui.DoubleClickAction;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Holds the root items in a table.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class RootPanel extends PFUIComponent {
    private JComponent panel;

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
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setContent(tableScroller);
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
            getController(), getUIController().getControlQuarter()
                .getNavigationTreeModel());
        rootTable = new RootTable(rootTableModel, getController());
        tableScroller = new JScrollPane(rootTable);
        UIUtil.whiteStripTable(rootTable);
        UIUtil.removeBorder(tableScroller);
        UIUtil.setZeroHeight(tableScroller);
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
