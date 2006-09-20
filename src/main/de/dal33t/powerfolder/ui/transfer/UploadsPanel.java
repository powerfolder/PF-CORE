/* $Id: UploadsPanel.java,v 1.4 2006/04/15 04:23:51 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.awt.Component;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.ShowHideFileDetailsAction;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.HasUIPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Contains all information about uploads
 * 
 * @version $Revision: 1.4 $
 */
public class UploadsPanel extends PFUIComponent implements HasUIPanel {
    private JPanel panel;
    
    private QuickInfoPanel quickInfo;
    private UploadsTable table;
    private UploadsTableModel tableModel;
    private JScrollPane tablePane;
    private JComponent toolbar;

    // The actions
    private FileDetailsPanel filePanel;
    private JPanel filePanelComp;
    // private Action startDownloadsAction;
    // private Action abortDownloadsAction;
    private Action showHideFileDetailsAction;

    // private Action clearCompletedAction;

    public UploadsPanel(Controller controller) {
        super(controller);
    }

    // UI Building ************************************************************

    /**
     * Returns (and builds layzily) the ui component of this panel
     * 
     * @return
     */
    public Component getUIComponent() {
        if (panel == null) {
            // initalize
            initComponents();

            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, pref, fill:pref:grow, pref, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            
            builder.add(quickInfo.getUIComponent(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.add(tablePane, cc.xy(1, 3));
            builder.addSeparator(null, cc.xy(1, 4));
            builder.add(filePanelComp, cc.xy(1, 5));
            builder.add(toolbar, cc.xy(1, 6));

            panel = builder.getPanel();
        }
        return panel;
    }

    public String getTitle() {
        return Translation.getTranslation("general.uploads");
    }

    private void initComponents() {
        quickInfo = new UploadsQuickInfoPanel(getController());
        // Uploads table
        table = new UploadsTable(getController().getTransferManager());
        tableModel = (UploadsTableModel) table.getModel();
        tablePane = new JScrollPane(table);
        // Whitestrip
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tablePane);
        UIUtil.setZeroHeight(tablePane);

        // The file/upload info
        createFilePanel().setVisible(false);

        // Initalize actions
        // abortDownloadsAction = new AbortDownloadAction();
        // abortDownloadsAction.setEnabled(false);
        // startDownloadsAction = new StartDownloadsAction();
        // startDownloadsAction.setEnabled(false);
        showHideFileDetailsAction = new ShowHideFileDetailsAction(
            filePanelComp, getController());
        // clearCompletedAction = new ClearCompletedAction();

        // Create toolbar
        toolbar = createToolBar();

        // Add mouselisteners to table
        // table.addMouseListener(new DoubleClickAction(startDownloadsAction));
        // table.addMouseListener(new PopupMenuOpener(createPopupMenu()));

        // Listener on table selections
        table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        // Update actions
                        // updateActions();
                        int index = table.getSelectionModel()
                            .getLeadSelectionIndex();
                        if (index >= 0 && index < tableModel.getRowCount()) {
                            Upload ul = tableModel.getUploadAtRow(index);
                            if (ul != null) {// null if upload removed in
                                                // meantime
                                filePanel.setFile(ul.getFile());
                            }
                        }
                    }
                }
            });

        // setup inital actions state
        // updateActions();
    }

    /**
     * Creates the file panel
     * 
     * @return
     */
    private JComponent createFilePanel() {
        filePanel = new FileDetailsPanel(getController());

        FormLayout layout = new FormLayout("fill:pref:grow",
            "3dlu, pref, fill:pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 2));
        builder.add(filePanel.getEmbeddedPanel(), cc.xy(1, 3));
        builder.addSeparator(null, cc.xy(1, 4));

        filePanelComp = builder.getPanel();
        return filePanelComp;
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        // bar.addGridded(new JButton(startDownloadsAction));
        // bar.addRelatedGap();
        // bar.addGridded(new JButton(abortDownloadsAction));
        // bar.addUnrelatedGap();
        bar.addGridded(new JToggleButton(showHideFileDetailsAction));
        // bar.addRelatedGap();
        // bar.addGridded(new JButton(clearCompletedAction));
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /**
     * Creates the uploads popup menu
     */
    // private JPopupMenu createPopupMenu() {
    // JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
    // popupMenu.add(startDownloadsAction);
    // popupMenu.add(abortDownloadsAction);
    // popupMenu.addSeparator();
    // popupMenu.add(clearCompletedAction);
    // return popupMenu;
    // }
    // Helper methods *********************************************************
    /**
     * Updates all action states (enabled/disabled)
     */
    // private void updateActions() {
    // abortDownloadsAction.setEnabled(false);
    // startDownloadsAction.setEnabled(false);
    // int[] rows = table.getSelectedRows();
    // if (rows == null || rows.length <= 0) {
    // return;
    // }
    // }
    // for (int i = 0; i < rows.length; i++) {
    // Download download = tableModel.getDownloadAtRow(rows[i]);
    // if (download.isCompleted()) {
    // startDownloadsAction.setEnabled(true);
    // }
    // if (!download.isCompleted()) {
    // abortDownloadsAction.setEnabled(true);
    // }
    // }
    // }
    // Inner classes **********************************************************
    /**
     * Starts the selected downloads
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.4 $
     */
    // private class StartDownloadsAction extends BaseAction {
    // public StartDownloadsAction() {
    // super("startdownloads", UploadsPanel.this.getController());
    // }
    // public void actionPerformed(ActionEvent e) {
    // int[] rows = table.getSelectedRows();
    // if (rows == null || rows.length <= 0) {
    // return;
    // }
    // Download[] selected = new Download[rows.length];
    // for (int i = 0; i < rows.length; i++) {
    // selected[i] = tableModel.getDownloadAtRow(rows[i]);
    // }
    // Abort it two steps, because .abort causes model to change
    // for (int i = 0; i < selected.length; i++) {
    // File file = selected[i].getFile().getDiskFile(
    // UploadsPanel.this.getController().getFolderRepository());
    // if (file != null && file.exists()) {
    // try {
    // Util.executeFile(file);
    // } catch (IOException ex) {
    // log().error(ex);
    // }
    // }
    // }
    // }
    // }
    /**
     * Aborts the selected downloads
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.4 $
     */
    // private class AbortDownloadAction extends BaseAction {
    // public AbortDownloadAction() {
    // super("abortdownload", UploadsPanel.this.getController());
    // }
    // public void actionPerformed(ActionEvent e) {
    // int[] rows = table.getSelectedRows();
    // if (rows == null || rows.length <= 0) {
    // return;
    // }
    // Download[] dl2abort = new Download[rows.length];
    // for (int i = 0; i < rows.length; i++) {
    // dl2abort[i] = tableModel.getDownloadAtRow(rows[i]);
    // }
    // Abort it two steps, because .abort causes model to change
    // for (int i = 0; i < dl2abort.length; i++) {
    // dl2abort[i].abort();
    // }
    // }
    // }
    // /**
    // * clears all completed uploads
    // *
    // * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
    // * @version $Revision: 1.4 $
    // */
    // private class ClearCompletedAction extends BaseAction {
    // public ClearCompletedAction() {
    // super("cleardompleteddownloads", UploadsPanel.this.getController());
    // }
    //
    // public void actionPerformed(ActionEvent e) {
    // tableModel.clearCompleted();
    // }
    // }
    // Helper classes *********************************************************
    /**
     * Helper class which opens a popmenu when requested (right-mouseclick)
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.4 $
     */
    // private class PopupMenuOpener extends MouseAdapter {
    // private JPopupMenu popupMenu;
    // private PopupMenuOpener(JPopupMenu popupMenu) {
    // if (popupMenu == null) {
    // throw new NullPointerException("Popupmenu is null");
    // }
    // this.popupMenu = popupMenu;
    // }
    // public void mousePressed(MouseEvent evt) {
    // if (evt.isPopupTrigger()) {
    // showContextMenu(evt);
    // }
    // }
    // public void mouseReleased(MouseEvent evt) {
    // if (evt.isPopupTrigger()) {
    // showContextMenu(evt);
    // }
    // }
    //      
    // private void showContextMenu(MouseEvent evt) {
    // updateActions();
    // popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    // }
    // }
}