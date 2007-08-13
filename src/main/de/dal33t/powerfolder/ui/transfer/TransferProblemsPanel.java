package de.dal33t.powerfolder.ui.transfer;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.transfer.TransferProblemBean;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.util.List;

/**
 * Contains all information about transfer problemss
 *
 * @author <a href="mailto:harryglasgow@gmail.com">Harry Glasgow</a>
 * @version $Revision: 2.0 $
 */
public class TransferProblemsPanel extends PFUIPanel {

    // Visual components
    private JComponent panel;
    private QuickInfoPanel quickInfo;
    private JScrollPane tablePane;
    private JComponent toolbar;
    private JPopupMenu fileMenu;
    private JComponent filePanelComponent;
    private SelectionModel selectionModel;
    private IgnoreFileAction ignoreFileAction;
    private UnIgnoreFileAction unIgnoreFileAction;

    // Actions
    private Action clearProblemsAction;

    /**
     * Constructor
     *
     * @param controller
     */
    public TransferProblemsPanel(Controller controller) {
        super(controller);

        // Init components.
        initComponents();

        // Register listeners.
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(
                new MyTransferManagerListener());
    }


    /**
     * lazy build main panel
     *
     * @return the panel
     */
    public Component getUIComponent() {

        // build if necessary
        if (panel == null) {

            // Build panel.
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Create content panel.
     *
     * @return
     */
    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow",
                "fill:0:grow, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(tablePane, cc.xy(1, 1));
        builder.add(filePanelComponent, cc.xy(1, 2));
        return builder.getPanel();
    }

    /**
     * Return the title
     *
     * @return the title
     */
    public static String getTitle() {
        return Translation.getTranslation("general.transfer.problems");
    }

    /**
     * Initialize the component.
     */
    private void initComponents() {

        selectionModel = new SelectionModel();
        ignoreFileAction = new IgnoreFileAction(getController(), selectionModel);
        unIgnoreFileAction = new UnIgnoreFileAction(getController(), selectionModel);
        buildPopupMenus();

        // Build the quickInfo panel
        quickInfo = new TransferProblemsQuickInfoPanel(getController());

        // Build the table and panel
        TransferProblemsTable table = new TransferProblemsTable(getController(), selectionModel);
        tablePane = new JScrollPane(table);
        table.addMouseListener(new MyMouseListener());

        // Whitestrip & set sizes
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tablePane);
        UIUtil.removeBorder(tablePane);

        // Build the filePanelComponent
        filePanelComponent = createFilePanel();
        filePanelComponent.setVisible(false);

        // Initialize actions
        clearProblemsAction = new ClearProblemsAction(getController());

        // Create toolbar
        toolbar = createToolBar();

        // Listener on table selections
        table.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {

                        if (!e.getValueIsAdjusting()) {

                            // Update actions
                            updateActions();
                        }
                    }
                });

        updateActions();
    }

    /**
     * Create the file panel
     *
     * @return the file panel
     */
    private JComponent createFilePanel() {
        FileDetailsPanel filePanel = new FileDetailsPanel(getController());
        FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, 3dlu, pref, fill:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(filePanel.getEmbeddedPanel(), cc.xy(1, 4));
        return builder.getPanel();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        fileMenu.add(ignoreFileAction);
        fileMenu.add(unIgnoreFileAction);
    }

    /**
     * Create the toolbar
     *
     * @return the toolbar
     */
    private JComponent createToolBar() {

        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(clearProblemsAction));

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /**
     * Helper class, Opens the local folder on action *
     */
    private class ClearProblemsAction extends BaseAction {

        private ClearProblemsAction(Controller controller) {
            super("clearrecentdownloads", controller);
        }

        /**
         * Clears the transfer problem count in the transfer manager.
         *
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            getController().getTransferManager().clearTransferProblems();
        }
    }

    /**
     * Updates all action states (enabled/disabled)
     */
    private void updateActions() {

        // Only enable if tm.countTransferProblems() > 0
        // because updateActions() seems to fire before the table has settled.

        int count = getController().getTransferManager().countTransferProblems();

        // Enable the clearProblemsAction if there are any rows.
        clearProblemsAction.setEnabled(count > 0);
    }

    /**
     * Listens to transfer manager
     */
    private class MyTransferManagerListener extends TransferAdapter {

        public void transferProblem(TransferManagerEvent event) {
            updateActions();
        }

        public void clearTransferProblems() {
            updateActions();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    /**
     * marks all selected files as ignored (blacklisted, do not share/ do not
     * download )
     */
    private class IgnoreFileAction extends SelectionBaseAction {

        private TransferProblemBean tpb;

        public IgnoreFileAction(Controller controller, SelectionModel selectionModel) {
            super("ignorefile", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            if (event.getSelection() instanceof TransferProblemBean) {
                tpb = (TransferProblemBean) event.getSelection();
                Folder folder = tpb.getFileInfo().getFolder(getController().getFolderRepository());
                Blacklist blacklist = folder.getBlacklist();
                boolean ignored = blacklist.isIgnored(tpb.getFileInfo());
                setEnabled(!ignored);
            } else if (event.getSelection() == null) {
                tpb = null;
                setEnabled(false);
            }
        }

        public void actionPerformed(ActionEvent e) {
            Folder folder = tpb.getFileInfo().getFolder(getController().getFolderRepository());
            Blacklist blacklist = folder.getBlacklist();
            blacklist.add(tpb.getFileInfo());

            // Abort all autodownloads on this folder
            getController().getTransferManager().abortAllAutodownloads(folder);

            // Request those still needed
            getController().getFolderRepository().getFileRequestor().triggerFileRequesting(folder.getInfo());

            setEnabled(false);
            unIgnoreFileAction.setEnabled(true);
        }
    }

    /**
     * marks all selected files as unignored (not blacklisted, do share/ do
     * download )
     */
    private class UnIgnoreFileAction extends SelectionBaseAction {

        private TransferProblemBean tpb;

        public UnIgnoreFileAction(Controller controller, SelectionModel selectionModel) {
            super("unignorefile", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            if (event.getSelection() instanceof TransferProblemBean) {
                tpb = (TransferProblemBean) event.getSelection();
                Folder folder = tpb.getFileInfo().getFolder(getController().getFolderRepository());
                Blacklist blacklist = folder.getBlacklist();
                boolean ignored = blacklist.isIgnored(tpb.getFileInfo());
                setEnabled(ignored);
            } else if (event.getSelection() == null) {
                tpb = null;
                setEnabled(false);
            }
        }

        public void actionPerformed(ActionEvent e) {
            Folder folder = tpb.getFileInfo().getFolder(getController().getFolderRepository());
            Blacklist blacklist = folder.getBlacklist();
            blacklist.remove(tpb.getFileInfo());

            // trigger download if something was removed for the exclusions
            getController().getFolderRepository().getFileRequestor().triggerFileRequesting(folder.getInfo());

            setEnabled(false);
            ignoreFileAction.setEnabled(true);
        }
    }

    private class MyMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }
}
