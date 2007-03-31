package de.dal33t.powerfolder.ui.recyclebin;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.EmptyRecycleBinAction;
import de.dal33t.powerfolder.ui.action.RestoreFileAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.RecycleDelete;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.HasUIPanel;
import de.dal33t.powerfolder.util.ui.PopupMenuOpener;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Shows a Table with the contents of the internal RecycleBin in a Table with a
 * buttonbar and context menu.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinPanel extends PFUIComponent implements HasUIPanel {
    private JComponent panel;
    private QuickInfoPanel quickInfo;
    private RecycleBinTable table;
    private JScrollPane tableScroller;
    private JPanel toolbar;
    /** The currently selected items */
    private SelectionModel selectionModel;
    private RestoreFileAction restoreFileAction;
    private RemoveFromRecycleBinAction removeFromRecycleBinAction;
    private JPopupMenu fileMenu;

    public RecycleBinPanel(Controller controller) {
        super(controller);
        selectionModel = new SelectionModel();
    }

    /** returns this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(tableScroller);
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        quickInfo = new RecycleBinQuickInfoPanel(getController());
        table = new RecycleBinTable(getController(), new RecycleBinTableModel(getController(),
            getController().getRecycleBin()));
        // Add selection listener for updating selection model
        table.getSelectionModel().addListSelectionListener(
            new RecycleBinListSelectionListener());
        tableScroller = new JScrollPane(table);
        restoreFileAction = new RestoreFileAction(getController(),
            selectionModel);
        removeFromRecycleBinAction = new RemoveFromRecycleBinAction(
            getController(), selectionModel);
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tableScroller);
        UIUtil.setZeroHeight(tableScroller);
        toolbar = createToolBar();
        buildPopupMenus();
    }

    public String getTitle() {
        return Translation.getTranslation("general.recyclebin");
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        bar.addGridded(new JButton(new EmptyRecycleBinAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(restoreFileAction));
        bar.addRelatedGap();
        bar.setBorder(Borders.DLU4_BORDER);
        return bar.getPanel();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        fileMenu.add(restoreFileAction);
        fileMenu.add(removeFromRecycleBinAction);
        table.addMouseListener(new PopupMenuOpener(fileMenu));

    }

    /** updates the SelectionModel if some selection has changed in the table */
    private class RecycleBinListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = table.getModel().getValueAt(
                        selectedRows[i], 0);
                }
                selectionModel.setSelections(selectedObjects);
            } else {
                selectionModel.setSelection(null);
            }
        }
    }

    private class RemoveFromRecycleBinAction extends SelectionBaseAction {
        public RemoveFromRecycleBinAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("remove_from_recycle_bin", controller, selectionModel);
            setEnabled(false);
        }

        /**
         * Called if button RemoveFromRecycleBinAction is clicked, will show a
         * warning, and remove the selected files from the internal RecycleBin. Will
         * delete to the system recycle bin if possible (warning will change
         * accordingly).
         */
        public void actionPerformed(ActionEvent e) {
            Object[] selections = getSelectionModel().getSelections();
            RecycleBin recycleBin = getController().getRecycleBin();
            String titleKey;
            String textKey;
            if (RecycleDelete.isSupported()) {
                titleKey = "remove_from_recycle_bin_confimation.title";
                textKey = "remove_from_recycle_bin_confimation.text";
            } else {
                titleKey = "delete_confimation.title";
                textKey = "delete_confimation.text";
            }
            String filesText = "";
            String separetor = "";
            for (Object selection : selections) {
                if (selection instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) selection;
                    filesText += separetor + fileInfo.getFilenameOnly();
                    separetor = "\n";
                }
            }
            
            int choice = DialogFactory
            .showScrollableOkCancelDialog(
                getController(),
                true, // modal
                true, // border
                Translation.getTranslation(titleKey),
                Translation
                    .getTranslation(textKey),
                    filesText, Icons.DELETE);
                   
            if (choice == JOptionPane.OK_OPTION) {
                for (Object selection : selections) {
                    if (selection instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selection;
                        if (recycleBin.isInRecycleBin(fileInfo)) {
                            recycleBin.delete(fileInfo);
                        }
                    }
                }
            }
        }

        /**
         * called if selections changed in table, enable this
         * RemoveFromRecycleBinAction if something is selected.
         */
        public void selectionChanged(SelectionChangeEvent event) {
            Object[] selections = getSelectionModel().getSelections();
            if ((selections == null) || selections.length == 0) {
                setEnabled(false);
                return;
            }
            for (Object selection : selections) {
                if (selection == null) {
                    setEnabled(false);
                    return;
                }
                if (!(selection instanceof FileInfo)) {
                    throw new IllegalStateException("Must be FileInfos "
                        + selection);
                }
            }
            setEnabled(true);
        }
    }
}
