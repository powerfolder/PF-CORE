package de.dal33t.powerfolder.ui.folder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import static de.dal33t.powerfolder.disk.FolderSettings.*;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.model.BlackListPatternsListModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * Tab holding the settings of the folder. Selection of sync profile and
 * ignore/blacklist patterns
 */
public class SettingsTab extends PFUIComponent implements FolderTab {
    private Folder folder;
    private JPanel panel;
    private SelectionModel selectionModel;
    private SyncProfileSelectorPanel  syncProfileSelectorPanel;
    private JList jListPatterns;
    /** Maps the blacklist of the current folder to a ListModel */
    private BlackListPatternsListModel blackListPatternsListModel;
    /** listens to changes in the syncprofile */
    private MyFolderListener myFolderListener;
    private JCheckBox useRecycleBinBox;
    private boolean previewMode;

    public SettingsTab(Controller controller, boolean previewMode) {
        super(controller);
        selectionModel = new SelectionModel();
        myFolderListener = new MyFolderListener();
        this.previewMode = previewMode;
    }

    public String getTitle() {
        return Translation.getTranslation("folderpanel.settingstab.title");
    }

    /** Set the folder to display 
     * @param folder */
    public void setFolder(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        if (this.folder != null) {
            this.folder.removeFolderListener(myFolderListener);
        }

        this.folder = folder;
        blackListPatternsListModel.setBlacklist(folder.getBlacklist());
        folder.addFolderListener(myFolderListener);
        syncProfileSelectorPanel.setUpdateableFolder(folder);
        useRecycleBinBox.setSelected(folder.isUseRecycleBin());
        update();
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    private void initComponents() {
        FormLayout layout = new FormLayout(
            "4dlu, right:pref, 4dlu, pref",
            "4dlu, pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
            .getTranslation("folderpanel.settingstab.choose_sync_profile")), cc.xy(
            2, 2));

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        builder.add(syncProfileSelectorPanel.getUIComponent(), cc.xy(4, 2));

        createUseRecycleBin();
        builder.add(useRecycleBinBox, cc.xy(4, 4));

        builder.add(new JLabel(Translation
            .getTranslation("folderpanel.settingstab.ignorepatterns")), cc.xy(
            2, 6));

        builder.add(createPatternsPanel(), cc.xy(4, 6));

        panel = builder.getPanel();
    }

    private void createUseRecycleBin() {
        useRecycleBinBox = new JCheckBox(new AbstractAction(
                Translation.getTranslation("folderpanel.settingstab.userecyclebin")) {
            public void actionPerformed(ActionEvent event) {
                folder.setUseRecycleBin(useRecycleBinBox.isSelected());
                Properties config = getController().getConfig();
                // Inverse logic for backward compatability.
                config.setProperty(FOLDER_SETTINGS_PREFIX + folder.getName() +
                        FOLDER_SETTINGS_DONT_RECYCLE,
                        String.valueOf(!useRecycleBinBox.isSelected()));
                getController().saveConfig();
            }
        });
    }

