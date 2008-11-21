/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.transfer;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.actionold.HasDetailsPanel;
import de.dal33t.powerfolder.ui.actionold.ShowHideFileDetailsAction;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.ui.SwingWorker;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Contains all information about uploads
 * 
 * @version $Revision: 1.4 $
 */
public class UploadsPanel extends PFUIPanel implements HasDetailsPanel {
    private JComponent panel;

    private QuickInfoPanel quickInfo;
    private UploadsTable table;
    private UploadsTableModel tableModel;
    private JScrollPane tablePane;
    private JComponent toolbar;
    private JToggleButton showHideFileDetailsButton;

    // The actions
    private FileDetailsPanel fileDetailsPanel;
    private JComponent fileDetailsPanelComp;

    private Action clearCompletedAction;
    private JCheckBox autoCleanupCB;

    public UploadsPanel(Controller controller) {
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

    public String getTitle() {
        return Translation.getTranslation("general.uploads");
    }

    private JComponent getFileDetailsPanelComp() {
        if (fileDetailsPanelComp == null) {
            fileDetailsPanelComp = createFileDetailsPanel();
        }
        return fileDetailsPanelComp;
    }

    /*
     * public Action getShowHideFileDetailsAction(){
     * if(showHideFileDetailsAction == null){ showHideFileDetailsAction = }
     * return showHideFileDetailsAction; }
     */

    private void initComponents() {
        final TransferManagerModel model = getApplicationModel()
            .getTransferManagerModel();

        quickInfo = new UploadsQuickInfoPanel(getController());
        // Uploads table
        table = new UploadsTable(getApplicationModel().getTransferManagerModel());
        table.getTableHeader().addMouseListener(
            new TableHeaderMouseListener());
        tableModel = (UploadsTableModel) table.getModel();
        tablePane = new JScrollPane(table);
        // Whitestrip
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tablePane);
        UIUtil.setZeroHeight(tablePane);

        autoCleanupCB = new JCheckBox(Translation
            .getTranslation("upload_panel.auto_cleanup.name"));
        autoCleanupCB.setToolTipText(Translation
            .getTranslation("upload_panel.auto_cleanup.description"));
        autoCleanupCB.setSelected(ConfigurationEntry.UPLOADS_AUTO_CLEANUP
            .getValueBoolean(getController()));
        autoCleanupCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.getUploadsAutoCleanupModel().setValue(
                    autoCleanupCB.isSelected());
                ConfigurationEntry.UPLOADS_AUTO_CLEANUP
                    .setValue(getController(), String.valueOf(autoCleanupCB
                        .isSelected()));
                getController().saveConfig();
            }
        });

        clearCompletedAction = new ClearCompletedAction();

        // The file/upload info
        fileDetailsPanelComp = getFileDetailsPanelComp();
        fileDetailsPanelComp.setVisible(false);

        // Initalize actions
        Action showHideFileDetailsAction = new ShowHideFileDetailsAction(
                this, getController());
        showHideFileDetailsButton = new JToggleButton(showHideFileDetailsAction);

        // clearCompletedAction = new ClearCompletedAction();

        // Create toolbar
        toolbar = createToolBar();

        // Listener on table selections
        table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int index = table.getSelectionModel()
                            .getLeadSelectionIndex();
                        if (index >= 0 && index < tableModel.getRowCount()) {
                            Upload ul = tableModel.getUploadAtRow(index);
                            if (ul != null) {// null if upload removed in
                                // meantime
                                fileDetailsPanel.setFile(ul.getFile());
                            }
                        }

                        // Update actions
                        updateActions();

                    }
                }
            });

        // Listener on transfer manager
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());

        // setup inital actions state
        updateActions();
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
        bar.addGridded(showHideFileDetailsButton);
        bar.addRelatedGap();
        bar.addGridded(new JButton(clearCompletedAction));
        bar.addRelatedGap();
        bar.addGridded(autoCleanupCB);
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    public void toggleDetails() {
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
                if (model instanceof UploadsTableModel) {
                    UploadsTableModel uploadsTableModel = (UploadsTableModel) model;
                    boolean freshSorted = uploadsTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        uploadsTableModel.reverseList();
                    }
                }
            }
        }
    }

    public void clearUploads() {

        // Clear completed uploads
        SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                int rowCount = table.getRowCount();
                if (rowCount == 0) {
                    return null;
                }

                // If no rows are selected,
                // arrange for all uploads to be cleared.
                boolean noneSelected = true;
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (table.isRowSelected(i)) {
                        noneSelected = false;
                        break;
                    }
                }

                // Do in two passes so changes to the model do not affect
                // the process.
                List<Upload> uploadsToClear = new ArrayList<Upload>();

                for (int i = 0; i < table.getRowCount(); i++) {
                    if (noneSelected || table.isRowSelected(i)) {
                        Upload ul = tableModel.getUploadAtRow(i);
                        if (ul.isCompleted()) {
                            uploadsToClear.add(ul);
                        }
                    }
                }
                for (Upload ul : uploadsToClear) {
                    getController().getTransferManager()
                        .clearCompletedUpload(ul);
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
     * Clears completed uploads. See MainFrame.MyCleanupAction for accelerator
     * functionality
     */
    private class ClearCompletedAction extends BaseAction {
        ClearCompletedAction() {
            super("clear_completed_uploads", UploadsPanel.this
                .getController());
        }

        public void actionPerformed(ActionEvent e) {
            clearUploads();
        }
    }

    /**
     * Updates all action states (enabled/disabled)
     */
    private void updateActions() {
        boolean rowsExist = table.getRowCount() > 0;
        clearCompletedAction.setEnabled(rowsExist);
    }

    /**
     * TransferManagerListener to respond to upload changes.
     */
    private class MyTransferManagerListener extends TransferAdapter {

        public void uploadRequested(TransferManagerEvent event) {
            updateActions();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateActions();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateActions();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateActions();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateActions();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateActions();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }


}