package de.dal33t.powerfolder.ui;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.RequestFileListCallback;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.ui.action.JoinAction;
import de.dal33t.powerfolder.ui.folder.FileFilterModel;
import de.dal33t.powerfolder.ui.folder.FileFilterPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.*;

/**
 * Holds table displaying the filelist of one public folder and a filter box. If
 * there is no filelist yet it displays a text explaining it is fetching the
 * list.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.13 $
 */
public class OnePublicFolderPanel extends PFUIComponent {
    private static final String FILES_PANEL = "files";
    private static final String TEXT_PANEL = "text";
    private FolderDetails folderDetails;
    private JPanel panel;
    private TextPanel textPanel;
    private FileFilterPanel fileFilterPanel;
    private FileFilterModel fileFilterModel;
    private OnePublicFolderTable onePublicFolderTable;
    private OnePublicFolderTableModel onePublicFolderTableModel;
    private CardLayout cardLayout;
    private SelectionModel selectionModel;
    private JPanel contentPanel;

    public OnePublicFolderPanel(Controller controller) {
        super(controller);
        fileFilterModel = new FileFilterModel(controller);
        selectionModel = new SelectionModel();
    }

    public void setFolderInfo(FolderDetails folderDetails) {
        this.folderDetails = folderDetails;
        show(folderDetails, true);
        selectionModel.setSelection(folderDetails);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Filter bar
            builder.add(createFilterToolBar(), cc.xy(1, 1));

            // Content
            builder.add(contentPanel, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 3));

            // toolbar
            builder.add(createToolBar(), cc.xy(1, 4));