    private JPanel createPatternsPanel() {
        blackListPatternsListModel = new BlackListPatternsListModel(null);
        jListPatterns = new JList(blackListPatternsListModel);
        jListPatterns.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selectionModel.setSelection(jListPatterns.getSelectedValue());
            }

        });

        Dimension size = new Dimension(200, 150);

        JScrollPane scroller = new JScrollPane(jListPatterns);
        scroller.setPreferredSize(size);

        FormLayout layout = new FormLayout("pref", "pref, 4dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(scroller, cc.xy(1, 1));
        builder.add(createButtonBar(), cc.xy(1, 3));
        return builder.getPanel();
    }

    private JPanel createButtonBar() {
        AddAction addAction = new AddAction(getController());
        EditAction editAction = new EditAction(getController(), selectionModel);
        RemoveAction removeAction = new RemoveAction(getController(),
            selectionModel);

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(addAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(editAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(removeAction));

        return bar.getPanel();
    }

    /**
     * Add a pattern to the backlist, opens a input dialog so user can enter
     * one.
     */
    private class AddAction extends BaseAction {
        public AddAction(Controller controller) {
            super("folderpanel.settingstab.addbutton", controller);

        }

        public void actionPerformed(ActionEvent e) {
            showAddPane(null);
        }
    }

    public void showAddPane(String initialPattern) {
        String text = Translation
            .getTranslation("folderpanel.settingstab.add_a_pattern.text");
        String title = Translation
            .getTranslation("folderpanel.settingstab.add_a_pattern.title");
        if (initialPattern == null || initialPattern.trim().length() == 0) {
            // Use example if none supplied.
            initialPattern = Translation
                .getTranslation("folderpanel.settingstab.add_a_pattern.example");
        }

        String pattern = (String) JOptionPane.showInputDialog(
            getUIController().getMainFrame().getUIComponent(), text, title,
            JOptionPane.PLAIN_MESSAGE, null, null, initialPattern);
        if (!StringUtils.isBlank(pattern)) {
            folder.getBlacklist().addPattern(pattern);
            blackListPatternsListModel.fireUpdate();
        }
        jListPatterns.getSelectionModel().clearSelection();
    }

    /** removes the selected pattern from the blacklist */
    private class RemoveAction extends SelectionBaseAction {
        public RemoveAction(Controller controller, SelectionModel selectionModel)
        {
            super("folderpanel.settingstab.removebutton", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            for (Object object : selectionModel.getSelections()) {
                String selection = (String) object;
                folder.getBlacklist().removePattern(selection);
            }
            blackListPatternsListModel.fireUpdate();
            jListPatterns.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
        }

    }

    /**
     * Removes any patterns for this file name.
     * Directories should have "/*" added to the name.
     *
     * @param fileName
     */
    public void removePatternsForFile(String fileName) {

        // Match any patterns for this file.
        for (String pattern : folder.getBlacklist().getPatterns()) {
            if (PatternMatch.isMatch(fileName.toLowerCase(), pattern)) {

                // Confirm that the user wants to remove this.
                int result = DialogFactory.genericDialog(
                        getController().getUIController().getMainFrame().getUIComponent(),
                        Translation.getTranslation("remove_pattern.title"),
                        Translation.getTranslation("remove_pattern.prompt", pattern),
                        new String[]{Translation.getTranslation("remove_pattern.remove"),
                        Translation.getTranslation("remove_pattern.dont"),
                        Translation.getTranslation("general.cancel")},
                        0, GenericDialogType.INFO); // Default is remove.
                if (result == 0) { // Remove
                    // Remove pattern and update.
                    folder.getBlacklist().removePattern(pattern);
                    blackListPatternsListModel.fireUpdate();
                } else if (result == 2) { // Cancel
                    // Abort for all other patterns.
                    break;
                }
            }
        }
    }

    /** opens a popup, input dialog to edit the selected pattern */
    private class EditAction extends SelectionBaseAction {
        EditAction(Controller controller, SelectionModel selectionModel)
        {
            super("folderpanel.settingstab.editbutton", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            String text = Translation
                .getTranslation("folderpanel.settingstab.edit_a_pattern.text");
            String title = Translation
                .getTranslation("folderpanel.settingstab.edit_a_pattern.title");

            String pattern = (String) JOptionPane.showInputDialog(
                getUIController().getMainFrame().getUIComponent(), text, title,
                JOptionPane.PLAIN_MESSAGE, null, null,
                // the text to edit:
                selectionModel.getSelection());
            if (!StringUtils.isBlank(pattern)) {
                folder.getBlacklist().removePattern(
                    (String) selectionModel.getSelection());
                folder.getBlacklist().addPattern(pattern);
                blackListPatternsListModel.fireUpdate();
            }
            jListPatterns.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
        }

    }

    /** refreshes the UI elements with the current data */
    private void update() {
        if (!previewMode) {
            syncProfileSelectorPanel.setSyncProfile(folder.getSyncProfile(), false);
        }
        if (jListPatterns != null) {
            blackListPatternsListModel.fireUpdate();
        }
    }

    /** listens to changes of the sync profile */
    private class MyFolderListener implements FolderListener {

        public boolean fireInEventDispathThread() {
            return true;
        }

        public void folderChanged(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }
        
        public void syncProfileChanged(FolderEvent folderEvent) {
            syncProfileSelectorPanel.setSyncProfile(folder.getSyncProfile(), false);
        }

    }
}
