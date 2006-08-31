package de.dal33t.powerfolder.ui.folder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.render.PFListCellRenderer;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;

public class FolderSettingsPanel extends PFUIComponent {
    private Folder folder;
    private JPanel panel;
    private SelectionModel selectionModel;
    private SyncProfileSelectionBox syncProfileChooser;
    private JList jListPatterns;
    private BlackListPatternsListModel blackListPatternsListModel;

    public FolderSettingsPanel(Controller controller) {
        super(controller);
        selectionModel = new SelectionModel();

    }

    /** set the folder to display */
    public void setFolder(Folder folder) {
        this.folder = folder;
        folder.addFolderListener(new MyFolderListener());
        syncProfileChooser.addDefaultActionListener(folder);
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
            "4dlu, pref, 4dlu, pref",
            "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
            .getTranslation("folderpanel.hometab.choose_sync_profile")), cc.xy(
            2, 2));

        builder.add(createChooserAndHelpPanel(), cc.xy(4, 2));

        builder.add(new JLabel(Translation
            .getTranslation("folderpanel.settingstab.ignorepatterns")), cc.xy(
            2, 6));

        builder.add(createPatternsPanel(), cc.xy(4, 6));

        panel = builder.getPanel();
    }

    /**
     * Create chooser + help
     */
    private JPanel createChooserAndHelpPanel() {
        syncProfileChooser = new SyncProfileSelectionBox();
        syncProfileChooser.setRenderer(new PFListCellRenderer());

        JLabel helpLabel = Help.createHelpLinkLabel("help", "node/syncoptions");

        FormLayout layout = new FormLayout("pref,4dlu,pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(syncProfileChooser, cc.xy(1, 1));
        builder.add(helpLabel, cc.xy(3, 1));

        return builder.getPanel();
    }

    private JPanel createPatternsPanel() {
        blackListPatternsListModel = new BlackListPatternsListModel();
        jListPatterns = new JList(blackListPatternsListModel);
        jListPatterns.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selectionModel.setSelection(jListPatterns.getSelectedValue());
            }

        });
        jListPatterns.setPreferredSize(new Dimension(130, 150));
        JScrollPane scroller = new JScrollPane(jListPatterns);

        FormLayout layout = new FormLayout("pref:g,4dlu,pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(scroller, cc.xy(1, 1));
        builder.add(createButtonBar(), cc.xy(3, 1));
        return builder.getPanel();
    }

    private JPanel createButtonBar() {
        AddAction addAction = new AddAction(getController());
        EditAction editAction = new EditAction(getController(), selectionModel);
        RemoveAction removeAction = new RemoveAction(getController(),
            selectionModel);
        
        FormLayout layout = new FormLayout("pref", "pref, 4dlu, pref, 4dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        
        builder.add(new JButton(addAction), cc.xy(1, 1));
        builder.add(new JButton(editAction), cc.xy(1, 3));
        builder.add(new JButton(removeAction), cc.xy(1, 5));
       
        return builder.getPanel();
    }

    private class AddAction extends BaseAction {
        public AddAction(Controller controller) {
            super("folderpanel.settingstab.addbutton", controller);

        }

        public void actionPerformed(ActionEvent e) {
            String pattern = (String) JOptionPane.showInputDialog(
                getUIController().getMainFrame().getUIComponent(),
                "Enter a Pattern\nto exclude files from synchronization:",
                "Add ignore pattern", JOptionPane.PLAIN_MESSAGE, null, null,
                "example/*.class");
            if (pattern != null && pattern.length() > 0) {
                folder.getBlacklist().addPattern(pattern);
                blackListPatternsListModel.fireUpdate();
            }
            jListPatterns.getSelectionModel().clearSelection();
        }
    }

    private class RemoveAction extends SelectionBaseAction {
        public RemoveAction(Controller controller, SelectionModel selectionModel)
        {
            super("folderpanel.settingstab.removebutton", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            folder.getBlacklist().removePattern(
                (String) selectionModel.getSelection());
            blackListPatternsListModel.fireUpdate();
            jListPatterns.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
        }

    }

    private class EditAction extends SelectionBaseAction {
        public EditAction(Controller controller, SelectionModel selectionModel)
        {
            super("folderpanel.settingstab.editbutton", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            String pattern = (String) JOptionPane.showInputDialog(
                getUIController().getMainFrame().getUIComponent(),
                "Edit a Pattern\nto exclude files from synchronization:",
                "Edit ignore pattern", JOptionPane.PLAIN_MESSAGE, null, null,
                selectionModel.getSelection() );
            if (pattern != null &&pattern.length() > 0) {
                folder.getBlacklist().removePattern((String)selectionModel.getSelection());
                folder.getBlacklist().addPattern(pattern);
                blackListPatternsListModel.fireUpdate();
            }
            jListPatterns.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
        }

    }

    private void update() {
        syncProfileChooser.setSelectedItem(folder.getSyncProfile());
    }

    private class MyFolderListener implements FolderListener {

        public boolean fireInEventDispathThread() {
            return false;
        }

        public void folderChanged(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            syncProfileChooser.setSelectedItem(folder.getSyncProfile());
        }
    }

    private class BlackListPatternsListModel extends AbstractListModel {
        public Object getElementAt(int index) {
            return folder.getBlacklist().getPatterns().get(index);
        }

        public int getSize() {
            if (folder == null || folder.getBlacklist() == null) {
                return 0;
            }
            return folder.getBlacklist().getPatterns().size();

        }

        public void fireUpdate() {
            fireContentsChanged(this, 0, folder.getBlacklist().getPatterns()
                .size()+1);
        }
    }

}
