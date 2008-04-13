/* $Id: DownloadsPanel.java,v 1.3 2006/04/15 04:23:51 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.HasDetailsPanel;
import de.dal33t.powerfolder.ui.action.ShowHideFileDetailsAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all information about downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class DownloadsPanel extends PFUIPanel implements HasDetailsPanel {
    private JComponent panel;

    private QuickInfoPanel quickInfo;
    private DownloadsTable table;
    private DownloadsTableModel tableModel;
    private JScrollPane tablePane;
    private JComponent toolbar;

    private FileDetailsPanel fileDetailsPanel;
    private JComponent fileDetailsPanelComp;
    private JToggleButton showHideFileDetailsButton;

    private File selectedFileBase;

    // The actions
    private Action startDownloadsAction;
    private Action abortDownloadsAction;
    private Action showHideFileDetailsAction;
    private Action clearCompletedAction;
    private Action openLocalFolderAction;
    private JCheckBox autoCleanupCB;

    public DownloadsPanel(Controller controller) {
        super(controller);
    }

    // UI Building ************************************************************

    /**
     * @return (and builds layzily) the ui component of this panel
     */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow",
            "fill:0:grow, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(tablePane, cc.xy(1, 1));
        builder.add(fileDetailsPanelComp, cc.xy(1, 2));
        return builder.getPanel();
    }

    public static String getTitle() {
        return Translation.getTranslation("general.downloads");
    }

    private void initComponents() {
        quickInfo = new DownloadsQuickInfoPanel(getController());

        final TransferManagerModel model = getUIController()
            .getTransferManagerModel();
        // Download table
        table = new DownloadsTable(model);
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        tableModel = (DownloadsTableModel) table.getModel();
        tablePane = new JScrollPane(table);

        // Whitestrip & set sizes
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tablePane);
        UIUtil.removeBorder(tablePane);

        // The file/download info
        fileDetailsPanelComp = getFileDetailsPanelComp();
        fileDetailsPanelComp.setVisible(false);

        // Initalize actions
        abortDownloadsAction = new AbortDownloadAction();
        startDownloadsAction = new StartDownloadsAction();
        showHideFileDetailsAction = new ShowHideFileDetailsAction(this,
            getController());
        showHideFileDetailsButton = new JToggleButton(showHideFileDetailsAction);

        clearCompletedAction = new ClearCompletedAction();
        openLocalFolderAction = new OpenLocalFolderAction(getController());

        autoCleanupCB = new JCheckBox(Translation
            .getTranslation("download_panel.auto_cleanup.name"));
        autoCleanupCB.setToolTipText(Translation
            .getTranslation("download_panel.auto_cleanup.description"));
        autoCleanupCB.setSelected(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
            .getValueBoolean(getController()));
        autoCleanupCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.getDownloadsAutoCleanupModel().setValue(
                    autoCleanupCB.isSelected());
                ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                    .setValue(getController(), String.valueOf(autoCleanupCB
                        .isSelected()));
                getController().saveConfig();
            }
        });

        // Create toolbar
        toolbar = createToolBar();

        // Add mouselisteners to table
        table.addMouseListener(new DoubleClickAction(startDownloadsAction));
        table.addMouseListener(new PopupMenuOpener(createPopupMenu()));

        // Listener on table selections
        table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        // Set file details
                        selectedFileBase = null;
                        int index = table.getSelectionModel()
                            .getLeadSelectionIndex();
                        Download dl = tableModel.getDownloadAtRow(index);
                        if (dl != null) {
                            fileDetailsPanel.setFile(dl.getFile());
                            File diskFile = dl.getFile().getDiskFile(getController().getFolderRepository());
                            if (diskFile != null) {
                                selectedFileBase = diskFile.getParentFile();
                            }
                        }

                        // Update actions
                        updateActions();
                    }
                }
            });

        // setup inital actions state
        updateActions();
    }

    private JComponent getFileDetailsPanelComp() {
        if (fileDetailsPanelComp == null) {
            fileDetailsPanelComp = createFileDetailsPanel();
        }
        return fileDetailsPanelComp;
    }

    /**
     * @return the file panel
     */
    private JComponent createFileDetailsPanel() {
        fileDetailsPanel = new FileDetailsPanel(getController());

        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 3dlu, pref, fill:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 4));
        return builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(startDownloadsAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(abortDownloadsAction));
        bar.addUnrelatedGap();
        bar.addGridded(showHideFileDetailsButton);

        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            bar.addRelatedGap();
            bar.addGridded(new JButton(openLocalFolderAction));
        }

        bar.addRelatedGap();
        bar.addGridded(new JButton(clearCompletedAction));
        bar.addRelatedGap();
        bar.addGridded(autoCleanupCB);
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /** Helper class, Opens the local folder on action * */
    private class OpenLocalFolderAction extends BaseAction {

        public OpenLocalFolderAction(Controller controller) {
            super("open_local_folder", controller);
        }

        /**
         * opens the folder currently in view in the operatings systems file
         * explorer
         */
        public void actionPerformed(ActionEvent e) {
            // File localBase = folder.getLocalBase();
            File localBase = selectedFileBase;
            try {
                FileUtils.executeFile(localBase);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Creates the downloads popup menu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
        popupMenu.add(startDownloadsAction);
        popupMenu.add(abortDownloadsAction);
        popupMenu.addSeparator();
        popupMenu.add(clearCompletedAction);
        popupMenu.add(openLocalFolderAction);
        return popupMenu;
    }

    // Helper methods *********************************************************

    /**
     * Updates all action states (enabled/disabled)
     */
    private void updateActions() {
        abortDownloadsAction.setEnabled(false);
        startDownloadsAction.setEnabled(false);

        int[] rows = table.getSelectedRows();
        boolean rowsSelected = rows.length > 0;
        boolean rowsExist = table.getRowCount() > 0;

        openLocalFolderAction.setEnabled(rowsSelected
                && selectedFileBase != null);
        clearCompletedAction.setEnabled(rowsExist);

        if (rowsSelected) {
            for (int row : rows) {
                Download download = tableModel.getDownloadAtRow(row);
                if (download == null) {
                    continue;
                }
                if (download.isCompleted()) {
                    startDownloadsAction.setEnabled(true);
                } else {
                    abortDownloadsAction.setEnabled(true);
                }
            }
        }
    }

    // Inner classes **********************************************************

    /**
     * Starts the selected downloads
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class StartDownloadsAction extends BaseAction {
        public StartDownloadsAction() {
            super("startdownloads", DownloadsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            if (rows == null || rows.length <= 0) {
                return;
            }

            Download[] selected = new Download[rows.length];
            for (int i = 0; i < rows.length; i++) {
                selected[i] = tableModel.getDownloadAtRow(rows[i]);
            }

            // Abort it two steps, because .abort causes model to change
            for (Download download : selected) {
                if (download == null) {
                    continue;
                }
                File file = download.getFile().getDiskFile(
                    DownloadsPanel.this.getController().getFolderRepository());
                if (file != null && file.exists()) {
                    try {
                        FileUtils.executeFile(file);
                    } catch (IOException ex) {
                        log().error(ex);
                    }
                }
            }
        }
    }

    /**
     * Aborts the selected downloads
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class AbortDownloadAction extends BaseAction {
        public AbortDownloadAction() {
            super("abortdownload", DownloadsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            // Abort in background
            SwingWorker worker = new SwingWorker() {
                @Override
                public Object construct() {
                    int[] rows = table.getSelectedRows();
                    if (rows == null || rows.length <= 0) {
                        return null;
                    }

                    Set<DownloadManager> dlMans = new HashSet<DownloadManager>();
                    for (int i = 0; i < rows.length; i++) {
                        dlMans.add(tableModel.getDownloadAtRow(rows[i]).getDownloadManager());
                    }

                    // Abort it two steps, because .abort causes model to change
                    for (DownloadManager manager: dlMans) {
                        manager.abort();
                    }
                    return null;
                }

            };
            worker.start();
        }
    }

    // Helper classes *********************************************************

    /**
     * Helper class which opens a popmenu when requested (right-mouseclick)
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class PopupMenuOpener extends MouseAdapter {
        private JPopupMenu popupMenu;

        private PopupMenuOpener(JPopupMenu popupMenu) {
            if (popupMenu == null) {
                throw new NullPointerException("Popupmenu is null");
            }
            this.popupMenu = popupMenu;
        }

        public void mousePressed(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                showContextMenu(evt);
            }
        }

        public void mouseReleased(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                showContextMenu(evt);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * Mouselistener, which perfoms the action, when clicked
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class DoubleClickAction extends MouseAdapter {
        private Action action;

        public DoubleClickAction(Action action) {
            if (action == null) {
                throw new NullPointerException("Action is null");
            }
            this.action = action;
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2
                && action.isEnabled())
            {
                action.actionPerformed(null);
            }
        }
    }

    public void clearDownloads() {

        // Clear completed downloads
        SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                int rowCount = table.getRowCount();
                if (rowCount == 0) {
                    return null;
                }

                // If no rows are selected,
                // arrange for all downloads to be cleared.
                boolean noneSelected = true;
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (table.isRowSelected(i)) {
                        noneSelected = false;
                        break;
                    }
                }

                // Do in two passes so changes to the model do not affect
                // the process.
                List<Download> downloadsToClear = new ArrayList<Download>();

                for (int i = 0; i < table.getRowCount(); i++) {
                    if (noneSelected || table.isRowSelected(i)) {
                        Download dl = tableModel.getDownloadAtRow(i);
                        if (dl.isCompleted()) {
                            downloadsToClear.add(dl);
                        }
                    }
                }
                for (Download dl : downloadsToClear) {
                    getController().getTransferManager()
                        .clearCompletedDownload(dl.getDownloadManager());
                }
                return null;
            }

            @Override
            public void finished() {
                updateActions();
            }
        };
        worker.start();
    }

    /**
     * Clears completed downloads. See MainFrame.MyCleanupAction for accelerator
     * functionality
     */
    private class ClearCompletedAction extends BaseAction {
        ClearCompletedAction() {
            super("clearcompleteddownloads", DownloadsPanel.this
                .getController());
        }

        public void actionPerformed(ActionEvent e) {
            clearDownloads();
        }
    }

    public void toggeDetails() {
        // Ensure the component is created.
        JComponent comp = getFileDetailsPanelComp();
        comp.setVisible(!comp.isVisible());
        showHideFileDetailsButton.setSelected(comp.isVisible());
    }

    /**
     * Listener on table header, takes care about the sorting of table
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof DownloadsTableModel) {
                    DownloadsTableModel downloadsTableModel = (DownloadsTableModel) model;
                    boolean freshSorted = downloadsTableModel
                        .sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        downloadsTableModel.reverseList();
                    }
                }
            }
        }
    }
}