            panel = builder.getPanel();
        }
        return panel;
    }

    public String getTitle() {
        return Translation.getTranslation("title.public.folder") + " > "
            + folderDetails.getFolderInfo().name;
    }

    private void initComponents() {
        onePublicFolderTable = new OnePublicFolderTable(getController(),
            fileFilterModel);
        onePublicFolderTable.getTableHeader().addMouseListener(
            new TableHeaderMouseListener());
        onePublicFolderTableModel = (OnePublicFolderTableModel) onePublicFolderTable
            .getModel();
        JScrollPane onePublicFolderTableScrollPane = new JScrollPane(
            onePublicFolderTable);

        textPanel = new TextPanel();
        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);
        contentPanel.add(TEXT_PANEL, textPanel.getUIComponent());
        contentPanel.add(FILES_PANEL, onePublicFolderTableScrollPane);
        cardLayout.show(contentPanel, TEXT_PANEL);

        UIUtil.whiteStripTable(onePublicFolderTable);
        UIUtil.removeBorder(onePublicFolderTableScrollPane);
        UIUtil.setZeroHeight(onePublicFolderTableScrollPane);
        onePublicFolderTable.getSelectionModel().addListSelectionListener(
            new TableSelectionListener());
        onePublicFolderTable.addMouseListener(new DoubleClickAction(
            new JoinAction(getController(), selectionModel)));
        JPopupMenu popupMenu = createPopupMenu();
        onePublicFolderTable.addMouseListener(new PopupMenuOpener(popupMenu));
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JoinAction(getController(), selectionModel));
        return menu;
    }

    private JComponent createFilterToolBar() {
        fileFilterPanel = new FileFilterPanel(fileFilterModel, false);
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(fileFilterPanel.getUIComponent());
        filterPanel.setBorder(Borders
            .createEmptyBorder("1dlu, 1dlu, 1dlu, 1dlu"));
        return filterPanel;
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(new JoinAction(getController(),
            selectionModel)));
        bar.addRelatedGap();
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /**
     * Shows information about the folder.
     * <p>
     * TODO: Rewrite request of filelist
     * 
     * @param foInfo
     * @param requestIfRequired
     *            true if view shouled automatically request a filelist if
     *            required
     */
    private void show(final FolderDetails foDetails, boolean requestIfRequired)
    {
        if (requestIfRequired && !foDetails.isCurrentlyRequesting()) {
            // Request filelist from sources
            RequestFileListCallback requestCallback = new RequestFileListCallback()
            {
                public void fileListReceived(Member from, FileList filelist) {
                    Member source = getController().getFolderRepository()
                        .getSourceFor(filelist.folder, true);
                    log().warn(
                        "Received filelist for " + filelist.folder.name
                            + " from " + from.getNick() + ", offical source: "
                            + source);

                    // /Only display if we are still displaying that folder
                    if (stillDisplayingFolder()) {
                        show(foDetails, false);
                    }
                }

                public void fileListRequestOver() {
                    if (stillDisplayingFolder()) {
                        show(foDetails, false);
                    }
                }

                /**
                 * Answers if we still display the folder
                 * 
                 * @return
                 */
                private boolean stillDisplayingFolder() {
                    Object dt = getController().getUIController()
                        .getInformationQuarter().getDisplayTarget();
                    // /Only display if we are still displaying that folder
                    if (dt instanceof FolderDetails) {
                        FolderDetails displayedFolder = (FolderDetails) dt;
                        return foDetails.getFolderInfo().equals(
                            displayedFolder.getFolderInfo());
                    }
                    return false;
                }
            };

            // Request filelist of folder
            foDetails.requestFileList(getController(), requestCallback);
        }

        FolderInfo foInfo = foDetails.getFolderInfo();
        // Get source with filelist
        Member source = getController().getFolderRepository().getSourceFor(
            foInfo, true);
        // Get filelist
        FileInfo[] fileList = null;

        if (source != null) {
            fileList = source.getLastFileList(foInfo);
        }

        log().warn(
            "Source for " + foInfo.name + " is " + source + ". filelist: "
                + fileList);

        if (fileList != null) {
            show(fileList);
        } else { // FIXME I18n !!!
            // SHOW DISPLAY WINDOW
            String text = "\n"
                + Translation.getTranslation("general.folder")
                + ": "
                + foInfo.name
                + " ("
                + (foInfo.secret
                    ? Translation
                        .getTranslation("onepublicfolderpanel.private")
                    : Translation.getTranslation("onepublicfolderpanel.public"))
                + ")";
            text += "\n"
                + Translation.getTranslation(
                    "onepublicfolderpanel.estimated_size_in_files", ""
                        + foInfo.filesCount, Format
                        .formatBytes(foInfo.bytesTotal));
            if (requestIfRequired || foDetails.isCurrentlyRequesting()) {
                boolean someoneOnline = foDetails
                    .isSomeoneOnline(getController());

                text += "\n\n"
                    + Translation
                        .getTranslation("onepublicfolderpanel.searching_filelist_in_network");
                if (someoneOnline) {
                    text += "\n("
                        + Translation
                            .getTranslation("onepublicfolderpanel.this_may_take_up_to_2_minutes")
                        + ")";
                } else {
                    text += "\n("
                        + Translation
                            .getTranslation("onepublicfolderpanel.no_one_online_found")
                        + ")";
                }

            } else {
                text += "\n\n"
                    + Translation
                        .getTranslation("onepublicfolderpanel.file_list_not_found");
                text += "\n("
                    + Translation
                        .getTranslation("onepublicfolderpanel.unable_to_connect_to_a_source")
                    + ")";
            }

            if (getController().isVerbose()) {
                if (foDetails.getMembers() != null) {
                    text += "\n\nMembers:";
                    for (int i = 0; i < foDetails.getMembers().length; i++) {
                        text += "\n " + foDetails.getMembers()[i].nick;
                    }
                }
            }
            show(text);
        }
    }

    private void show(String text) {
        StyledDocument doc = new DefaultStyledDocument();
        try {
            doc.insertString(0, text, null);
        } catch (BadLocationException e) {
            log().verbose(e);
        }
        textPanel.setText(doc, false);
        cardLayout.show(contentPanel, TEXT_PANEL);
    }

    private void show(FileInfo[] fileList) {
        onePublicFolderTableModel.setFileList(folderDetails.getFolderInfo(),
            fileList);
        cardLayout.show(contentPanel, FILES_PANEL);
    }

    /** updates the selectionModel for the actions */
    private class TableSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            int selectedRow = onePublicFolderTable.getSelectedRow();
            if (selectedRow == -1) {
                selectionModel.setSelection(null);
            } else {// FIXME
                FileInfo fileInfo = (FileInfo) onePublicFolderTableModel
                    .getValueAt(selectedRow, 0);
                selectionModel.setSelection(fileInfo);
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
                if (model instanceof OnePublicFolderTableModel) {
                    OnePublicFolderTableModel fModel = (OnePublicFolderTableModel) model;
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
