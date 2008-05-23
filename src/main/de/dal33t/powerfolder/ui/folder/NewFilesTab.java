package de.dal33t.powerfolder.ui.folder;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Shows folder details from new downloads.
 *
 * @author <A HREF="mailto:hglasgow@powerfolder.com">Harry Glasgow</A>
 * @version $Revision: 3.1 $ *
 */
public class NewFilesTab extends PFUIComponent implements FolderTab,
    HasDetailsPanel
{

    private JComponent panel;
    private FolderDownloadsTable folderDownloadsTable;
    private JScrollPane directoryTableScrollPane;
    private JPanel toolbar;
    private JToggleButton showHideFileDetailsButton;

    private JComponent fileDetailsPanelComp;

    private Action clearCompletedAction;

    /** The currently selected items */
    private SelectionModel selectionModel;

    public NewFilesTab(Controller controller) {
        super(controller);
        selectionModel = new SelectionModel();
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
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
        builder.add(directoryTableScrollPane, cc.xy(1, 1));
        builder.add(fileDetailsPanelComp, cc.xy(1, 2));
        return builder.getPanel();
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        folderDownloadsTable = new FolderDownloadsTable(getController());
        directoryTableScrollPane = new JScrollPane(folderDownloadsTable);
        UIUtil.whiteStripTable(folderDownloadsTable);
        UIUtil.removeBorder(directoryTableScrollPane);
        UIUtil.setZeroHeight(directoryTableScrollPane);

        // Add selection listener for updating selection model
        folderDownloadsTable.getSelectionModel().addListSelectionListener(
            new DirectoryListSelectionListener());

        fileDetailsPanelComp = createFileDetailsPanel();
        // Details not visible @ start
        fileDetailsPanelComp.setVisible(false);

        Action showHideFileDetailsAction = new ShowHideFileDetailsAction(this,
            getController());
        showHideFileDetailsButton = new JToggleButton(showHideFileDetailsAction);

        clearCompletedAction = new ClearCompletedAction();

        toolbar = createToolBar();

        updateActions();
    }

    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addRelatedGap();

        bar.addGridded(showHideFileDetailsButton);
        bar.addRelatedGap();
        bar.addGridded(new JButton(clearCompletedAction));
                      
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /**
     * @return the file details panel and sets it to the internal variable
     */
    private JComponent createFileDetailsPanel() {
        FileDetailsPanel fileDetailsPanel = new FileDetailsPanel(
            getController(), selectionModel);
        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 3dlu, pref, fill:pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 4));
        return builder.getPanel();
    }

    public String getTitle() {
        return Translation.getTranslation("folderpanel.new_files_tab.title");
    }

    public void setFolder(Folder folder) {
        folderDownloadsTable.getFolderDownloadsTableModel().setFolder(folder);
    }

    // Actions ****************************************************************

    /** updates the SelectionModel if some selection has changed in the table */
    private class DirectoryListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                updateActions();
            }
        }
    }

    private void updateActions() {
        int[] selectedRows = folderDownloadsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            selectionModel.setSelection(null);
        } else {
            Object[] selectedObjects = new Object[selectedRows.length];

            for (int i = 0; i < selectedRows.length; i++) {
                selectedObjects[i] = folderDownloadsTable
                        .getFolderDownloadsTableModel()
                        .getValueAt(selectedRows[i], 0);

            }
            selectionModel.setSelections(selectedObjects);
        }

        clearCompletedAction.setEnabled(selectedRows.length > 0);
    }

    public void toggeDetails() {
        fileDetailsPanelComp.setVisible(!fileDetailsPanelComp.isVisible());
        showHideFileDetailsButton.setSelected(fileDetailsPanelComp.isVisible());
    }

    /**
     * Clears completed downloads. 
     */
    private class ClearCompletedAction extends BaseAction {
        ClearCompletedAction() {
            super("clearcompleteddownloads", NewFilesTab.this
                .getController());
        }

        public void actionPerformed(ActionEvent e) {
            clearDownloads();
        }
    }

    private void clearDownloads() {

        ActivityVisualizationWorker avw =
                new ActivityVisualizationWorker(getUIController()) {

                    @Override
                    protected String getTitle()
                    {
                        return Translation.getTranslation("new_files_tab.cleanup_activity.title");
                    }

                    @Override
                    protected String getWorkingText()
                    {
                        return Translation.getTranslation("new_files_tab.cleanup_activity.description");
                    }

                    public Object construct()
                    {

                        TransferManager transferManager = getController().getTransferManager();
                        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                        for (int i = 0; i < selectionModel.getSelections().length; i++)
                        {
                            FileInfo fileInfo = (FileInfo) selectionModel.getSelections()[i];
                            fileInfos.add(fileInfo);
                        }

                        for (FileInfo fileInfo : fileInfos)
                        {
                            transferManager.clearCompletedDownload(fileInfo);
                        }
                        return null;

                    }
                };

        // Clear completed downloads
        avw.start();

    }
}