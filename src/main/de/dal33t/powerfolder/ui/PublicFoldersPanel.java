package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.JoinAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;

/**
 * Holds a Table with all the Public Folders
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.11 $
 */
public class PublicFoldersPanel extends PFUIComponent {
    private JPanel panel;
    private PublicFoldersTable table;
    private JScrollPane tablePane;
    private JPanel toolbar;
    private SelectionModel selectionModel;
    private JPopupMenu popupMenu;
    private FolderInfoFilterModel folderInfoFilterModel;
    private FolderInfoFilterPanel folderInfoFilterPanel;

    public PublicFoldersPanel(Controller controller) {
        super(controller);
        selectionModel = new SelectionModel();
        folderInfoFilterModel = new FolderInfoFilterModel(controller);
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            // main layout
            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(folderInfoFilterPanel.getUIComponent(), cc.xy(1, 1));
            builder.add(tablePane, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 3));
            builder.add(toolbar, cc.xy(1, 4));

            panel = builder.getPanel();
        }
        return panel;
    }

    public String getTitle() {
        return Translation.getTranslation("title.public.folders");
    }

    private void initComponents() {
        folderInfoFilterPanel = new FolderInfoFilterPanel(getController(),
            folderInfoFilterModel);
        table = new PublicFoldersTable(getController(), folderInfoFilterModel);

        table.getSelectionModel().addListSelectionListener(
            new TableSelectionListener());
        table
            .addMouseListener(new DoubleClickAction(new PreviewFolderAction()));

        tablePane = new JScrollPane(table);
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tablePane);
        UIUtil.setZeroHeight(tablePane);

        // Create toolbar
        toolbar = createToolBar();

        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        popupMenu = new JPopupMenu();
        popupMenu.add(new JoinAction(getController(), selectionModel));
        popupMenu.add(new ShowFilesAction(getController(), selectionModel));
        table.addMouseListener(new PopupMenuOpener(popupMenu));
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JPanel createToolBar() {
        /*
         * final JCheckBox showEmptyFolders = new JCheckBox("Show empty
         * folders"); showEmptyFolders.addChangeListener(new ChangeListener() {
         * public void stateChanged(ChangeEvent e) {
         * publicFoldersTableModel.setShowEmptyFolders(showEmptyFolders
         * .isSelected()); } });
         */
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(new RequestNetworkFolderListAction(
            getController())));
        bar.addUnrelatedGap();
        bar.addGridded(new JButton(new JoinAction(getController(),
            selectionModel)));
        bar.addRelatedGap();
        bar.addGridded(new JButton(new ShowFilesAction(getController(),
            selectionModel)));
        bar.addRelatedGap();

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /** updates the selectionModel for the actions */
    private class TableSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                selectionModel.setSelection(null);
            } else {
                FolderInfo folderInfo = (FolderInfo) table
                    .getPublicFoldersTableModel().getValueAt(selectedRow, 0);
                selectionModel.setSelection(folderInfo);
            }
        }
    }

    /**
     * Requests a new network folder list when performed
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class RequestNetworkFolderListAction extends BaseAction {
        private RequestNetworkFolderListAction(Controller controller) {
            super("requestnetfolderlist", controller);
        }

        public void actionPerformed(ActionEvent e) {
            table.getPublicFoldersTableModel().
            getController().getFolderRepository()
                .requestNetworkFolderListIfRequired();
        }
    }

    
    /**
     * Action which acts on selected folder. Shows the files of selected folder
     */
    private class ShowFilesAction extends BaseAction {
        // new selection model
        private SelectionModel selectionModel;

        public ShowFilesAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("previewfiles", controller);
            this.selectionModel = selectionModel;
            setEnabled(false);

            // Add behavior on selection model
            selectionModel
                .addSelectionChangeListener(new SelectionChangeListener() {
                    public void selectionChanged(SelectionChangeEvent event) {
                        Object selection = event.getSelection();
                        // enable button if there is something selected
                        setEnabled(selection != null);
                    }
                });
        }

        // called if show button clicked
        public void actionPerformed(ActionEvent e) {
            // selected folder
            FolderInfo folderInfo = (FolderInfo) selectionModel.getSelection();
            FolderDetails details = folderInfo
                .getFolderDetails(getController());
            getUIController().getControlQuarter().setSelected(details);
        }
    }

    /** on double click preview the folder* */
    private class PreviewFolderAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                FolderInfo folderInfo = (FolderInfo) table
                    .getPublicFoldersTableModel().getValueAt(selectedRow, 0);
                FolderDetails details = folderInfo
                    .getFolderDetails(getController());
                getUIController().getControlQuarter().setSelected(details);
            }
        }
    }

    /**
     * Listner on table header, takes care about the sorting of table
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public final void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof PublicFoldersTableModel) {
                    PublicFoldersTableModel fModel = (PublicFoldersTableModel) model;
                    boolean freshSorted = fModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        fModel.reverseList();
                    }
                }
            }
        }
    }
}
